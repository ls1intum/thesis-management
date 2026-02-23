package de.tum.cit.aet.thesis.cron;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;

/**
 * One-time migration task that attempts to fetch existing profile pictures for users
 * who don't have a custom avatar and stores them locally. After running successfully,
 * this task can be removed from the codebase.
 *
 * <p>Uses an external avatar service to look up images by email hash. Only users with
 * an existing profile picture get their image downloaded.</p>
 */
@Component
public class ProfilePictureMigration {
	private static final Logger log = LoggerFactory.getLogger(ProfilePictureMigration.class);

	private static final String AVATAR_LOOKUP_URL = "https://www.gravatar.com/avatar/";

	private final UserRepository userRepository;
	private final UploadService uploadService;

	public ProfilePictureMigration(UserRepository userRepository, UploadService uploadService) {
		this.userRepository = userRepository;
		this.uploadService = uploadService;
	}

	/**
	 * Runs 5 minutes after server start, once only. Finds all users without a custom avatar,
	 * checks if they have an existing profile picture, and if so downloads and stores it locally.
	 */
	@Scheduled(initialDelay = 5 * 60 * 1000, fixedDelay = Long.MAX_VALUE)
	public void migrateProfilePictures() {
		List<User> usersWithoutAvatar = userRepository.findAllByAvatarIsNullOrAvatarIsEmpty();

		if (usersWithoutAvatar.isEmpty()) {
			log.info("Profile picture migration: no users without custom avatar found, skipping");
			return;
		}

		log.info("Profile picture migration: checking {} users without custom avatar", usersWithoutAvatar.size());

		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();

		int downloaded = 0;
		int skipped = 0;

		for (User user : usersWithoutAvatar) {
			try {
				String email = user.getEmail() != null ? user.getEmail().getAddress() : null;
				if (email == null || email.isBlank()) {
					skipped++;
					continue;
				}

				String hash = sha256Hex(email.trim().toLowerCase());
				String lookupUrl = AVATAR_LOOKUP_URL + hash + "?s=400&d=404";

				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(lookupUrl))
						.timeout(Duration.ofSeconds(10))
						.GET()
						.build();

				HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

				if (response.statusCode() == 200) {
					byte[] imageBytes = response.body().readAllBytes();

					String storedFilename = uploadService.storeBytes(imageBytes, "png", 1024 * 1024);
					user.setAvatar(storedFilename);
					userRepository.save(user);
					downloaded++;

					log.info("Profile picture migration: downloaded avatar for user {} ({})",
							user.getUniversityId(), user.getId());
				} else {
					skipped++;
				}
			} catch (Exception e) {
				log.warn("Profile picture migration: failed to process user {}", user.getId(), e);
				skipped++;
			}
		}

		log.info("Profile picture migration completed: {} images downloaded, {} skipped", downloaded, skipped);
	}

	private String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes());
			StringBuilder sb = new StringBuilder();
			for (byte b : hashBytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException("SHA-256 not available", e);
		}
	}
}
