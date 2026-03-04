package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.UploadService;
import de.tum.cit.aet.thesis.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * REST controller for serving user avatar images.
 *
 * <p>Avatars are served publicly only for users who appear in publicly visible data
 * (published theses, open topics, or research group head listings). For all other users,
 * the requester must be authenticated. This prevents leaking profile pictures of students
 * who have no public presence in the system.</p>
 */
@Slf4j
@RestController
@RequestMapping("/v2/avatars")
public class AvatarController {
	private final UserService userService;
	private final UploadService uploadService;
	private final UserRepository userRepository;

	@Autowired
	public AvatarController(UserService userService, UploadService uploadService,
			UserRepository userRepository) {
		this.userService = userService;
		this.uploadService = uploadService;
		this.userRepository = userRepository;
	}

	/**
	 * Retrieves the avatar image for a user by their ID.
	 *
	 * <p>For unauthenticated requests, the avatar is only served if the user appears in
	 * publicly visible data (e.g. a finished thesis with PUBLIC visibility, an open topic,
	 * or as a research group head). Authenticated users can access any avatar.</p>
	 *
	 * @param userId the ID of the user
	 * @return the avatar image as a resource, or 404 if not found or not accessible
	 */
	@GetMapping("/{userId}")
	public ResponseEntity<Resource> getAvatar(@PathVariable UUID userId) {
		// If the request is unauthenticated, only serve avatars of publicly visible users
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal());

		if (!isAuthenticated && !userRepository.isUserPubliclyVisible(userId)) {
			return ResponseEntity.notFound().build();
		}

		User user = userService.findById(userId);
		String avatar = user.getAvatar();

		if (avatar == null || avatar.isBlank()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
				.body(uploadService.load(avatar));
	}
}
