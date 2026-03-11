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
					"emailSettings", Map.of("applicationNotificationEmail", "notify@test.com"),
					"applicationEmailSettings", Map.of("includeApplicationDataInEmail", true)
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
			assertThat(json.get("applicationEmailSettings").get("includeApplicationDataInEmail").asBoolean()).isTrue();
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
	class ApplicationEmailSettings {
		@Test
		void getSettings_DefaultIncludeApplicationDataInEmail_IsFalse() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("App Email Default Group", head.universityId());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.has("applicationEmailSettings")).isTrue();
			assertThat(json.get("applicationEmailSettings").get("includeApplicationDataInEmail").asBoolean()).isFalse();
		}

		@Test
		void createSettings_WithApplicationEmailSettings_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("App Email Create Group", head.universityId());

			String payload = objectMapper.writeValueAsString(Map.of(
					"applicationEmailSettings", Map.of("includeApplicationDataInEmail", true)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("applicationEmailSettings").get("includeApplicationDataInEmail").asBoolean()).isTrue();
		}

		@Test
		void updateSettings_ToggleApplicationEmailSettings() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("App Email Toggle Group", head.universityId());

			// Enable
			String enablePayload = objectMapper.writeValueAsString(Map.of(
					"applicationEmailSettings", Map.of("includeApplicationDataInEmail", true)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(enablePayload))
					.andExpect(status().isOk());

			// Disable
			String disablePayload = objectMapper.writeValueAsString(Map.of(
					"applicationEmailSettings", Map.of("includeApplicationDataInEmail", false)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(disablePayload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("applicationEmailSettings").get("includeApplicationDataInEmail").asBoolean()).isFalse();
		}

		@Test
		void updateSettings_ApplicationEmailDoesNotAffectOtherSettings() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("App Email Isolated Group", head.universityId());

			// First set reject settings
			String rejectPayload = objectMapper.writeValueAsString(Map.of(
					"rejectSettings", Map.of("automaticRejectEnabled", true, "rejectDuration", 14)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(rejectPayload))
					.andExpect(status().isOk());

			// Then update only application email settings
			String emailPayload = objectMapper.writeValueAsString(Map.of(
					"applicationEmailSettings", Map.of("includeApplicationDataInEmail", true)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(emailPayload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			// Application email settings should be updated
			assertThat(json.get("applicationEmailSettings").get("includeApplicationDataInEmail").asBoolean()).isTrue();
			// Reject settings should be preserved
			assertThat(json.get("rejectSettings").get("automaticRejectEnabled").asBoolean()).isTrue();
			assertThat(json.get("rejectSettings").get("rejectDuration").asInt()).isEqualTo(14);
		}
	}

	@Nested
	class GradingSchemeSettings {
		@Test
		void getSettings_DefaultGradingSchemeIsEmpty() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Default Group", head.universityId());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			// With @JsonInclude(NON_EMPTY), empty components list is omitted
			assertThat(!json.has("gradingSchemeSettings") || !json.get("gradingSchemeSettings").has("components")
					|| json.get("gradingSchemeSettings").get("components").size() == 0).isTrue();
		}

		@Test
		void createGradingScheme_WithValidComponents_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Create Group", head.universityId());

			String payload = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "Thesis Content", "weight", 40, "isBonus", false),
									Map.of("name", "Methodology", "weight", 30, "isBonus", false),
									Map.of("name", "Presentation", "weight", 30, "isBonus", false)
							)
					)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			JsonNode components = json.get("gradingSchemeSettings").get("components");
			assertThat(components.size()).isEqualTo(3);
			assertThat(components.get(0).get("name").asString()).isEqualTo("Thesis Content");
			assertThat(components.get(0).get("weight").asDouble()).isEqualTo(40.0);
			assertThat(components.get(0).get("isBonus").asBoolean()).isFalse();
			assertThat(components.get(0).get("position").asInt()).isEqualTo(0);
			assertThat(components.get(1).get("name").asString()).isEqualTo("Methodology");
			assertThat(components.get(1).get("weight").asDouble()).isEqualTo(30.0);
			assertThat(components.get(1).get("position").asInt()).isEqualTo(1);
			assertThat(components.get(2).get("name").asString()).isEqualTo("Presentation");
			assertThat(components.get(2).get("weight").asDouble()).isEqualTo(30.0);
			assertThat(components.get(2).get("position").asInt()).isEqualTo(2);
		}

		@Test
		void createGradingScheme_WithBonusComponent_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Bonus Group", head.universityId());

			String payload = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "Content", "weight", 50, "isBonus", false),
									Map.of("name", "Methods", "weight", 50, "isBonus", false),
									Map.of("name", "Extra Credit", "weight", 0, "isBonus", true)
							)
					)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			JsonNode components = json.get("gradingSchemeSettings").get("components");
			assertThat(components.size()).isEqualTo(3);
			assertThat(components.get(2).get("name").asString()).isEqualTo("Extra Credit");
			assertThat(components.get(2).get("isBonus").asBoolean()).isTrue();
		}

		@Test
		void createGradingScheme_WeightsNotSumTo100_ReturnsBadRequest() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Invalid Weight Group", head.universityId());

			String payload = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "Content", "weight", 40, "isBonus", false),
									Map.of("name", "Methods", "weight", 30, "isBonus", false)
							)
					)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isBadRequest());
		}

		@Test
		void createGradingScheme_EmptyComponentName_ReturnsBadRequest() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Empty Name Group", head.universityId());

			String payload = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "", "weight", 100, "isBonus", false)
							)
					)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isBadRequest());
		}

		@Test
		void updateGradingScheme_ReplacesExistingComponents() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Replace Group", head.universityId());

			// Create initial scheme
			String initial = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "Old Component", "weight", 100, "isBonus", false)
							)
					)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(initial))
					.andExpect(status().isOk());

			// Replace with new scheme
			String updated = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "New A", "weight", 60, "isBonus", false),
									Map.of("name", "New B", "weight", 40, "isBonus", false)
							)
					)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(updated))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			JsonNode components = json.get("gradingSchemeSettings").get("components");
			assertThat(components.size()).isEqualTo(2);
			assertThat(components.get(0).get("name").asString()).isEqualTo("New A");
			assertThat(components.get(1).get("name").asString()).isEqualTo("New B");
		}

		@Test
		void updateGradingScheme_DoesNotAffectOtherSettings() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Isolated Group", head.universityId());

			// Set reject settings first
			String rejectPayload = objectMapper.writeValueAsString(Map.of(
					"rejectSettings", Map.of("automaticRejectEnabled", true, "rejectDuration", 14)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(rejectPayload))
					.andExpect(status().isOk());

			// Set grading scheme
			String gradingPayload = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "Content", "weight", 100, "isBonus", false)
							)
					)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(gradingPayload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("gradingSchemeSettings").get("components").size()).isEqualTo(1);
			assertThat(json.get("rejectSettings").get("automaticRejectEnabled").asBoolean()).isTrue();
			assertThat(json.get("rejectSettings").get("rejectDuration").asInt()).isEqualTo(14);
		}

		@Test
		void getGradingScheme_AsSupervisor_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Supervisor Access Group", head.universityId());

			// Create scheme as admin
			String payload = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "Content", "weight", 60, "isBonus", false),
									Map.of("name", "Methods", "weight", 40, "isBonus", false)
							)
					)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isOk());

			// Fetch as supervisor via grading-scheme endpoint
			String supervisorAuth = generateTestAuthenticationHeader(head.universityId(), List.of("supervisor"));
			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}/grading-scheme", groupId)
							.header("Authorization", supervisorAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("components").size()).isEqualTo(2);
			assertThat(json.get("components").get(0).get("name").asString()).isEqualTo("Content");
			assertThat(json.get("components").get(1).get("name").asString()).isEqualTo("Methods");
		}

		@Test
		void getGradingScheme_AsStudent_Forbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Student Forbidden Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-group-settings/{id}/grading-scheme", groupId)
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isForbidden());
		}

		@Test
		void clearGradingScheme_EmptyComponentsList() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Grading Clear Group", head.universityId());

			// Create scheme
			String createPayload = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of(
									Map.of("name", "Content", "weight", 100, "isBonus", false)
							)
					)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(createPayload))
					.andExpect(status().isOk());

			// Clear scheme with empty list
			String clearPayload = objectMapper.writeValueAsString(Map.of(
					"gradingSchemeSettings", Map.of(
							"components", List.of()
					)
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(clearPayload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			// With @JsonInclude(NON_EMPTY), empty components list is omitted
			assertThat(!json.has("gradingSchemeSettings") || !json.get("gradingSchemeSettings").has("components")
					|| json.get("gradingSchemeSettings").get("components").size() == 0).isTrue();
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
