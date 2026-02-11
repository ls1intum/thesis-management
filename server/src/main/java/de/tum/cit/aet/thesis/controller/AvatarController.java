package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.UploadService;
import de.tum.cit.aet.thesis.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * REST controller for serving user avatar images.
 */
@Slf4j
@RestController
@RequestMapping("/v2/avatars")
public class AvatarController {
	private final UserService userService;
	private final UploadService uploadService;

	/**
	 * Injects the user service and upload service.
	 *
	 * @param userService the user service
	 * @param uploadService the upload service
	 */
	@Autowired
	public AvatarController(UserService userService, UploadService uploadService) {
		this.userService = userService;
		this.uploadService = uploadService;
	}

	/**
	 * Retrieves the avatar image for a user by their ID.
	 *
	 * @param userId the ID of the user
	 * @return the avatar image as a resource
	 */
	@GetMapping("/{userId}")
	public ResponseEntity<Resource> getTheses(@PathVariable UUID userId) {
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
