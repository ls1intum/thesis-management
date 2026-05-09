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
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages user roles via the local database and provides user lookup from Keycloak.
 * Authorization (group assignments) is handled entirely through the user_groups table.
 * Keycloak is used only for authentication and user search.
 */
@Service
public class AccessManagementService {
	private static final Logger log = LoggerFactory.getLogger(AccessManagementService.class);

	private final WebClient webClient;

	private final String keycloakRealmName;
	private final String serviceClientId;
	private final String serviceClientSecret;

	private String accessToken;
	private Instant tokenExpiration;
	private final Object tokenLock = new Object();

	private final UserGroupRepository userGroupRepository;

	/**
	 * Constructs the access management service with Keycloak connection settings for user lookup
	 * and the user group repository for authorization changes.
	 *
	 * @param keycloakHost the Keycloak server URL
	 * @param keycloakRealmName the Keycloak realm name
	 * @param serviceClientId the service client ID for Keycloak API access
	 * @param serviceClientSecret the service client secret for Keycloak API access
	 * @param userGroupRepository the repository for managing user group assignments
	 */
	@Autowired
	public AccessManagementService(
			@Value("${thesis-management.keycloak.host}") String keycloakHost,
			@Value("${thesis-management.keycloak.realm-name}") String keycloakRealmName,
			@Value("${thesis-management.keycloak.service-client.id}") String serviceClientId,
			@Value("${thesis-management.keycloak.service-client.secret}") String serviceClientSecret,
			UserGroupRepository userGroupRepository
	) {
		this.userGroupRepository = userGroupRepository;
		this.keycloakRealmName = keycloakRealmName;
		this.serviceClientId = serviceClientId;
		this.serviceClientSecret = serviceClientSecret;

		this.webClient = WebClient.builder()
				.baseUrl(keycloakHost)
				.build();
	}

	/**
	 * Assigns the student group to the given user in the database.
	 *
	 * @param user the user to assign the student group to
	 */
	public void addStudentGroup(User user) {
		addGroup(user, "student");
	}

	/**
	 * Removes the student group from the given user in the database.
	 *
	 * @param user the user to remove the student group from
	 */
	public void removeStudentGroup(User user) {
		removeGroup(user, "student");
	}

	/**
	 * Assigns the advisor role to the user, removing any conflicting supervisor or student roles.
	 *
	 * @param user the user to assign the advisor role to
	 */
	public void assignAdvisorRole(User user) {
		removeGroup(user, "student");
		removeGroup(user, "supervisor");
		addGroup(user, "advisor");
	}

	/**
	 * Assigns the supervisor and advisor roles to the user and removes the student role.
	 *
	 * @param user the user to assign the supervisor role to
	 */
	public void assignSupervisorRole(User user) {
		removeGroup(user, "student");
		addGroup(user, "supervisor");
		addGroup(user, "advisor");
	}

	/**
	 * Assigns the group-admin role to the given user.
	 *
	 * @param user the user to assign the group-admin role to
	 */
	public void assignGroupAdminRole(User user) {
		addGroup(user, "group-admin");
	}

	/**
	 * Removes the group-admin role from the given user.
	 *
	 * @param user the user to remove the group-admin role from
	 */
	public void removeGroupAdminRole(User user) {
		removeGroup(user, "group-admin");
	}

	/**
	 * Removes all research group-related roles from the user and reassigns the student role.
	 *
	 * @param user the user to remove research group roles from
	 */
	public void removeResearchGroupRoles(User user) {
		removeGroup(user, "advisor");
		removeGroup(user, "supervisor");
		removeGroup(user, "group-admin");
		addGroup(user, "student");
	}

	private void addGroup(User user, String group) {
		java.util.Objects.requireNonNull(user, "user must not be null");
		userGroupRepository.insertIfNotExists(user.getId(), group);

		// Update in-memory set if the group isn't already present
		boolean alreadyPresent = user.getGroups().stream()
				.anyMatch(ug -> ug.getId().getGroup().equals(group));
		if (!alreadyPresent) {
			UserGroup entity = new UserGroup();
			UserGroupId entityId = new UserGroupId();

			entityId.setUserId(user.getId());
			entityId.setGroup(group);

			entity.setUser(user);
			entity.setId(entityId);

			user.getGroups().add(entity);
		}
	}

	private void removeGroup(User user, String group) {
		java.util.Objects.requireNonNull(user, "user must not be null");
		userGroupRepository.deleteByUserIdAndGroup(user.getId(), group);
		user.getGroups().removeIf(ug -> ug.getId().getGroup().equals(group));
	}

	private record TokensResponse(String access_token) { }

	private HttpHeaders getAuthenticationHeaders() {
		synchronized (tokenLock) {
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
