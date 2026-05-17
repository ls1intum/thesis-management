package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;

import java.util.Set;

/**
 * Minimal projection of a Keycloak directory entry for the "create thesis" student-search fallback.
 * Returned by {@code GET /v2/users/keycloak/students} so supervisors can pick a student who has not
 * logged in to the portal yet. The {@code existsLocally} flag lets the client tag entries that already
 * have a local user row (so it can deduplicate against the DB result set).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record KeycloakStudentDto(
		String username,
		String firstName,
		String lastName,
		String email,
		String matriculationNumber,
		boolean existsLocally
) {
	public static KeycloakStudentDto from(KeycloakUserInformation keycloakUser, Set<String> localUsernames) {
		return new KeycloakStudentDto(
				keycloakUser.username(),
				keycloakUser.firstName(),
				keycloakUser.lastName(),
				keycloakUser.email(),
				keycloakUser.getMatriculationNumber(),
				localUsernames.contains(keycloakUser.username())
		);
	}
}
