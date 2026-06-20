package de.tum.cit.aet.thesis.core.user.dto;

import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.service.AccessManagementService.KeycloakUserInformation;

import java.util.UUID;

public record KeycloakUserDto(
		UUID id,
		String username,
		String firstName,
		String lastName,
		String email,
		boolean hasResearchGroup
) {
	public static KeycloakUserDto from(KeycloakUserInformation keycloakUser, User systemUser) {
		boolean hasResearchGroup = systemUser != null && systemUser.getResearchGroup() != null;

		return new KeycloakUserDto(
				keycloakUser.id(),
				keycloakUser.username(),
				keycloakUser.firstName(),
				keycloakUser.lastName(),
				keycloakUser.email(),
				hasResearchGroup
		);
	}
}
