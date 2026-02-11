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
		String studentKeycloakId = adminClient.realm("thesis-management")
				.users().search("student", true).get(0).getId();
		List<RoleRepresentation> currentRoles = adminClient.realm("thesis-management")
				.users().get(studentKeycloakId).roles().clientLevel(appClientUuid).listAll();
		if (!currentRoles.isEmpty()) {
			adminClient.realm("thesis-management")
					.users().get(studentKeycloakId).roles().clientLevel(appClientUuid).remove(currentRoles);
		}
		RoleRepresentation studentRole = adminClient.realm("thesis-management")
				.clients().get(appClientUuid).roles().get("student").toRepresentation();
		adminClient.realm("thesis-management")
				.users().get(studentKeycloakId).roles().clientLevel(appClientUuid).add(List.of(studentRole));

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
