package de.tum.cit.aet.thesis.cron;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.GravatarService;
import de.tum.cit.aet.thesis.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

	private final UserRepository userRepository;
	private final GravatarService gravatarService;
	private final UploadService uploadService;

	/**
	 * Constructs the migration task with the user repository, gravatar service, and upload service.
	 *
	 * @param userRepository the user repository
	 * @param gravatarService the gravatar service
	 * @param uploadService the upload service
	 */
	public ProfilePictureMigration(UserRepository userRepository, GravatarService gravatarService, UploadService uploadService) {
		this.userRepository = userRepository;
		this.gravatarService = gravatarService;
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

		int downloaded = 0;
		int skipped = 0;

		for (User user : usersWithoutAvatar) {
			try {
				String email = user.getEmail() != null ? user.getEmail().getAddress() : null;
				if (email == null || email.isBlank()) {
					skipped++;
					continue;
				}

				Optional<byte[]> imageBytes = gravatarService.fetchProfilePicture(email);
				if (imageBytes.isPresent()) {
					String storedFilename = uploadService.storeBytes(imageBytes.get(), "png", 1024 * 1024);
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
}
