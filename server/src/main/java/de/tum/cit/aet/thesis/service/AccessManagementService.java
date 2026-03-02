package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.UserGroup;
import de.tum.cit.aet.thesis.entity.key.UserGroupId;
import de.tum.cit.aet.thesis.repository.UserGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages user roles and group assignments in Keycloak and synchronizes them with the local database.
 */
@Service
public class AccessManagementService {
	private static final Logger log = LoggerFactory.getLogger(AccessManagementService.class);

	private final WebClient webClient;

	private final String keycloakRealmName;
	private final String serviceClientId;
	private final String serviceClientSecret;
	private final String clientId;
	private final UUID applicationClientUUID;
	private final UUID studentGroupId;

	private volatile String accessToken;
	private volatile Instant tokenExpiration;

	private final UserGroupRepository userGroupRepository;

	/**
	 * Initializes the Keycloak WebClient and resolves the application client UUID and student group ID.
	 *
	 * @param keycloakHost the Keycloak server host URL
	 * @param keycloakRealmName the Keycloak realm name
	 * @param serviceClientId the service client ID for authentication
	 * @param serviceClientSecret the service client secret for authentication
	 * @param studentGroupName the name of the student group in Keycloak
	 * @param clientId the application client ID
	 * @param userGroupRepository the user group repository
	 */
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

		UUID applicationClientUUID = null;
		try {
			applicationClientUUID = clientId.isBlank() || serviceClientSecret.isBlank() ? null : getApplicationClientUUID();
		} catch (RuntimeException exception) {
			log.warn("Could not fetch client id from configured service client", exception);
		}
		this.applicationClientUUID = applicationClientUUID;

		UUID studentGroupId = null;
		try {
			studentGroupId = studentGroupName.isBlank() || serviceClientSecret.isBlank() ? null : getGroupId(studentGroupName);
		} catch (RuntimeException exception) {
			log.warn("Could not fetch group id from configured student group", exception);
		}
		this.studentGroupId = studentGroupId;
	}

	/**
	 * Assigns the configured student Keycloak group to the given user.
	 *
	 * @param user the user to assign the student group to
	 */
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

	/**
	 * Removes the configured student Keycloak group from the given user.
	 *
	 * @param user the user to remove the student group from
	 */
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
				.uri("/admin/realms/" + keycloakRealmName + "/users/" + userId + "/groups/" + groupId)
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
				.uri("/admin/realms/" + keycloakRealmName + "/users/" + userId + "/groups/" + groupId)
				.headers(headers -> headers.addAll(getAuthenticationHeaders()))
				.retrieve()
				.bodyToMono(Void.class)
				.block();
	}


	/**
	 * Assigns the advisor Keycloak role to the user and removes any conflicting supervisor or student roles.
	 *
	 * @param user the user to assign the advisor role to
	 */
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

	/**
	 * Assigns the supervisor and advisor Keycloak roles to the user and removes the student role.
	 *
	 * @param user the user to assign the supervisor role to
	 */
	public void assignSupervisorRole(User user) {
		if (user == null) {
			throw new RuntimeException("User is null");
		}

		try {
			UUID userId = getUserId(user.getUniversityId());
			// As of right now supervisors also get the role advisor by default
			assignKeycloakRole(userId, "supervisor");
			assignKeycloakRole(userId, "advisor");

			removeKeycloakRole(userId, "student");
		} catch (RuntimeException exception) {
			log.warn("Could not assign supervisor role to user", exception);
		}
	}

	/**
	 * Assigns the group-admin Keycloak role to the given user.
	 *
	 * @param user the user to assign the group-admin role to
	 */
	public void assignGroupAdminRole(User user) {
		if (user == null) {
			throw new RuntimeException("User is null");
		}

		try {
			UUID userId = getUserId(user.getUniversityId());
			assignKeycloakRole(userId, "group-admin");
		} catch (RuntimeException exception) {
			log.warn("Could not assign groupadmin role to user", exception);
		}
	}

	/**
	 * Removes the group-admin Keycloak role from the given user.
	 *
	 * @param user the user to remove the group-admin role from
	 */
	public void removeGroupAdminRole(User user) {
		if (user == null) {
			throw new RuntimeException("User is null");
		}

		try {
			UUID userId = getUserId(user.getUniversityId());
			removeKeycloakRole(userId, "group-admin");
		} catch (RuntimeException exception) {
			log.warn("Could not remove groupadmin role from user", exception);
		}
	}

	/**
	 * Removes all research group-related Keycloak roles from the user and reassigns the student role.
	 *
	 * @param user the user to remove research group roles from
	 */
	public void removeResearchGroupRoles(User user) {
		if (user == null) {
			throw new RuntimeException("User is null");
		}

		UUID userId = getUserId(user.getUniversityId());
		assignKeycloakRole(userId, "student");

		removeKeycloakRole(userId, "advisor");
		removeKeycloakRole(userId, "supervisor");
		removeKeycloakRole(userId, "group-admin");
	}

	private void removeKeycloakRole(UUID userId, String roleName) {
		Role roleObject = getClientRoleByName(roleName);

		webClient.method(HttpMethod.DELETE)
				.uri("/admin/realms/" + keycloakRealmName + "/users/" + userId + "/role-mappings/clients/" + applicationClientUUID)
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
				.uri("/admin/realms/" + keycloakRealmName + "/users/" + userId + "/role-mappings/clients/" + applicationClientUUID)
				.headers(headers -> headers.addAll(getAuthenticationHeaders()))
				.bodyValue(List.of(roleObject))
				.retrieve()
				.toBodilessEntity()
				.block();
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public Set<UserGroup> syncRolesFromKeycloakToDatabase(User user) {
		if (user == null) {
			throw new IllegalArgumentException("User cannot be null");
		}

		UUID keycloakUserId = getUserId(user.getUniversityId());

		// Fetch all client roles from Keycloak for the given user
		List<Role> keycloakRoles = webClient.get()
				.uri("/admin/realms/" + keycloakRealmName + "/users/" + keycloakUserId + "/role-mappings/clients/" + applicationClientUUID)
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
				.uri("/admin/realms/" + keycloakRealmName + "/clients/" + applicationClientUUID + "/roles/" + roleName)
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
			tokenExpiration = Instant.now().plus(30, ChronoUnit.SECONDS);
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
	private UUID getApplicationClientUUID() {
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

	/**
	 * Fetches a user by their username from Keycloak.
	 * In case of Tum the username is the university ID.
	 *
	 * @param username the Keycloak username to search for
	 * @return the Keycloak user information
	 */
	public KeycloakUserInformation getUserByUsername(String username) {
		List<KeycloakUserInformation> users = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/admin/realms/" + keycloakRealmName + "/users")
						.queryParam("username", username)
						.queryParam("exact", "true")
						.queryParam("briefRepresentation", "false")
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

	/**
	 * Represents user information retrieved from Keycloak, including optional custom attributes.
	 *
	 * @param id the Keycloak user ID
	 * @param username the Keycloak username
	 * @param firstName the user's first name
	 * @param lastName the user's last name
	 * @param email the user's email address
	 * @param attributes the custom user attributes from Keycloak
	 */
	public record KeycloakUserInformation(UUID id, String username, String firstName, String lastName, String email, Map<String, List<String>> attributes) {
		/**
		 * Returns the matriculation number from the Keycloak user attributes, or null if not present.
		 *
		 * @return the matriculation number, or null if not available
		 */
		public String getMatriculationNumber() {
			if (attributes == null) {
				return null;
			}
			List<String> values = attributes.get("matrikelnr");
			if (values == null || values.isEmpty()) {
				return null;
			}
			return values.getFirst();
		}
	}

	/**
	 * Searches for users in Keycloak matching the given search key.
	 *
	 * @param searchKey the search key to match users against
	 * @return the list of matching Keycloak users
	 */
	public List<KeycloakUserInformation> getAllUsers(String searchKey) {
		try {
			return webClient.get()
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
		} catch (RuntimeException exception) {
			throw new RuntimeException("Could not fetch users from keycloak", exception);
		}
	}
}
