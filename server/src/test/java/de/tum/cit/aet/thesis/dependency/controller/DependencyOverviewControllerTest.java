package de.tum.cit.aet.thesis.dependency.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

@Testcontainers
class DependencyOverviewControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
		// Point both external integrations at the same WireMock created by BaseIntegrationTest.
		registry.add("thesis-management.dependency-scan.osv-url", () -> wireMockServer.baseUrl());
		registry.add("thesis-management.dependency-scan.github-api-url", () -> wireMockServer.baseUrl());
		registry.add("thesis-management.dependency-scan.github-repo", () -> "ls1intum/thesis-management");
		// Point SBOM loader at the small fixtures committed under src/test/resources/.
		registry.add("thesis-management.dependency-scan.server-sbom-path",
				() -> "sbom-fixtures/sample-server-sbom.json");
		registry.add("thesis-management.dependency-scan.client-sbom-path",
				() -> "sbom-fixtures/sample-client-sbom.json");
		registry.add("thesis-management.dependency-scan.admin-email", () -> "admin@example.com");
	}

	@BeforeEach
	void resetExtraStubs() {
		// Default stubs that most tests share. Re-register fresh on each test to avoid
		// state leakage across the suite (calendar stubs in BaseIntegrationTest stay intact
		// because they use a different path).
		wireMockServer.stubFor(post(urlPathEqualTo("/v1/querybatch"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"results\":[{}, {}, {}, {}, {}]}")));
		wireMockServer.stubFor(get(urlPathEqualTo("/repos/ls1intum/thesis-management/releases/latest"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"tag_name\":\"v4.11.0\"}")));
	}

	@Nested
	class GetCombinedSbom {

		@Test
		void returnsCombinedSbomForAdmin() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies")
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith("application/json"))
					.andExpect(jsonPath("$.server.bomFormat").value("CycloneDX"))
					.andExpect(jsonPath("$.server.components.length()").value(3))
					.andExpect(jsonPath("$.client.components.length()").value(2));
		}

		@Test
		void returnsServerOnlySbom() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies/server")
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.components.length()").value(3));
		}

		@Test
		void returnsClientOnlySbom() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies/client")
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.components.length()").value(2));
		}

		@Test
		void forbiddenForNonAdmin() throws Exception {
			String supervisorAuth = createRandomAuthentication("advisor");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies")
							.header("Authorization", supervisorAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void forbiddenForUnauthenticated() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class GetVulnerabilities {

		@Test
		void returnsEmptyCountsForFixtureSbom() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies/vulnerabilities")
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalVulnerabilities").value(0))
					.andExpect(jsonPath("$.lastChecked").isString());
		}

		@Test
		void refreshIsPostAndReturnsCounts() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/admin/dependencies/vulnerabilities/refresh")
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalVulnerabilities").value(0));
		}

		@Test
		void refreshRejectsGetMethod() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies/vulnerabilities/refresh")
							.header("Authorization", adminAuth))
					.andExpect(status().isMethodNotAllowed());
		}

		@Test
		void forbiddenForNonAdmin() throws Exception {
			String supervisorAuth = createRandomAuthentication("advisor");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies/vulnerabilities")
							.header("Authorization", supervisorAuth))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class GetVersion {

		@Test
		void returnsVersionInfo() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/admin/dependencies/version")
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(body.get("currentVersion").asString()).isNotBlank();
			assertThat(body.has("latestVersion")).isTrue();
		}
	}

	@Nested
	class SendVulnerabilityEmail {

		@Test
		void returnsSentTrueWhenAdminEmailConfigured() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/admin/dependencies/vulnerabilities/send-email")
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.sent").value(true));
		}

		@Test
		void forbiddenForNonAdmin() throws Exception {
			String supervisorAuth = createRandomAuthentication("advisor");

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/admin/dependencies/vulnerabilities/send-email")
							.header("Authorization", supervisorAuth))
					.andExpect(status().isForbidden());
		}
	}
}
