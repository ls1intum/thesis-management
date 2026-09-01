package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
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
public class CategoryFanOutReviewerTest {

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
	CategoryReviewer categoryReviewer;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private CategoryFanOutReviewer reviewer;

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Group overview.",
			List.of(new CategoryGuidelines("structure", List.of("Every proposal must contain an Abstract."))));

	@BeforeEach
	void setUp() {
		when(chatClientBuilder.build()).thenReturn(chatClient);
		reviewer = new CategoryFanOutReviewer(pdfService, chatClientBuilder, objectMapper, true, "logos/openai/gpt-oss-120b") {
			@Override
			protected CategoryReviewer createReviewer(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt) {
				return categoryReviewer;
			}
		};
	}

	@Test
	void reviewFansOutPerCategoryAndMergesTheResults() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());
		List<String> extractedText = List.of("Extracted text from PDF");
		List<Media> extractedImages = List.of(new Media(MimeTypeUtils.IMAGE_PNG, URI.create("file:///proposal-template-page-1.png")));
		ReviewResult expectedResult = new ReviewResult(AssessmentCategory.ACCEPTABLE, 65, "Overall assessment", List.of());

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(extractedText);
		when(pdfService.extractImagesFromPdf(any(Resource.class))).thenReturn(extractedImages);
		when(categoryReviewer.review(anyList(), anyList())).thenReturn(new CategoryFindings(List.of()));
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.entity(ReviewResult.class)).thenReturn(expectedResult);

		ReviewResult actualResult = reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdfResource));

		assertSame(expectedResult, actualResult);
		verify(pdfService).extractTextFromPdf(pdfResource);
		verify(pdfService).extractImagesFromPdf(pdfResource);
		// Fan-out remains one call per ReviewCategory even after parallelization — the executor
		// waits for all futures before invoking the merger step.
		verify(categoryReviewer, times(ReviewCategory.values().length)).review(extractedText, extractedImages);
		verify(chatClient).prompt();
		verify(callResponseSpec).entity(ReviewResult.class);
	}

	@Test
	void buildMergePromptFencesTheIntermediateFindings() {
		Map<String, CategoryFindings> perCategory = Map.of(
				"structure", new CategoryFindings(List.of(new Finding("HIGH", "structure", "Poor structure", "The paper has a poor structure.", List.of(new Location(1, "Introduction", "The introduction is not well structured."))))),
				"writing-style", new CategoryFindings(List.of(new Finding("LOW", "writing-style", "Clear writing style", "The writing style is clear.", List.of(new Location(2, "Methodology", "The methodology section is well written.")))))
		);

		String mergePrompt = reviewer.buildMergePrompt(perCategory);

		// The merger receives intermediate findings as JSON inside a fenced tag so the LLM can treat
		// every field as untrusted data. Assert the fence is present and that each finding's field
		// values survive serialization (Map iteration order is unspecified).
		assertThat(mergePrompt).startsWith("<intermediate-findings>\n");
		assertThat(mergePrompt).endsWith("\n</intermediate-findings>\n");
		assertThat(mergePrompt).contains("\"title\":\"Poor structure\"");
		assertThat(mergePrompt).contains("\"description\":\"The paper has a poor structure.\"");
		assertThat(mergePrompt).contains("\"quote\":\"The introduction is not well structured.\"");
		assertThat(mergePrompt).contains("\"title\":\"Clear writing style\"");
		assertThat(mergePrompt).contains("\"description\":\"The writing style is clear.\"");
		assertThat(mergePrompt).contains("\"quote\":\"The methodology section is well written.\"");
	}

	@Test
	void skipsImageExtractionWhenTheModelHasNoVision() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());
		CategoryFanOutReviewer textOnly = new CategoryFanOutReviewer(pdfService, chatClientBuilder, objectMapper, null, "openai/gpt-oss-120b") {
			@Override
			protected CategoryReviewer createReviewer(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt) {
				return categoryReviewer;
			}
		};

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Page one."));
		when(categoryReviewer.review(anyList(), anyList())).thenReturn(new CategoryFindings(List.of()));
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.entity(ReviewResult.class))
				.thenReturn(new ReviewResult(AssessmentCategory.GOOD, 90, "Fine.", List.of()));

		textOnly.review(new ReviewRequest(ReviewType.THESIS, GUIDELINES, pdfResource));

		verify(pdfService, times(0)).extractImagesFromPdf(any(Resource.class));
		verify(categoryReviewer, times(ReviewCategory.values().length)).review(List.of("Page one."), List.of());
	}
}
