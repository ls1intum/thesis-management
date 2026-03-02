package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.UserDeletionPreviewDto;
import de.tum.cit.aet.thesis.dto.UserDeletionResultDto;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.AuthenticationService;
import de.tum.cit.aet.thesis.service.UserDeletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for handling user account deletion and anonymization.
 */
@Slf4j
@RestController
@RequestMapping("/v2/user-deletion")
public class UserDeletionController {
	private final UserDeletionService userDeletionService;
	private final AuthenticationService authenticationService;

	/**
	 * Constructs a new UserDeletionController with the required dependencies.
	 *
	 * @param userDeletionService the user deletion service
	 * @param authenticationService the authentication service
	 */
	public UserDeletionController(UserDeletionService userDeletionService, AuthenticationService authenticationService) {
		this.userDeletionService = userDeletionService;
		this.authenticationService = authenticationService;
	}

	/**
	 * Returns a preview of the data affected by deleting the authenticated user.
	 *
	 * @param jwt the authentication token
	 * @return the deletion preview
	 */
	@GetMapping("/me/preview")
	public ResponseEntity<UserDeletionPreviewDto> previewSelfDeletion(JwtAuthenticationToken jwt) {
		User user = authenticationService.getAuthenticatedUser(jwt);
		return ResponseEntity.ok(userDeletionService.previewDeletion(user.getId()));
	}

	/**
	 * Deletes or anonymizes the authenticated user's account.
	 *
	 * @param jwt the authentication token
	 * @return the deletion result
	 */
	@DeleteMapping("/me")
	public ResponseEntity<UserDeletionResultDto> deleteSelf(JwtAuthenticationToken jwt) {
		User user = authenticationService.getAuthenticatedUser(jwt);
		UserDeletionResultDto result = userDeletionService.deleteOrAnonymizeUser(user.getId());
		return ResponseEntity.ok(result);
	}

	/**
	 * Returns a preview of the data affected by deleting the specified user.
	 *
	 * @param userId the user identifier
	 * @return the deletion preview
	 */
	@GetMapping("/{userId}/preview")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<UserDeletionPreviewDto> previewUserDeletion(@PathVariable UUID userId) {
		return ResponseEntity.ok(userDeletionService.previewDeletion(userId));
	}

	/**
	 * Deletes or anonymizes the specified user's account.
	 *
	 * @param userId the user identifier
	 * @return the deletion result
	 */
	@DeleteMapping("/{userId}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<UserDeletionResultDto> deleteUser(@PathVariable UUID userId) {
		UserDeletionResultDto result = userDeletionService.deleteOrAnonymizeUser(userId);
		return ResponseEntity.ok(result);
	}
}
