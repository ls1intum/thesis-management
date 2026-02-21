package de.tum.cit.aet.thesis.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.UserGroup;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.AccessManagementService;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class AccessManagementServiceIntegrationTest extends BaseKeycloakIntegrationTest {

	@Autowired
	private AccessManagementService accessManagementService;

	@Autowired
	private UserRepository userRepository;

	/**
	 * Resets Keycloak state that may have been mutated by tests.
	 * Runs after every test so cleanup happens even when assertions fail.
	 */
	@AfterEach
	void resetKeycloakState() {
		Keycloak adminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
		String appClientUuid = adminClient.realm("thesis-management")
				.clients().findByClientId("thesis-management-app").get(0).getId();

		// Reset "student" user's client roles back to only "student"
		resetUserRoles(adminClient, appClientUuid, "student", List.of("student"));

		// Reset "advisor" user's client roles back to only "advisor"
		resetUserRoles(adminClient, appClientUuid, "advisor", List.of("advisor"));

		// Remove "advisor" user from thesis-students group if present
		String advisorKeycloakId = adminClient.realm("thesis-management")
				.users().search("advisor", true).get(0).getId();
		List<GroupRepresentation> advisorGroups = adminClient.realm("thesis-management")
				.users().get(advisorKeycloakId).groups();
		advisorGroups.stream()
				.filter(g -> g.getName().equals("thesis-students"))
				.findFirst()
				.ifPresent(g -> adminClient.realm("thesis-management")
						.users().get(advisorKeycloakId).leaveGroup(g.getId()));
	}

	private void resetUserRoles(Keycloak adminClient, String appClientUuid, String username, List<String> expectedRoles) {
		String keycloakUserId = adminClient.realm("thesis-management")
				.users().search(username, true).get(0).getId();
		List<RoleRepresentation> currentRoles = adminClient.realm("thesis-management")
				.users().get(keycloakUserId).roles().clientLevel(appClientUuid).listAll();
		if (!currentRoles.isEmpty()) {
			adminClient.realm("thesis-management")
					.users().get(keycloakUserId).roles().clientLevel(appClientUuid).remove(currentRoles);
		}
		List<RoleRepresentation> rolesToAssign = expectedRoles.stream()
				.map(roleName -> adminClient.realm("thesis-management")
						.clients().get(appClientUuid).roles().get(roleName).toRepresentation())
				.toList();
		adminClient.realm("thesis-management")
				.users().get(keycloakUserId).roles().clientLevel(appClientUuid).add(rolesToAssign);
	}

	private User createLocalUser(String username) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeaderFor(username)))
				.andExpect(status().isOk());

		return userRepository.findByUniversityId(username).orElseThrow();
	}

	@Test
	void testGetUserByUsername_Admin() {
		KeycloakUserInformation user = accessManagementService.getUserByUsername("admin");

		assertNotNull(user);
		assertEquals("admin", user.username());
		assertEquals("admin@test.local", user.email());
	}

	@Test
	void testGetUserByUsername_SupervisorHasMatriculationNumber() {
		KeycloakUserInformation user = accessManagementService.getUserByUsername("supervisor");

		assertNotNull(user);
		assertEquals("supervisor", user.username());
		assertEquals("03700001", user.getMatriculationNumber());
	}

	@Test
	void testSyncRolesFromKeycloakToDatabase_Admin() throws Exception {
		User admin = createLocalUser("admin");

		Set<UserGroup> groups = accessManagementService.syncRolesFromKeycloakToDatabase(admin);

		Set<String> roleNames = groups.stream()
				.map(g -> g.getId().getGroup())
				.collect(Collectors.toSet());
		assertTrue(roleNames.contains("admin"));
	}

	@Test
	void testSyncRolesFromKeycloakToDatabase_Student() throws Exception {
		User student = createLocalUser("student");

		Set<UserGroup> groups = accessManagementService.syncRolesFromKeycloakToDatabase(student);

		Set<String> roleNames = groups.stream()
				.map(g -> g.getId().getGroup())
				.collect(Collectors.toSet());
		assertTrue(roleNames.contains("student"));
	}

	@Test
	void testAssignSupervisorRole() throws Exception {
		User student = createLocalUser("student");

		accessManagementService.assignSupervisorRole(student);

		Keycloak adminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
		String keycloakUserId = adminClient.realm("thesis-management")
				.users().search("student", true).get(0).getId();
		String appClientUuid = adminClient.realm("thesis-management")
				.clients().findByClientId("thesis-management-app").get(0).getId();
		List<RoleRepresentation> roles = adminClient.realm("thesis-management")
				.users().get(keycloakUserId).roles().clientLevel(appClientUuid).listAll();

		Set<String> roleNames = roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
		assertTrue(roleNames.contains("supervisor"));
		assertTrue(roleNames.contains("advisor"));
		assertFalse(roleNames.contains("student"));
	}

	@Test
	void testAssignAdvisorRole() throws Exception {
		User student = createLocalUser("student");

		accessManagementService.assignAdvisorRole(student);

		Keycloak adminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
		String keycloakUserId = adminClient.realm("thesis-management")
				.users().search("student", true).get(0).getId();
		String appClientUuid = adminClient.realm("thesis-management")
				.clients().findByClientId("thesis-management-app").get(0).getId();
		List<RoleRepresentation> roles = adminClient.realm("thesis-management")
				.users().get(keycloakUserId).roles().clientLevel(appClientUuid).listAll();

		Set<String> roleNames = roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
		assertTrue(roleNames.contains("advisor"));
		assertFalse(roleNames.contains("supervisor"));
		assertFalse(roleNames.contains("student"));
	}

	@Test
	void testAssignGroupAdminRole() throws Exception {
		User advisor = createLocalUser("advisor");

		accessManagementService.assignGroupAdminRole(advisor);

		Keycloak adminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
		String keycloakUserId = adminClient.realm("thesis-management")
				.users().search("advisor", true).get(0).getId();
		String appClientUuid = adminClient.realm("thesis-management")
				.clients().findByClientId("thesis-management-app").get(0).getId();
		List<RoleRepresentation> roles = adminClient.realm("thesis-management")
				.users().get(keycloakUserId).roles().clientLevel(appClientUuid).listAll();

		Set<String> roleNames = roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
		assertTrue(roleNames.contains("group-admin"));
		assertTrue(roleNames.contains("advisor"), "Original advisor role should be preserved");
	}

	@Test
	void testRemoveGroupAdminRole() throws Exception {
		User advisor = createLocalUser("advisor");

		// First assign group-admin, then remove it
		accessManagementService.assignGroupAdminRole(advisor);
		accessManagementService.removeGroupAdminRole(advisor);

		Keycloak adminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
		String keycloakUserId = adminClient.realm("thesis-management")
				.users().search("advisor", true).get(0).getId();
		String appClientUuid = adminClient.realm("thesis-management")
				.clients().findByClientId("thesis-management-app").get(0).getId();
		List<RoleRepresentation> roles = adminClient.realm("thesis-management")
				.users().get(keycloakUserId).roles().clientLevel(appClientUuid).listAll();

		Set<String> roleNames = roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
		assertFalse(roleNames.contains("group-admin"));
	}

	@Test
	void testRemoveResearchGroupRoles() throws Exception {
		User student = createLocalUser("student");

		// First assign supervisor role so the user has research group roles
		accessManagementService.assignSupervisorRole(student);

		// Now remove all research group roles
		accessManagementService.removeResearchGroupRoles(student);

		Keycloak adminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
		String keycloakUserId = adminClient.realm("thesis-management")
				.users().search("student", true).get(0).getId();
		String appClientUuid = adminClient.realm("thesis-management")
				.clients().findByClientId("thesis-management-app").get(0).getId();
		List<RoleRepresentation> roles = adminClient.realm("thesis-management")
				.users().get(keycloakUserId).roles().clientLevel(appClientUuid).listAll();

		Set<String> roleNames = roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
		assertTrue(roleNames.contains("student"), "Student role should be reassigned");
		assertFalse(roleNames.contains("advisor"), "Advisor role should be removed");
		assertFalse(roleNames.contains("supervisor"), "Supervisor role should be removed");
		assertFalse(roleNames.contains("group-admin"), "Group-admin role should be removed");
	}

	@Test
	void testGetAllUsers() {
		List<KeycloakUserInformation> users = accessManagementService.getAllUsers("student");

		assertNotNull(users);
		assertFalse(users.isEmpty(), "Should find at least one user matching 'student'");

		Set<String> usernames = users.stream()
				.map(KeycloakUserInformation::username)
				.collect(Collectors.toSet());
		assertTrue(usernames.contains("student"), "Results should include the 'student' user");
	}

	@Test
	void testAddAndRemoveStudentGroup() throws Exception {
		User advisor = createLocalUser("advisor");

		Keycloak adminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
		String keycloakUserId = adminClient.realm("thesis-management")
				.users().search("advisor", true).get(0).getId();

		accessManagementService.addStudentGroup(advisor);

		List<GroupRepresentation> groups = adminClient.realm("thesis-management")
				.users().get(keycloakUserId).groups();
		assertTrue(groups.stream().anyMatch(g -> g.getName().equals("thesis-students")));

		accessManagementService.removeStudentGroup(advisor);

		groups = adminClient.realm("thesis-management")
				.users().get(keycloakUserId).groups();
		assertFalse(groups.stream().anyMatch(g -> g.getName().equals("thesis-students")));
	}

}
