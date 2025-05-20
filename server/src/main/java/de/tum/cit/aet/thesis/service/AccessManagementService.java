package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.UserGroup;
import de.tum.cit.aet.thesis.entity.key.UserGroupId;
import de.tum.cit.aet.thesis.repository.UserGroupRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import de.tum.cit.aet.thesis.entity.User;

import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AccessManagementService {
    private static final Logger log = LoggerFactory.getLogger(AccessManagementService.class);

    private final WebClient webClient;

    private final String keycloakRealmName;
    private final String serviceClientId;
    private final String serviceClientSecret;

    private final UUID clientUUID;
    private final UUID studentGroupId;

    private String accessToken;
    private Instant tokenExpiration;

    private String clientId;

    private final UserGroupRepository userGroupRepository;

    @Autowired
    public AccessManagementService(
            @Value("${thesis-management.keycloak.host}") String keycloakHost,
            @Value("${thesis-management.keycloak.realm-name}") String keycloakRealmName,
            @Value("${thesis-management.keycloak.service-client.id}") String serviceClientId,
            @Value("${thesis-management.keycloak.service-client.secret}") String serviceClientSecret,
            @Value("${thesis-management.keycloak.service-client.student-group-name}") String studentGroupName,
            @Value("${thesis-management.keycloak.client-id}") String clientId,
            UserGroupRepository userGroupRepository
    ) {
        this.userGroupRepository = userGroupRepository;
        this.keycloakRealmName = keycloakRealmName;
        this.serviceClientId = serviceClientId;
        this.serviceClientSecret = serviceClientSecret;
        this.clientId = clientId;

        this.webClient = WebClient.builder()
                .baseUrl(keycloakHost)
                .build();

        UUID clientUUID = null;
        try {
            clientUUID = clientId.isBlank() || serviceClientSecret.isBlank() ? null : getServiceClientUUID();
        } catch (RuntimeException exception) {
            log.warn("Could not fetch client id from configured service client", exception);
        }
        this.clientUUID = clientUUID;

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
                .uri("/admin/realms/" + keycloakRealmName + "/users/" + userId + "/role-mappings/clients/" + clientUUID)
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
                .uri("/admin/realms/" + keycloakRealmName + "/users/" + userId + "/role-mappings/clients/" + clientUUID)
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .bodyValue(List.of(roleObject))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Transactional
    public Set<UserGroup> syncRolesFromKeycloakToDatabase(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        UUID keycloakUserId = getUserId(user.getUniversityId());

        // Fetch all client roles from Keycloak for the given user
        List<Role> keycloakRoles = webClient.get()
                .uri("/admin/realms/" + keycloakRealmName + "/users/" + keycloakUserId + "/role-mappings/clients/" + clientUUID)
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .retrieve()
                .bodyToFlux(Role.class)
                .collectList()
                .block();

        // Delete old group assignments
        userGroupRepository.deleteByUserId(user.getId());

        // Create and persist new user groups
        Set<UserGroup> userGroups = new HashSet<>();

        if (keycloakRoles != null && !keycloakRoles.isEmpty()) {
            for (Role role : keycloakRoles) {
                UserGroup entity = new UserGroup();
                UserGroupId entityId = new UserGroupId();

                entityId.setUserId(user.getId());
                entityId.setGroup(role.name());

                entity.setUser(user);
                entity.setId(entityId);

                userGroups.add(userGroupRepository.save(entity));
            }
        }

        return userGroups;
    }

    private record Role(String id, String name, String description, boolean composite, boolean clientRole, String containerId) {}

    private Role getClientRoleByName(String roleName) {
        return webClient.get()
                .uri("/admin/realms/" + keycloakRealmName + "/clients/" + clientUUID + "/roles/" + roleName)
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

    public KeycloakUserInformation getUserByUsername(String username) {
        List<KeycloakUserInformation> users = webClient.method(HttpMethod.GET)
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/" + keycloakRealmName + "/users")
                        .queryParam("username", username)
                        .build()
                )
                .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                .retrieve()
                .bodyToFlux(KeycloakUserInformation.class)
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

    public record KeycloakUserInformation(UUID id, String username, String firstName, String lastName , String email) {}
    public List<KeycloakUserInformation> getAllUsers(String searchKey) {
        try {
            List<KeycloakUserInformation> users = webClient.method(HttpMethod.GET)
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/" + keycloakRealmName + "/users")
                            .queryParam("search", searchKey)
                            .build()
                    )
                    .headers(headers -> headers.addAll(getAuthenticationHeaders()))
                    .retrieve()
                    .bodyToFlux(KeycloakUserInformation.class)
                    .collectList()
                    .block();
            return users;
        } catch (RuntimeException exception) {
            log.warn("Could not fetch users from keycloak", exception);
            return new ArrayList<>();
        }
    }
}
