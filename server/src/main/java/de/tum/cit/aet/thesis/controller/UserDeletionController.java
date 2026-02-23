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

@Slf4j
@RestController
@RequestMapping("/v2/user-deletion")
public class UserDeletionController {
	private final UserDeletionService userDeletionService;
	private final AuthenticationService authenticationService;

	public UserDeletionController(UserDeletionService userDeletionService, AuthenticationService authenticationService) {
		this.userDeletionService = userDeletionService;
		this.authenticationService = authenticationService;
	}

	@GetMapping("/me/preview")
	public ResponseEntity<UserDeletionPreviewDto> previewSelfDeletion(JwtAuthenticationToken jwt) {
		User user = authenticationService.getAuthenticatedUser(jwt);
		return ResponseEntity.ok(userDeletionService.previewDeletion(user.getId()));
	}

	@DeleteMapping("/me")
	public ResponseEntity<UserDeletionResultDto> deleteSelf(JwtAuthenticationToken jwt) {
		User user = authenticationService.getAuthenticatedUser(jwt);
		UserDeletionResultDto result = userDeletionService.deleteOrAnonymizeUser(user.getId());
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{userId}/preview")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<UserDeletionPreviewDto> previewUserDeletion(@PathVariable UUID userId) {
		return ResponseEntity.ok(userDeletionService.previewDeletion(userId));
	}

	@DeleteMapping("/{userId}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<UserDeletionResultDto> deleteUser(@PathVariable UUID userId) {
		UserDeletionResultDto result = userDeletionService.deleteOrAnonymizeUser(userId);
		return ResponseEntity.ok(result);
	}
}
