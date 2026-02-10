package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;

import java.util.UUID;

/**
 * DTO for Keycloak user data returned by the admin API user search.
 *
 * <p>This DTO is used when looking up Keycloak users, e.g. to add supervisors, advisors, or
 * students to research groups. Including {@code matriculationNumber} here ensures that users
 * created through this flow (rather than through a regular login) also get their matriculation
 * number populated immediately, without having to wait for their first login or the backfill task.</p>
 */
public record KeycloakUserDto(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String email,
        String matriculationNumber,
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
                keycloakUser.getMatriculationNumber(),
                hasResearchGroup
        );
    }
}
