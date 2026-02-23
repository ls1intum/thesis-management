package de.tum.cit.aet.thesis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

/**
 * Fetches profile pictures from an external avatar service.
 * Requests are made server-side so that the user's IP address is not exposed to the external service.
 */
@Service
public class GravatarService {
	private static final Logger log = LoggerFactory.getLogger(GravatarService.class);

	private static final String AVATAR_LOOKUP_URL = "https://www.gravatar.com/avatar/";

	/**
	 * Looks up a profile picture for the given email address.
	 *
	 * @param email the email address to look up
	 * @return the image bytes if a profile picture exists, empty otherwise
	 */
	public Optional<byte[]> fetchProfilePicture(String email) {
		if (email == null || email.isBlank()) {
			return Optional.empty();
		}

		String hash = sha256Hex(email.trim().toLowerCase());
		String lookupUrl = AVATAR_LOOKUP_URL + hash + "?s=400&d=404";

		try (HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build()) {

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(lookupUrl))
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();

			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

			if (response.statusCode() != 200) {
				return Optional.empty();
			}

			try (InputStream body = response.body()) {
				return Optional.of(body.readAllBytes());
			}
		} catch (Exception e) {
			log.warn("Failed to fetch profile picture for email hash {}: {}", hash, e.getMessage());
			return Optional.empty();
		}
	}

	private static String sha256Hex(String input) {
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
