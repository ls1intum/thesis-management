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
