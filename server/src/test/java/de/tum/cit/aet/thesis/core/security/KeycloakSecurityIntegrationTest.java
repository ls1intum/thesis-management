package de.tum.cit.aet.thesis.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.dockerjava.api.DockerClient;
import de.tum.cit.aet.thesis.mock.BaseKeycloakIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.DockerClientFactory;

import java.time.Duration;

/**
 * Security tests against a real Keycloak: its access token is accepted only at the session exchange, every
 * other request is authenticated by the session cookie, and an existing session does not depend on Keycloak.
 */
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
	void testSessionExchangeRejectsInvalidToken() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/session")
						.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testKeycloakTokenIsOnlyAcceptedAtTheSessionExchange() throws Exception {
		createLocalUserWithGroups("student", "student");

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeaderFor("student")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testAdminUserInfo() throws Exception {
		createLocalUserWithGroups("admin", "admin");
		KeycloakSession session = sessionFor("admin");

		mockMvc.perform(withSession(session, MockMvcRequestBuilders.get("/v2/user-info")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.universityId").value("admin"));
	}

	@Test
	void testStudentUserInfo() throws Exception {
		createLocalUserWithGroups("student", "student");
		KeycloakSession session = sessionFor("student");

		mockMvc.perform(withSession(session, MockMvcRequestBuilders.get("/v2/user-info")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.universityId").value("student"))
				.andExpect(jsonPath("$.groups", hasItem("student")));
	}

	@Test
	void testAdminCanAccessUsersEndpoint() throws Exception {
		createLocalUserWithGroups("admin", "admin");
		KeycloakSession session = sessionFor("admin");

		mockMvc.perform(withSession(session, MockMvcRequestBuilders.get("/v2/users")))
				.andExpect(status().isOk());
	}

	@Test
	void testStudentCannotAccessUsersEndpoint() throws Exception {
		createLocalUserWithGroups("student", "student");
		KeycloakSession session = sessionFor("student");

		mockMvc.perform(withSession(session, MockMvcRequestBuilders.get("/v2/users")))
				.andExpect(status().isForbidden());
	}

	@Test
	void testExaminerPassesSecurityButNeedsResearchGroup() throws Exception {
		createLocalUserWithGroups("examiner", "supervisor", "advisor");
		KeycloakSession session = sessionFor("examiner");

		// Examiner passes @PreAuthorize but gets application-level 403
		// because no research group is assigned — this proves the DB role check passed
		mockMvc.perform(withSession(session, MockMvcRequestBuilders.get("/v2/users")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Your account must be assigned to a research group."));
	}

	@Test
	void testSessionKeepsWorkingWhileKeycloakIsUnreachable() throws Exception {
		createLocalUserWithGroups("admin", "admin");
		KeycloakSession session = sessionFor("admin");
		DockerClient docker = DockerClientFactory.instance().client();
		String keycloakId = KEYCLOAK_CONTAINER.getContainerId();

		// Freeze Keycloak: connections to it hang instead of being answered, like during an outage.
		docker.pauseContainerCmd(keycloakId).exec();
		try {
			long start = System.nanoTime();

			mockMvc.perform(withSession(session, MockMvcRequestBuilders.get("/v2/user-info")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.universityId").value("admin"));
			mockMvc.perform(withSession(session, MockMvcRequestBuilders.get("/v2/users")))
					.andExpect(status().isOk());
			mockMvc.perform(withSession(session, MockMvcRequestBuilders.get("/v2/auth/session")))
					.andExpect(status().isOk());

			// no request waited for Keycloak
			assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(10));
		} finally {
			docker.unpauseContainerCmd(keycloakId).exec();
		}
	}
}
