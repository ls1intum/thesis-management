package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
class ResearchGroupSettingsControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Nested
	class GetSettings {
		@Test
		void getSettings_ReturnsDefaults_WhenNoSettingsExist() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Settings Default Group", head.universityId());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.has("rejectSettings")).isTrue();
			assertThat(json.has("phaseSettings")).isTrue();
		}

		@Test
		void getSettings_ReturnsSavedSettings() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Settings Saved Group", head.universityId());

			String createPayload = objectMapper.writeValueAsString(Map.of(
					"rejectSettings", Map.of("automaticRejectEnabled", true, "rejectDuration", 30),
					"presentationSettings", Map.of("presentationSlotDuration", 45),
					"phaseSettings", Map.of("proposalPhaseActive", false)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(createPayload))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("rejectSettings").get("automaticRejectEnabled").asBoolean()).isTrue();
			assertThat(json.get("rejectSettings").get("rejectDuration").asInt()).isEqualTo(30);
			assertThat(json.get("presentationSettings").get("presentationSlotDuration").asInt()).isEqualTo(45);
			assertThat(json.get("phaseSettings").get("proposalPhaseActive").asBoolean()).isFalse();
		}

		@Test
		void getSettings_AsStudent_Forbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Forbidden Settings", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class CreateOrUpdateSettings {
		@Test
		void createSettings_WithAllOptions_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Full Settings Group", head.universityId());

			String payload = objectMapper.writeValueAsString(Map.of(
					"rejectSettings", Map.of("automaticRejectEnabled", true, "rejectDuration", 14),
					"presentationSettings", Map.of("presentationSlotDuration", 60),
					"phaseSettings", Map.of("proposalPhaseActive", true),
					"emailSettings", Map.of("applicationNotificationEmail", "notify@test.com")
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("rejectSettings").get("automaticRejectEnabled").asBoolean()).isTrue();
			assertThat(json.get("rejectSettings").get("rejectDuration").asInt()).isEqualTo(14);
			assertThat(json.get("emailSettings").get("applicationNotificationEmail").asString()).isEqualTo("notify@test.com");
		}

		@Test
		void updateSettings_PartialUpdate_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Partial Update Group", head.universityId());

			String createPayload = objectMapper.writeValueAsString(Map.of(
					"rejectSettings", Map.of("automaticRejectEnabled", false, "rejectDuration", 7)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(createPayload))
					.andExpect(status().isOk());

			String updatePayload = objectMapper.writeValueAsString(Map.of(
					"presentationSettings", Map.of("presentationSlotDuration", 90)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(updatePayload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("presentationSettings").get("presentationSlotDuration").asInt()).isEqualTo(90);
		}

		@Test
		void createSettings_InvalidEmail_ReturnsBadRequest() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Invalid Email Group", head.universityId());

			String payload = objectMapper.writeValueAsString(Map.of(
					"emailSettings", Map.of("applicationNotificationEmail", "not-an-email")
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class GetPhaseSettings {
		@Test
		void getPhaseSettings_ReturnsDefaults() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Phase Settings Group", head.universityId());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}/phase-settings", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.has("proposalPhaseActive")).isTrue();
		}

		@Test
		void getPhaseSettings_ReturnsSavedPhaseSettings() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Saved Phase Group", head.universityId());

			String payload = objectMapper.writeValueAsString(Map.of(
					"phaseSettings", Map.of("proposalPhaseActive", false)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}/phase-settings", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("proposalPhaseActive").asBoolean()).isFalse();
		}
	}
}
