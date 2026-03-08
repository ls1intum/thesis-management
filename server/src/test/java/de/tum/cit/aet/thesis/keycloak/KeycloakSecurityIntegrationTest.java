package de.tum.cit.aet.thesis.keycloak;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class KeycloakSecurityIntegrationTest extends BaseKeycloakIntegrationTest {

	@Test
	void testPublicEndpointAccessibleWithoutAuth() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
						.param("onlyOwnResearchGroup", "false"))
				.andExpect(status().isOk());
	}

	@Test
	void testProtectedEndpointRequiresAuth() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testProtectedEndpointRejectsInvalidToken() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testAdminUserInfo() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeaderFor("admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.universityId").value("admin"));
	}

	@Test
	void testStudentUserInfo() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeaderFor("student")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.universityId").value("student"))
				.andExpect(jsonPath("$.groups", hasItem("student")));
	}

	@Test
	void testAdminCanAccessUsersEndpoint() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeaderFor("admin")))
				.andExpect(status().isOk());

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
						.header("Authorization", authHeaderFor("admin")))
				.andExpect(status().isOk());
	}

	@Test
	void testStudentCannotAccessUsersEndpoint() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeaderFor("student")))
				.andExpect(status().isOk());

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
						.header("Authorization", authHeaderFor("student")))
				.andExpect(status().isForbidden());
	}

	@Test
	void testExaminerPassesSecurityButNeedsResearchGroup() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeaderFor("examiner")))
				.andExpect(status().isOk());

		// Examiner passes @PreAuthorize but gets application-level 403
		// because no research group is assigned — this proves the JWT role check passed
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/users")
						.header("Authorization", authHeaderFor("examiner")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Your account must be assigned to a research group."));
	}
}
