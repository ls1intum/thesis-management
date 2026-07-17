package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.dto.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.dto.FindingDTO;
import de.tum.cit.aet.thesis.feedback.dto.IntermediateReviewResult;
import de.tum.cit.aet.thesis.feedback.dto.Location;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.service.reviewer.LlmReviewer;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:LineLength")
public class ReviewServiceTest {

	@Mock
	private PdfService pdfService;

	@Mock
	private ChatClient.Builder chatClientBuilder;

	@Mock
	private ChatClient chatClient;

	@Mock
	ChatClient.ChatClientRequestSpec chatClientRequestSpec;

	@Mock
	ChatClient.CallResponseSpec callResponseSpec;

	@Mock
	LlmReviewer llmReviewer;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ReviewService reviewService;

	@BeforeEach
	void setUp() {
		when(chatClientBuilder.build()).thenReturn(chatClient);
		reviewService = new ReviewService(pdfService, chatClientBuilder, objectMapper, true, "logos/openai/gpt-oss-120b") {
			@Override
			protected LlmReviewer createReviewer(String taskPrompt, ReviewType reviewType) {
				return llmReviewer;
			}
		};
	}

	@Test
	void testReview() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());
		List<String> extractedText = List.of("Extracted text from PDF");
		List<Media> extractedImages = List.of(new Media(MimeTypeUtils.IMAGE_PNG, URI.create("file:///proposal-template-page-1.png")));
		IntermediateReviewResult intermediateReviewResult = new IntermediateReviewResult(List.of());
		ReviewResultDTO expectedResult = new ReviewResultDTO(AssessmentCategory.ACCEPTABLE, "Overall assessment", List.of());

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(extractedText);
		when(pdfService.extractImagesFromPdf(any(Resource.class))).thenReturn(extractedImages);
		when(llmReviewer.review(anyList(), anyList())).thenReturn(intermediateReviewResult);
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.entity(ReviewResultDTO.class)).thenReturn(expectedResult);

		ReviewResultDTO actualResult = reviewService.review(pdfResource, ReviewType.PROPOSAL);

		assertSame(expectedResult, actualResult);
		verify(pdfService).extractTextFromPdf(pdfResource);
		verify(pdfService).extractImagesFromPdf(pdfResource);
		// Fan-out remains one call per ReviewCategory even after parallelization — the executor
		// waits for all futures before invoking the merger step.
		verify(llmReviewer, times(9)).review(extractedText, extractedImages);
		verify(chatClient).prompt();
		verify(callResponseSpec).entity(ReviewResultDTO.class);
	}

	@Test
	void testBuildMergePrompt() {
		Map<String, IntermediateReviewResult> reviewResults = Map.of(
				"structure", new IntermediateReviewResult(List.of(new FindingDTO("HIGH", "structure", "Poor structure", "The paper has a poor structure.", List.of(new Location(1, "Introduction", "The introduction is not well structured."))))),
				"writing-style", new IntermediateReviewResult(List.of(new FindingDTO("LOW", "writing-style", "Clear writing style", "The writing style is clear.", List.of(new Location(2, "Methodology", "The methodology section is well written.")))))
		);

		String mergePrompt = reviewService.buildMergePrompt(reviewResults);

		// The merger receives intermediate findings as JSON inside a fenced tag so the LLM
		// can treat every field as untrusted data. Assert the fence is present and that each
		// finding's field values survive serialization (Map iteration order is unspecified).
		assertThat(mergePrompt).startsWith("<intermediate-findings>\n");
		assertThat(mergePrompt).endsWith("\n</intermediate-findings>\n");
		assertThat(mergePrompt).contains("\"title\":\"Poor structure\"");
		assertThat(mergePrompt).contains("\"description\":\"The paper has a poor structure.\"");
		assertThat(mergePrompt).contains("\"quote\":\"The introduction is not well structured.\"");
		assertThat(mergePrompt).contains("\"title\":\"Clear writing style\"");
		assertThat(mergePrompt).contains("\"description\":\"The writing style is clear.\"");
		assertThat(mergePrompt).contains("\"quote\":\"The methodology section is well written.\"");
	}
}
