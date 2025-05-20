package de.tum.cit.aet.thesis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import de.tum.cit.aet.thesis.entity.User;

import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccessManagementService {
    private static final Logger log = LoggerFactory.getLogger(AccessManagementService.class);

    private final WebClient webClient;

    private final String keycloakRealmName;
    private final String serviceClientId;
    private final String serviceClientSecret;

    private final UUID serviceClientUUID;
    private final UUID studentGroupId;

    private String accessToken;
    private Instant tokenExpiration;

    @Autowired
    public AccessManagementService(
            @Value("${thesis-management.keycloak.host}") String keycloakHost,
            @Value("${thesis-management.keycloak.realm-name}") String keycloakRealmName,
            @Value("${thesis-management.keycloak.service-client.id}") String serviceClientId,
            @Value("${thesis-management.keycloak.service-client.secret}") String serviceClientSecret,
            @Value("${thesis-management.keycloak.service-client.student-group-name}") String studentGroupName
    ) {
        this.keycloakRealmName = keycloakRealmName;
        this.serviceClientId = serviceClientId;
        this.serviceClientSecret = serviceClientSecret;

        this.webClient = WebClient.builder()
                .baseUrl(keycloakHost)
                .build();

        UUID serviceClientUUID = null;
        try {
            serviceClientUUID = serviceClientId.isBlank() || serviceClientSecret.isBlank() ? null : getServiceClientUUID();
        } catch (RuntimeException exception) {
            log.warn("Could not fetch client id from configured service client", exception);
        }
        this.serviceClientUUID = serviceClientUUID;

        UUID studentGroupId = null;
        try {
            studentGroupId = studentGroupName.isBlank() || serviceClientSecret.isBlank() ? null : getGroupId(studentGroupName);
        } catch (RuntimeException exception) {
            log.warn("Could not fetch group id from configured student group", exception);
        }
        this.studentGroupId = studentGroupId;
    }

    public void addStudentGroup(User user) {
        if (studentGroupId == null) {
            return;
        }

        try {
            assignKeycloakGroup(getUserId(user.getUniversityId()), studentGroupId);
        } catch (RuntimeException exception) {
            log.warn("Could not assign keycloak group to user", exception);
        }
    }

    public void removeStudentGroup(User user) {
        if (studentGroupId == null) {
            return;
        }

        try {
            removeKeycloakGroup(getUserId(user.getUniversityId()), studentGroupId);
        } catch (RuntimeException exception) {
            log.warn("Could not remove keycloak group from user", exception);
        }
    }

    private void assignKeycloakGroup(UUID userId, UUID groupId) {
        if (userId == null || groupId == null) {
            throw new RuntimeException("User id or group id is null");
        }

        webClient.method(HttpMethod.PUT)
                .uri("/admin/realms/" + keycloakRealmName + "/users/" + userId +"/groups/" + groupId)
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private void removeKeycloakGroup(UUID userId, UUID groupId) {
        if (userId == null || groupId == null) {
            throw new RuntimeException("User id or group id is null");
        }

        webClient.method(HttpMethod.DELETE)
                .uri("/admin/realms/" + keycloakRealmName + "/users/" + userId +"/groups/" + groupId)
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }


    public void assignAdvisorRole(User user) {
        if (user == null) {
            throw new RuntimeException("User is null");
        }

        try {
            UUID userId = getUserId(user.getUniversityId());
            assignKeycloakRole(userId, "advisor");

            removeKeycloakRole(userId, "supervisor");
            removeKeycloakRole(userId, "student");
        } catch (RuntimeException exception) {
            log.warn("Could not assign advisor role to user", exception);
        }
    }

    public void assignSupervisorRole(User user) {
        if (user == null) {
            throw new RuntimeException("User is null");
        }

        try {
            UUID userId = getUserId(user.getUniversityId());
            assignKeycloakRole(userId, "supervisor");

            removeKeycloakRole(userId, "advisor");
            removeKeycloakRole(userId, "student");
        } catch (RuntimeException exception) {
            log.warn("Could not assign supervisor role to user", exception);
        }
    }

    public void removeResearchGroupRoles(User user) {
        if (user == null) {
            throw new RuntimeException("User is null");
        }

        try {
            UUID userId = getUserId(user.getUniversityId());
            assignKeycloakRole(userId, "student");

            removeKeycloakRole(userId, "advisor");
            removeKeycloakRole(userId, "supervisor");
        } catch (RuntimeException exception) {
            log.warn("Could not remove supervisor and/or advisor role from user", exception);
        }
    }

    private void removeKeycloakRole(UUID userId, String roleName) {
        Role roleObject = getClientRoleByName(roleName);

        webClient.method(HttpMethod.DELETE)
                .uri("/admin/realms/" + keycloakRealmName + "/users/" + userId + "/role-mappings/clients/" + serviceClientUUID)
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .bodyValue(List.of(roleObject))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private void assignKeycloakRole(UUID userId, String role) {
        if (userId == null || role == null) {
            throw new RuntimeException("User id or role is null");
        }

        Role roleObject = getClientRoleByName(role);

        webClient.post()
                .uri("/admin/realms/" + keycloakRealmName + "/users/" + userId + "/role-mappings/clients/" + serviceClientUUID)
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .bodyValue(List.of(roleObject))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private record Role(String id, String name, String description, boolean composite, boolean clientRole, String containerId) {}

    private Role getClientRoleByName(String roleName) {
        return webClient.get()
                .uri("/admin/realms/" + keycloakRealmName + "/clients/" + serviceClientUUID + "/roles/" + roleName)
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .retrieve()
                .bodyToMono(Role.class)
                .block();
    }

    private record TokensResponse(String access_token) { }

    private HttpHeaders getAuthenticationHeaders() {
        if (tokenExpiration == null || Instant.now().isAfter(tokenExpiration)) {
            TokensResponse response = webClient.post()
                    .uri("/realms/" + keycloakRealmName + "/protocol/openid-connect/token")
                    .body(
                            BodyInserters.fromFormData("grant_type", "client_credentials")
                                    .with("client_id", serviceClientId)
                                    .with("client_secret", serviceClientSecret)
                    )
                    .retrieve()
                    .bodyToMono(TokensResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Access token not returned");
            }

            accessToken = response.access_token();
            tokenExpiration = Instant.now().plus(300, ChronoUnit.SECONDS);
        }

        HttpHeaders authenticationHeaders = new HttpHeaders();
        authenticationHeaders.set("Authorization", "Bearer " + accessToken);

        return authenticationHeaders;
    }

    private record GroupElement(UUID id, String name) {}

    private UUID getGroupId(String groupName) {
        List<GroupElement> groups = webClient.method(HttpMethod.GET)
                .uri("/admin/realms/" + keycloakRealmName + "/groups")
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .retrieve()
                .bodyToFlux(GroupElement.class)
                .collectList()
                .block();

        if (groups == null) {
            throw new RuntimeException("Groups request was empty");
        }

        return groups.stream()
                .filter(group -> group.name().equals(groupName))
                .findFirst()
                .map(GroupElement::id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupName));
    }

    private record ClientElement(UUID id, String clientId, String name) {}
    private UUID getServiceClientUUID() {
        //TODO: application.yml?
        String clientId = "thesis-management-app";

        ClientElement client = webClient.method(HttpMethod.GET)
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/" + keycloakRealmName + "/clients")
                        .queryParam("clientId", clientId)
                        .build())
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .retrieve()
                .bodyToFlux(ClientElement.class)
                .next()
                .block();

        if (client == null) {
            throw new IllegalStateException("Client not found: " + clientId);
        }

        return client.id();
    }

    public KeycloakUserElement getUserByUsername(String username) {
        List<KeycloakUserElement> users = webClient.method(HttpMethod.GET)
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/" + keycloakRealmName + "/users")
                        .queryParam("username", username)
                        .build()
                )
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .retrieve()
                .bodyToFlux(KeycloakUserElement.class)
                .collectList()
                .block();

        if (users == null) {
            throw new RuntimeException("Users request was empty");
        }

        return users.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private UUID getUserId(String username) {
        return getUserByUsername(username).id;
    }

    public record KeycloakUserElement(UUID id, String username, String firstName, String lastName , String email) {}

    public List<KeycloakUserElement> getAllUsers(String searchKey) {
        try {
            List<KeycloakUserElement> users = webClient.method(HttpMethod.GET)
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/" + keycloakRealmName + "/users")
                            .queryParam("search", searchKey)
                            .build()
                    )
                    .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                    .retrieve()
                    .bodyToFlux(KeycloakUserElement.class)
                    .collectList()
                    .block();
            return users;
        } catch (RuntimeException exception) {
            log.warn("Could not fetch users from keycloak", exception);
            return new ArrayList<>();
        }
    }
}
