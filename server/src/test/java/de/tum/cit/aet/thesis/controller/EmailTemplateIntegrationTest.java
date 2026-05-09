package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
class EmailTemplateIntegrationTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private EmailTemplateRepository emailTemplateRepository;

	private UUID createTemplate(String templateCase) throws Exception {
		String payload = objectMapper.writeValueAsString(Map.of(
				"templateCase", templateCase,
				"description", "Test desc for " + templateCase,
				"subject", "Subject for " + templateCase,
				"bodyHtml", "<p>Body for " + templateCase + "</p>",
				"language", "en"
		));

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/email-templates")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return UUID.fromString(objectMapper.readTree(response).get("id").asString());
	}

	private record RoleUserContext(String authHeader, UUID researchGroupId) {}

	private RoleUserContext createUserWithResearchGroup(String role) throws Exception {
		TestUser user = createRandomTestUser(List.of(role));
		UUID researchGroupId = createTestResearchGroup("Email Test Group", user.universityId());
		String authHeader = generateTestAuthenticationHeader(user.universityId(), List.of(role));
		return new RoleUserContext(authHeader, researchGroupId);
	}

	private UUID createTemplateForGroup(String templateCase, UUID researchGroupId) throws Exception {
		Map<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("templateCase", templateCase);
		payloadMap.put("description", "Test desc for " + templateCase);
		payloadMap.put("subject", "Subject for " + templateCase);
		payloadMap.put("bodyHtml", "<p>Body for " + templateCase + "</p>");
		payloadMap.put("language", "en");
		payloadMap.put("researchGroupId", researchGroupId.toString());

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/email-templates")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payloadMap)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return UUID.fromString(objectMapper.readTree(response).get("id").asString());
	}

	@Nested
	class GetEmailTemplates {
		@Test
		void getEmailTemplates_ReturnsCreatedTemplates() throws Exception {
			createTemplate("APPLICATION_CREATED_CHAIR");
			createTemplate("THESIS_CREATED");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
		}

		@Test
		void getEmailTemplates_WithTemplateCaseFilter() throws Exception {
			createTemplate("APPLICATION_CREATED_CHAIR");
			createTemplate("THESIS_CREATED");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", createRandomAdminAuthentication())
							.param("templateCases", "APPLICATION_CREATED_CHAIR"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(1)));
		}

		@Test
		void getEmailTemplates_WithLanguageFilter() throws Exception {
			createTemplate("APPLICATION_REJECTED");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", createRandomAdminAuthentication())
							.param("languages", "en"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

			String deResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", createRandomAdminAuthentication())
							.param("languages", "de"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			assertThat(objectMapper.readTree(deResponse).get("totalElements").asInt()).isZero();
		}

		@Test
		void getEmailTemplates_WithSearch() throws Exception {
			createTemplate("THESIS_FINAL_SUBMISSION");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", createRandomAdminAuthentication())
							.param("search", "THESIS_FINAL_SUBMISSION"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getEmailTemplates_AsStudent_Forbidden() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isForbidden());
		}

		@Test
		void getEmailTemplates_AsGroupAdmin_Success() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("group-admin");
			createTemplateForGroup("APPLICATION_CREATED_CHAIR", ctx.researchGroupId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", ctx.authHeader()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getEmailTemplates_AsSupervisor_Success() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("supervisor");
			createTemplateForGroup("THESIS_CREATED", ctx.researchGroupId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", ctx.authHeader()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getEmailTemplates_AsAdvisor_Success() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("advisor");
			createTemplateForGroup("THESIS_CREATED", ctx.researchGroupId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates")
							.header("Authorization", ctx.authHeader()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
		}
	}

	@Nested
	class GroupAdminAccess {
		@Test
		void createEmailTemplate_AsGroupAdmin_Success() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("group-admin");

			Map<String, Object> payloadMap = new HashMap<>();
			payloadMap.put("templateCase", "APPLICATION_CREATED_CHAIR");
			payloadMap.put("description", "Group admin test");
			payloadMap.put("subject", "GA Subject");
			payloadMap.put("bodyHtml", "<p>GA Body</p>");
			payloadMap.put("language", "en");
			payloadMap.put("researchGroupId", ctx.researchGroupId().toString());

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/email-templates")
							.header("Authorization", ctx.authHeader())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payloadMap)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("templateCase").asString()).isEqualTo("APPLICATION_CREATED_CHAIR");
			assertThat(json.get("subject").asString()).isEqualTo("GA Subject");
			assertThat(json.get("description").asString()).isEqualTo("Group admin test");
		}

		@Test
		void updateEmailTemplate_AsGroupAdmin_Success() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("group-admin");
			UUID templateId = createTemplateForGroup("THESIS_CLOSED", ctx.researchGroupId());

			Map<String, Object> updatePayloadMap = new HashMap<>();
			updatePayloadMap.put("templateCase", "THESIS_CLOSED");
			updatePayloadMap.put("description", "Updated by group admin");
			updatePayloadMap.put("subject", "Updated Subject GA");
			updatePayloadMap.put("bodyHtml", "<p>Updated GA</p>");
			updatePayloadMap.put("language", "en");
			updatePayloadMap.put("researchGroupId", ctx.researchGroupId().toString());

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/email-templates/{id}", templateId)
							.header("Authorization", ctx.authHeader())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(updatePayloadMap)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("subject").asString()).isEqualTo("Updated Subject GA");
			assertThat(json.get("description").asString()).isEqualTo("Updated by group admin");
		}

		@Test
		void deleteEmailTemplate_AsGroupAdmin_Success() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("group-admin");
			UUID templateId = createTemplateForGroup("THESIS_COMMENT_POSTED", ctx.researchGroupId());

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/email-templates/{id}", templateId)
							.header("Authorization", ctx.authHeader()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates/{id}", templateId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getEmailTemplateVariables_AsGroupAdmin_Success() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("group-admin");
			UUID templateId = createTemplateForGroup("APPLICATION_CREATED_CHAIR", ctx.researchGroupId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates/{id}/variables", templateId)
							.header("Authorization", ctx.authHeader()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", isA(List.class)));
		}

		@Test
		void createEmailTemplate_AsGroupAdmin_WithoutResearchGroup_Forbidden() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("group-admin");

			String payload = objectMapper.writeValueAsString(Map.of(
					"templateCase", "APPLICATION_CREATED_CHAIR",
					"description", "Should fail",
					"subject", "Subject",
					"bodyHtml", "<p>Body</p>",
					"language", "en"
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/email-templates")
							.header("Authorization", ctx.authHeader())
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isForbidden());
		}

		@Test
		void updateDefaultTemplate_AsGroupAdmin_Forbidden() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("group-admin");
			UUID defaultTemplateId = createTemplate("THESIS_CLOSED");

			Map<String, Object> updatePayloadMap = new HashMap<>();
			updatePayloadMap.put("templateCase", "THESIS_CLOSED");
			updatePayloadMap.put("description", "Should fail");
			updatePayloadMap.put("subject", "Subject");
			updatePayloadMap.put("bodyHtml", "<p>Body</p>");
			updatePayloadMap.put("language", "en");

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/email-templates/{id}", defaultTemplateId)
							.header("Authorization", ctx.authHeader())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(updatePayloadMap)))
					.andExpect(status().isForbidden());
		}

		@Test
		void deleteDefaultTemplate_AsGroupAdmin_Forbidden() throws Exception {
			RoleUserContext ctx = createUserWithResearchGroup("group-admin");
			UUID defaultTemplateId = createTemplate("THESIS_COMMENT_POSTED");

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/email-templates/{id}", defaultTemplateId)
							.header("Authorization", ctx.authHeader()))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class GetSingleTemplate {
		@Test
		void getEmailTemplate_Success() throws Exception {
			UUID templateId = createTemplate("APPLICATION_CREATED_STUDENT");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates/{id}", templateId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("id").asString()).isEqualTo(templateId.toString());
			assertThat(json.get("templateCase").asString()).isEqualTo("APPLICATION_CREATED_STUDENT");
		}

		@Test
		void getEmailTemplate_NotFound() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates/{id}", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	class UpdateTemplate {
		@Test
		void updateEmailTemplate_Success() throws Exception {
			UUID templateId = createTemplate("THESIS_CLOSED");

			String updatePayload = objectMapper.writeValueAsString(Map.of(
					"templateCase", "THESIS_CLOSED",
					"description", "Updated description",
					"subject", "Updated Subject",
					"bodyHtml", "<p>Updated body</p>",
					"language", "en"
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/email-templates/{id}", templateId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(updatePayload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("subject").asString()).isEqualTo("Updated Subject");
			assertThat(json.get("description").asString()).isEqualTo("Updated description");
		}
	}

	@Nested
	class DeleteTemplate {
		@Test
		void deleteEmailTemplate_Success() throws Exception {
			UUID templateId = createTemplate("THESIS_COMMENT_POSTED");

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/email-templates/{id}", templateId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates/{id}", templateId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	class GetTemplateVariables {
		@Test
		void getVariables_Success() throws Exception {
			UUID templateId = createTemplate("APPLICATION_CREATED_CHAIR");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/email-templates/{id}/variables", templateId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", isA(List.class)));
		}
	}
}
