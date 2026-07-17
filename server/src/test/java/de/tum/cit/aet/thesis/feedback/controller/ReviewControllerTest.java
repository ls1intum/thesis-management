package de.tum.cit.aet.thesis.feedback.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.service.AIFeedbackService;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

@Testcontainers
@TestPropertySource(properties = "thesis-management.ai.enabled=true")
class ReviewControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@MockitoBean
	private AIFeedbackService aiFeedbackService;

	@Test
	void preview_returnsMockedDraftsForSupervisor() throws Exception {
		UUID thesisId = createTestThesis("AI review preview test");

		AIPreviewResponseDTO mockResponse = new AIPreviewResponseDTO(
				AssessmentCategory.ACCEPTABLE,
				"Solid overall but bibliography is thin.",
				List.of(new AIFeedbackDraftDTO(
						"**Thin bibliography** — increase to at least 6 peer-reviewed sources.",
						ThesisFeedbackCategory.CITATION,
						ThesisFeedbackSeverity.MAJOR)));
		when(aiFeedbackService.previewReview(any(Thesis.class), any(ReviewType.class))).thenReturn(mockResponse);

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAdminAuthentication()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assessment").value("ACCEPTABLE"))
				.andExpect(jsonPath("$.summary").value("Solid overall but bibliography is thin."))
				.andExpect(jsonPath("$.drafts[0].category").value("CITATION"))
				.andExpect(jsonPath("$.drafts[0].severity").value("MAJOR"));

		verify(aiFeedbackService).previewReview(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void preview_returnsForbiddenForStudent() throws Exception {
		UUID thesisId = createTestThesis("AI review preview forbidden test");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("student")))
				.andExpect(status().isForbidden());
	}

	@Test
	void auto_returnsForbiddenForRandomOutsider() throws Exception {
		UUID thesisId = createTestThesis("AI review auto outsider test");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("student")))
				.andExpect(status().isForbidden());
	}

	@Test
	void preview_returnsUnauthorizedWithoutAuthentication() throws Exception {
		String body = "{\"thesisId\":\"" + UUID.randomUUID() + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void auto_returnsUnauthorizedWithoutAuthentication() throws Exception {
		String body = "{\"thesisId\":\"" + UUID.randomUUID() + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
	}
}
