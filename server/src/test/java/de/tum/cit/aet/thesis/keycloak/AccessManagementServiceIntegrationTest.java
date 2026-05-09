package de.tum.cit.aet.thesis.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.AccessManagementService;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;
import org.junit.jupiter.api.Test;
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

	private User createLocalUser(String username) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeaderFor(username)))
				.andExpect(status().isOk());

		return userRepository.findByUniversityId(username).orElseThrow();
	}

	private Set<String> getGroupNames(User user) {
		// Refresh user from DB to get current groups
		User refreshed = userRepository.findById(user.getId()).orElseThrow();
		return refreshed.getGroups().stream()
				.map(g -> g.getId().getGroup())
				.collect(Collectors.toSet());
	}

	@Test
	void testGetUserByUsername_Admin() {
		KeycloakUserInformation user = accessManagementService.getUserByUsername("admin");

		assertNotNull(user);
		assertEquals("admin", user.username());
		assertEquals("admin@test.local", user.email());
	}

	@Test
	void testGetUserByUsername_ExaminerHasMatriculationNumber() {
		KeycloakUserInformation user = accessManagementService.getUserByUsername("examiner");

		assertNotNull(user);
		assertEquals("examiner", user.username());
		assertEquals("03700001", user.getMatriculationNumber());
	}

	@Test
	void testAssignSupervisorRole() throws Exception {
		User student = createLocalUser("student");

		accessManagementService.assignSupervisorRole(student);

		Set<String> roleNames = getGroupNames(student);
		assertTrue(roleNames.contains("supervisor"));
		assertTrue(roleNames.contains("advisor"));
		assertFalse(roleNames.contains("student"));
	}

	@Test
	void testAssignAdvisorRole() throws Exception {
		User student = createLocalUser("student");

		accessManagementService.assignAdvisorRole(student);

		Set<String> roleNames = getGroupNames(student);
		assertTrue(roleNames.contains("advisor"));
		assertFalse(roleNames.contains("supervisor"));
		assertFalse(roleNames.contains("student"));
	}

	@Test
	void testAssignGroupAdminRole() throws Exception {
		User supervisor = createLocalUserWithGroups("supervisor", "advisor");

		accessManagementService.assignGroupAdminRole(supervisor);

		Set<String> roleNames = getGroupNames(supervisor);
		assertTrue(roleNames.contains("group-admin"));
		assertTrue(roleNames.contains("advisor"), "Original advisor role should be preserved");
	}

	@Test
	void testRemoveGroupAdminRole() throws Exception {
		User supervisor = createLocalUser("supervisor");

		// First assign group-admin, then remove it
		accessManagementService.assignGroupAdminRole(supervisor);
		accessManagementService.removeGroupAdminRole(supervisor);

		Set<String> roleNames = getGroupNames(supervisor);
		assertFalse(roleNames.contains("group-admin"));
	}

	@Test
	void testRemoveResearchGroupRoles() throws Exception {
		User student = createLocalUser("student");

		// First assign supervisor role so the user has research group roles
		accessManagementService.assignSupervisorRole(student);

		// Now remove all research group roles
		accessManagementService.removeResearchGroupRoles(student);

		Set<String> roleNames = getGroupNames(student);
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
		User supervisor = createLocalUser("supervisor");

		accessManagementService.addStudentGroup(supervisor);

		Set<String> groupsAfterAdd = getGroupNames(supervisor);
		assertTrue(groupsAfterAdd.contains("student"));

		accessManagementService.removeStudentGroup(supervisor);

		Set<String> groupsAfterRemove = getGroupNames(supervisor);
		assertFalse(groupsAfterRemove.contains("student"));
	}

}
