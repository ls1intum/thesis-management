package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.AccessManagementService;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.ServletException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
class UserControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccessManagementService accessManagementService;

	@Nested
	class GetUsers {
		@Test
		void getUsers_Unauthenticated_ReturnsUnauthorized() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void getUsers_Success_AsAdmin() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", adminAuth)
							.param("groups", "admin"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").isArray()).isTrue();
			assertThat(json.get("content").size()).isGreaterThanOrEqualTo(1);
			assertThat(json.get("totalElements").isNumber()).isTrue();
		}

		@Test
		void getUsers_Success_AsSupervisor() throws Exception {
			TestUser supervisor = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Test Group", supervisor.universityId());
			String supervisorAuth = generateTestAuthenticationHeader(supervisor.universityId(), List.of("supervisor"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", supervisorAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").isArray()).isTrue();
		}

		@Test
		void getUsers_Success_AsAdvisor() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("advisor"));
			createTestResearchGroup("Test Group", advisor.universityId());
			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("advisor"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", advisorAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").isArray()).isTrue();
		}

		@Test
		void getUsers_AsStudent_Forbidden() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isForbidden());
		}

		@Test
		void getUsers_FilterByGroups() throws Exception {
			createRandomTestUser(List.of("student"));
			createRandomTestUser(List.of("supervisor"));
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", adminAuth)
							.param("groups", "student"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			for (JsonNode user : json.get("content")) {
				assertThat(user.get("groups").toString()).contains("student");
			}
		}

		@Test
		void getUsers_SearchByName() throws Exception {
			TestUser user = createTestUser("searchuser123", List.of("student"));
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", adminAuth)
							.param("searchQuery", "searchuser123"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			JsonNode content = json.get("content");
			assertThat(content.size()).isGreaterThanOrEqualTo(1);

			List<String> userIds = new ArrayList<>();
			for (JsonNode u : content) {
				userIds.add(u.get("userId").asText());
			}
			assertThat(userIds).contains(user.userId().toString());
		}

		@Test
		void getUsers_Pagination() throws Exception {
			for (int i = 0; i < 3; i++) {
				createRandomTestUser(List.of("student"));
			}
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", adminAuth)
							.param("groups", "student")
							.param("page", "0")
							.param("limit", "2"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isLessThanOrEqualTo(2);
			assertThat(json.get("totalElements").asInt()).isGreaterThanOrEqualTo(3);
			assertThat(json.get("totalPages").asInt()).isGreaterThanOrEqualTo(2);
		}

		@Test
		void getUsers_SortByFirstName_Ascending() throws Exception {
			createTestUser("zzz_sortuser", List.of("student"));
			createTestUser("aaa_sortuser", List.of("student"));
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", adminAuth)
							.param("groups", "student")
							.param("sortBy", "firstName")
							.param("sortOrder", "asc"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode content = objectMapper.readTree(response).get("content");
			assertThat(content.size()).isGreaterThanOrEqualTo(2);

			for (int i = 1; i < content.size(); i++) {
				String prev = content.get(i - 1).get("firstName").asText();
				String curr = content.get(i).get("firstName").asText();
				assertThat(prev.compareToIgnoreCase(curr)).isLessThanOrEqualTo(0);
			}
		}

		@Test
		void getUsers_EmptyResult() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", adminAuth)
							.param("searchQuery", "nonexistentuserxyz999"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isZero();
			assertThat(json.get("totalElements").asInt()).isZero();
		}

		@Test
		void getUsers_VerifyResponseStructure() throws Exception {
			createTestUser("structuretest", List.of("student"));
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", adminAuth)
							.param("searchQuery", "structuretest"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isGreaterThanOrEqualTo(1);

			JsonNode firstUser = json.get("content").get(0);
			assertThat(firstUser.has("userId")).isTrue();
			assertThat(firstUser.has("universityId")).isTrue();
			assertThat(firstUser.has("firstName")).isTrue();
			assertThat(firstUser.has("lastName")).isTrue();
			assertThat(firstUser.has("email")).isTrue();
			assertThat(firstUser.has("groups")).isTrue();
			assertThat(firstUser.has("joinedAt")).isTrue();
		}

		@Test
		void getUsers_VerifyDatabaseState() throws Exception {
			createTestUser("dbcheck1", List.of("student"));
			createTestUser("dbcheck2", List.of("advisor"));
			String adminAuth = createRandomAdminAuthentication();

			long userCount = userRepository.count();
			assertThat(userCount).isGreaterThanOrEqualTo(3);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
							.header("Authorization", adminAuth)
							.param("groups", "student"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			boolean foundStudent = false;
			for (JsonNode u : json.get("content")) {
				if (u.get("universityId").asText().equals("dbcheck1")) {
					foundStudent = true;
					break;
				}
			}
			assertThat(foundStudent).isTrue();
		}
	}

	@Nested
	class GetKeycloakUsers {
		@BeforeEach
		void resetMocks() {
			reset(accessManagementService);
			// Re-establish baseline stubs from TestSecurityConfig
			doNothing().when(accessManagementService).assignSupervisorRole(any());
			when(accessManagementService.syncRolesFromKeycloakToDatabase(any()))
					.thenReturn(Collections.emptySet());
		}

		@Test
		void getKeycloakUsers_Success_AsAdmin() throws Exception {
			KeycloakUserInformation keycloakUser = new KeycloakUserInformation(
					UUID.randomUUID(), "kc-user-1", "John", "Doe", "john@example.com", Map.of()
			);
			when(accessManagementService.getAllUsers(any())).thenReturn(List.of(keycloakUser));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/keycloak")
							.header("Authorization", createRandomAdminAuthentication())
							.param("searchKey", "john"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.isArray()).isTrue();
			assertThat(json.size()).isEqualTo(1);
			assertThat(json.get(0).get("username").asText()).isEqualTo("kc-user-1");
			assertThat(json.get(0).get("firstName").asText()).isEqualTo("John");
			assertThat(json.get(0).get("lastName").asText()).isEqualTo("Doe");
			assertThat(json.get(0).get("email").asText()).isEqualTo("john@example.com");
		}

		@Test
		void getKeycloakUsers_WithExistingLocalUser() throws Exception {
			TestUser localUser = createTestUser("kc-existing-user", List.of("student"));

			KeycloakUserInformation keycloakUser = new KeycloakUserInformation(
					UUID.randomUUID(), "kc-existing-user", "Jane", "Doe", "jane@example.com", Map.of()
			);
			when(accessManagementService.getAllUsers(any())).thenReturn(List.of(keycloakUser));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/keycloak")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.isArray()).isTrue();
			assertThat(json.size()).isEqualTo(1);
			assertThat(json.get(0).get("username").asText()).isEqualTo("kc-existing-user");
		}

		@Test
		void getKeycloakUsers_EmptyResult() throws Exception {
			when(accessManagementService.getAllUsers(any())).thenReturn(List.of());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/keycloak")
							.header("Authorization", createRandomAdminAuthentication())
							.param("searchKey", "nonexistent"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.isArray()).isTrue();
			assertThat(json.size()).isZero();
		}

		@Test
		void getKeycloakUsers_MultipleResults() throws Exception {
			KeycloakUserInformation user1 = new KeycloakUserInformation(
					UUID.randomUUID(), "user-a", "Alice", "Smith", "alice@example.com", Map.of()
			);
			KeycloakUserInformation user2 = new KeycloakUserInformation(
					UUID.randomUUID(), "user-b", "Bob", "Jones", "bob@example.com", Map.of()
			);
			when(accessManagementService.getAllUsers(any())).thenReturn(List.of(user1, user2));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/keycloak")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.size()).isEqualTo(2);
		}

		@Test
		void getKeycloakUsers_AsStudent_Forbidden() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/keycloak")
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isForbidden());
		}

		@Test
		void getKeycloakUsers_AsAdvisor_Forbidden() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/keycloak")
							.header("Authorization", createRandomAuthentication("advisor")))
					.andExpect(status().isForbidden());
		}

		@Test
		void getKeycloakUsers_AsSupervisor_Forbidden() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/keycloak")
							.header("Authorization", createRandomAuthentication("supervisor")))
					.andExpect(status().isForbidden());
		}

		@Test
		void getKeycloakUsers_Success_AsGroupAdmin() throws Exception {
			when(accessManagementService.getAllUsers(any())).thenReturn(List.of());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/keycloak")
							.header("Authorization", createRandomAuthentication("group-admin")))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class DocumentDownloads {
		@Test
		void getExaminationReport_AccessDenied_AsDifferentStudent() throws Exception {
			TestUser targetUser = createRandomTestUser(List.of("student"));
			String differentStudentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/{userId}/examination-report", targetUser.userId())
							.header("Authorization", differentStudentAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void getCV_AccessDenied_AsDifferentStudent() throws Exception {
			TestUser targetUser = createRandomTestUser(List.of("student"));
			String differentStudentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/{userId}/cv", targetUser.userId())
							.header("Authorization", differentStudentAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void getDegreeReport_AccessDenied_AsDifferentStudent() throws Exception {
			TestUser targetUser = createRandomTestUser(List.of("student"));
			String differentStudentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/{userId}/degree-report", targetUser.userId())
							.header("Authorization", differentStudentAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void getExaminationReport_NotFound_NonExistentUser() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/{userId}/examination-report", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getCV_NotFound_NonExistentUser() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/{userId}/cv", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getDegreeReport_NotFound_NonExistentUser() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/users/{userId}/degree-report", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getExaminationReport_AccessAllowed_AsAdmin() throws Exception {
			TestUser targetUser = createRandomTestUser(List.of("student"));

			// Access control passes. No file uploaded so the controller throws (wrapped in
			// ServletException). We verify the exception is NOT an access-denied error.
			try {
				var result = mockMvc.perform(MockMvcRequestBuilders.get(
								"/v2/users/{userId}/examination-report", targetUser.userId())
								.header("Authorization", createRandomAdminAuthentication()))
						.andReturn();
				assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
			} catch (ServletException e) {
				assertThat(e).rootCause()
						.isNotInstanceOf(org.springframework.security.access.AccessDeniedException.class);
			}
		}

		@Test
		void getCV_AccessAllowed_AsSameUser() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			try {
				var result = mockMvc.perform(MockMvcRequestBuilders.get(
								"/v2/users/{userId}/cv", user.userId())
								.header("Authorization", auth))
						.andReturn();
				assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
			} catch (ServletException e) {
				assertThat(e).rootCause()
						.isNotInstanceOf(org.springframework.security.access.AccessDeniedException.class);
			}
		}

		@Test
		void getDegreeReport_AccessAllowed_AsSupervisor() throws Exception {
			TestUser targetUser = createRandomTestUser(List.of("student"));

			try {
				var result = mockMvc.perform(MockMvcRequestBuilders.get(
								"/v2/users/{userId}/degree-report", targetUser.userId())
								.header("Authorization", createRandomAuthentication("supervisor")))
						.andReturn();
				assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
			} catch (ServletException e) {
				assertThat(e).rootCause()
						.isNotInstanceOf(org.springframework.security.access.AccessDeniedException.class);
			}
		}
	}
}
