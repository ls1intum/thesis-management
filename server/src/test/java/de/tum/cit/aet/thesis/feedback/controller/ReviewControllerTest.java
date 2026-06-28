package de.tum.cit.aet.thesis.feedback.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.feedback.dto.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.dto.FindingDTO;
import de.tum.cit.aet.thesis.feedback.dto.Location;
import de.tum.cit.aet.thesis.feedback.dto.ReviewRequestDTO;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.service.ReviewService;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Testcontainers
@TestPropertySource(properties = "thesis-management.ai.enabled=true")
class ReviewControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@MockitoBean
	private ReviewService reviewService;

	private byte[] proposalTemplateContent;

	@BeforeEach
	void loadProposalTemplate() throws IOException {
		proposalTemplateContent = Files.readAllBytes(Path.of("src/test/resources/pdfs/proposal-template.pdf"));
	}

	@Test
	void reviewProposal_returnsMockedReviewResultForAuthenticatedRequest() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "proposal.pdf", "application/pdf", proposalTemplateContent);

		ReviewResultDTO reviewResult = new ReviewResultDTO(
				AssessmentCategory.ACCEPTABLE,
				"Overall assessment",
				List.of(new FindingDTO(
						"LOW",
						"writing-style",
						"Clear objective",
						"The objective is clearly stated.",
						List.of(new Location(1, "Introduction", "Objective is clearly stated.")))));

		when(reviewService.review(any(ReviewRequestDTO.class))).thenReturn(reviewResult);

		mockMvc.perform(multipart("/v2/ai-review/review-proposal")
						.file(file)
						.param("providerCategory", "LOCAL")
						.header("Authorization", createRandomAuthentication("supervisor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category").value("acceptable"))
				.andExpect(jsonPath("$.summary").value("Overall assessment"))
				.andExpect(jsonPath("$.findings[0].title").value("Clear objective"));

		ArgumentCaptor<ReviewRequestDTO> requestCaptor = ArgumentCaptor.forClass(ReviewRequestDTO.class);
		verify(reviewService).review(requestCaptor.capture());

		ReviewRequestDTO capturedRequest = requestCaptor.getValue();
		assertEquals("LOCAL", capturedRequest.providerCategory().name());
		assertEquals("proposal.pdf", capturedRequest.file().getOriginalFilename());
		assertArrayEquals(proposalTemplateContent, capturedRequest.file().getBytes());
	}

	@Test
	void reviewProposal_returnsUnauthorizedWithoutAuthentication() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "proposal.pdf", "application/pdf", proposalTemplateContent);

		mockMvc.perform(multipart("/v2/ai-review/review-proposal")
						.file(file)
						.param("providerCategory", "LOCAL"))
				.andExpect(status().isUnauthorized());
	}
}
