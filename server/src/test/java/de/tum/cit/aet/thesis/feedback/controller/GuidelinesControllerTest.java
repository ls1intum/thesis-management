package de.tum.cit.aet.thesis.feedback.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.feedback.entity.GuidelinesStatus;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.service.GuidelinesService;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Testcontainers
@TestPropertySource(properties = "thesis-management.ai.enabled=true")
class GuidelinesControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@MockitoBean
	private GuidelinesService guidelinesService;

	private static ResearchGroupGuidelines readyGuidelines(UUID researchGroupId) {
		ResearchGroupGuidelines entity = new ResearchGroupGuidelines();
		entity.setResearchGroupId(researchGroupId);
		entity.setRawGuidelines("Cite at least 6 peer-reviewed sources.");
		entity.setStructuredGuidelines(new StructuredGuidelines(
				"Concise, well-cited proposals.",
				List.of(new CategoryGuidelines("bibliography", List.of("Cite at least 6 peer-reviewed sources.")))));
		entity.setStatus(GuidelinesStatus.READY);
		entity.setProcessedAt(Instant.now());
		return entity;
	}

	@Test
	void getGuidelines_returnsReadyGuidelinesForGroupAdmin() throws Exception {
		UUID researchGroupId = UUID.randomUUID();
		when(guidelinesService.getByResearchGroupId(eq(researchGroupId)))
				.thenReturn(Optional.of(readyGuidelines(researchGroupId)));

		mockMvc.perform(get("/v2/ai-review/guidelines/" + researchGroupId)
						.header("Authorization", createRandomAuthentication("group-admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ready"))
				.andExpect(jsonPath("$.categories[?(@.category=='bibliography')].rules[0]")
						.value("Cite at least 6 peer-reviewed sources."));
	}

	@Test
	void getGuidelines_returnsEmptyObjectWhenUnset() throws Exception {
		UUID researchGroupId = UUID.randomUUID();
		when(guidelinesService.getByResearchGroupId(eq(researchGroupId))).thenReturn(Optional.empty());

		mockMvc.perform(get("/v2/ai-review/guidelines/" + researchGroupId)
						.header("Authorization", createRandomAuthentication("group-admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").doesNotExist());
	}

	@Test
	void updateGuidelines_returnsProcessedGuidelinesForGroupAdmin() throws Exception {
		UUID researchGroupId = UUID.randomUUID();
		when(guidelinesService.updateGuidelines(eq(researchGroupId), any(String.class)))
				.thenReturn(readyGuidelines(researchGroupId));

		String body = "{\"rawGuidelines\":\"Cite at least 6 peer-reviewed sources.\"}";
		mockMvc.perform(put("/v2/ai-review/guidelines/" + researchGroupId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("group-admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ready"));
	}

	@Test
	void updateGuidelines_rejectsBlankInput() throws Exception {
		UUID researchGroupId = UUID.randomUUID();

		String body = "{\"rawGuidelines\":\"   \"}";
		mockMvc.perform(put("/v2/ai-review/guidelines/" + researchGroupId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("group-admin")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateGuidelines_returnsForbiddenForStudent() throws Exception {
		UUID researchGroupId = UUID.randomUUID();

		String body = "{\"rawGuidelines\":\"Cite at least 6 peer-reviewed sources.\"}";
		mockMvc.perform(put("/v2/ai-review/guidelines/" + researchGroupId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("student")))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateStructuredGuidelines_returnsRefinedGuidelinesForGroupAdmin() throws Exception {
		UUID researchGroupId = UUID.randomUUID();
		when(guidelinesService.updateStructuredGuidelines(eq(researchGroupId), any(), any()))
				.thenReturn(readyGuidelines(researchGroupId));

		String body = "{\"overview\":\"Concise, well-cited proposals.\","
				+ "\"categories\":[{\"category\":\"bibliography\",\"rules\":[\"Cite at least 6 peer-reviewed sources.\"]}]}";
		mockMvc.perform(put("/v2/ai-review/guidelines/" + researchGroupId + "/rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("group-admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ready"));
	}

	@Test
	void updateStructuredGuidelines_rejectsMissingCategories() throws Exception {
		UUID researchGroupId = UUID.randomUUID();

		String body = "{\"overview\":\"No categories field.\"}";
		mockMvc.perform(put("/v2/ai-review/guidelines/" + researchGroupId + "/rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("group-admin")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateStructuredGuidelines_returnsForbiddenForStudent() throws Exception {
		UUID researchGroupId = UUID.randomUUID();

		String body = "{\"overview\":\"x\",\"categories\":[]}";
		mockMvc.perform(put("/v2/ai-review/guidelines/" + researchGroupId + "/rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("student")))
				.andExpect(status().isForbidden());
	}

	@Test
	void getGuidelines_returnsUnauthorizedWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/v2/ai-review/guidelines/" + UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}
}
