package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.StructuredOutputConverter;
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

	@Mock
	CategoryReviewer failingCategoryReviewer;

	@Mock
	private ProgressReporter progressReporter;

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
		when(callResponseSpec.entity(anyReviewResultConverter())).thenReturn(expectedResult);

		ReviewResult actualResult = reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdfResource));

		assertSame(expectedResult, actualResult);
		verify(pdfService).extractTextFromPdf(pdfResource);
		verify(pdfService).extractImagesFromPdf(pdfResource);
		// Fan-out remains one call per ReviewCategory even after parallelization — the executor
		// waits for all futures before invoking the merger step.
		verify(categoryReviewer, times(ReviewCategory.values().length)).review(extractedText, extractedImages);
		verify(chatClient).prompt();
		verify(callResponseSpec).entity(anyReviewResultConverter());
	}

	@Test
	void reviewReportsOneStepPerCategoryPlusTheMergeToTheProgressReporter() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());
		int total = ReviewCategory.values().length + 1;

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Extracted text"));
		when(pdfService.extractImagesFromPdf(any(Resource.class))).thenReturn(List.of());
		when(categoryReviewer.review(anyList(), anyList())).thenReturn(new CategoryFindings(List.of()));
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.entity(anyReviewResultConverter()))
				.thenReturn(new ReviewResult(AssessmentCategory.GOOD, 90, "Fine.", List.of()));

		reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdfResource, progressReporter));

		// Categories are dispatched in ReviewCategory declaration order on the calling thread, one
		// call per category.
		org.mockito.InOrder dispatchOrder = org.mockito.Mockito.inOrder(progressReporter);
		int index = 0;
		for (ReviewCategory category : ReviewCategory.values()) {
			index++;
			dispatchOrder.verify(progressReporter)
					.stepStarted(category.getSlug(), category.getDisplayName(), index, total);
		}
		// Completions land on virtual threads, so only that each category completed exactly once is
		// asserted, not the order.
		for (ReviewCategory category : ReviewCategory.values()) {
			verify(progressReporter).stepCompleted(eq(category.getSlug()), org.mockito.ArgumentMatchers.anyInt(), eq(total));
		}

		// The merge step is the last one, dispatched only once every category has completed.
		verify(progressReporter).stepStarted("merge", "Consolidating findings", total, total);
		verify(progressReporter).stepCompleted("merge", total, total);
		verify(progressReporter, org.mockito.Mockito.never()).stepFailed(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any());
	}

	@Test
	void aFailedCategoryIsDroppedAndTheOthersStillProduceAResult() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());
		ReviewCategory failingCategory = ReviewCategory.values()[0];
		int total = ReviewCategory.values().length + 1;
		ReviewResult expectedResult = new ReviewResult(AssessmentCategory.ACCEPTABLE, 60, "Partial review.", List.of());

		CategoryFanOutReviewer partiallyFailing = new CategoryFanOutReviewer(pdfService, chatClientBuilder, objectMapper, false, "") {
			@Override
			protected CategoryReviewer createReviewer(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt) {
				return category == failingCategory ? failingCategoryReviewer : categoryReviewer;
			}
		};

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Extracted text"));
		when(failingCategoryReviewer.review(anyList(), anyList())).thenThrow(new RuntimeException("502 Bad Gateway"));
		when(categoryReviewer.review(anyList(), anyList())).thenReturn(new CategoryFindings(List.of()));
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.entity(anyReviewResultConverter())).thenReturn(expectedResult);

		ReviewResult actualResult = partiallyFailing.review(
				new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdfResource, progressReporter));

		// One flaky LLM call costs its own category, not the whole review: every other category
		// still ran and the merge step still consolidated what came back.
		assertThat(actualResult.findings()).isEqualTo(expectedResult.findings());
		verify(categoryReviewer, times(ReviewCategory.values().length - 1)).review(anyList(), anyList());
		verify(progressReporter).stepFailed(eq(failingCategory.getSlug()), anyInt(), eq(total), eq("This step could not be completed"));
		verify(progressReporter, org.mockito.Mockito.never()).stepCompleted(eq(failingCategory.getSlug()), anyInt(), anyInt());
		verify(progressReporter).stepCompleted("merge", total, total);
	}

	@Test
	void anIncompleteReviewGivesUpItsScoreAndAssessmentAndSaysWhichChecksAreMissing() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());
		ReviewCategory failingCategory = ReviewCategory.values()[0];
		Finding finding = new Finding("MINOR", "WRITING", "Run-on sentence", "Split it in two.", List.of());

		CategoryFanOutReviewer partiallyFailing = new CategoryFanOutReviewer(pdfService, chatClientBuilder, objectMapper, false, "") {
			@Override
			protected CategoryReviewer createReviewer(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt) {
				return category == failingCategory ? failingCategoryReviewer : categoryReviewer;
			}
		};

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Extracted text"));
		when(failingCategoryReviewer.review(anyList(), anyList())).thenThrow(new RuntimeException("502 Bad Gateway"));
		when(categoryReviewer.review(anyList(), anyList())).thenReturn(new CategoryFindings(List.of()));
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
		// The merger only ever saw the categories that answered, yet reads like a verdict on all of
		// them — here, a clean bill of health that would otherwise be shown and persisted as one.
		when(callResponseSpec.entity(anyReviewResultConverter()))
				.thenReturn(new ReviewResult(AssessmentCategory.GOOD, 92, "A solid proposal.", List.of(finding)));

		ReviewResult actualResult = partiallyFailing.review(
				new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdfResource, progressReporter));

		// The findings are real and survive; the two claims the run cannot support do not.
		assertThat(actualResult.findings()).containsExactly(finding);
		assertThat(actualResult.assessment()).isNull();
		assertThat(actualResult.score()).isNull();
		assertThat(actualResult.normalizedScore()).isNull();
		// Both the supervisor's preview and the persisted summary row read this string, so the
		// caveat outlives the progress list that reported the failure while the review ran.
		assertThat(actualResult.summary())
				.startsWith("Incomplete review: 1 of " + ReviewCategory.values().length + " checks could not be completed ("
						+ failingCategory.getDisplayName() + ").")
				.contains("no overall score or assessment is given")
				.endsWith("A solid proposal.");
	}

	@Test
	void aCompleteReviewKeepsTheMergerScoreAssessmentAndSummaryUntouched() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());
		ReviewResult expectedResult = new ReviewResult(AssessmentCategory.GOOD, 92, "A solid proposal.", List.of());

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Extracted text"));
		when(pdfService.extractImagesFromPdf(any(Resource.class))).thenReturn(List.of());
		when(categoryReviewer.review(anyList(), anyList())).thenReturn(new CategoryFindings(List.of()));
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.entity(anyReviewResultConverter())).thenReturn(expectedResult);

		ReviewResult actualResult = reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdfResource));

		// Nothing is qualified when every category answered — the merger's verdict stands as-is.
		assertSame(expectedResult, actualResult);
	}

	@Test
	void aReviewWhoseEveryCategoryFailedHasNothingToConsolidateAndFails() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Extracted text"));
		when(pdfService.extractImagesFromPdf(any(Resource.class))).thenReturn(List.of());
		when(categoryReviewer.review(anyList(), anyList())).thenThrow(new RuntimeException("502 Bad Gateway"));

		ReviewRequest request = new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdfResource, progressReporter);
		assertThatThrownBy(() -> reviewer.review(request)).isInstanceOf(IllegalStateException.class);

		// No merge call is attempted when there is nothing to merge.
		verify(chatClient, org.mockito.Mockito.never()).prompt();
	}

	@Test
	void aFailedCategoryReportsAFixedMessageRatherThanTheProviderException() {
		Resource pdfResource = new ByteArrayResource("pdf-content".getBytes());
		int total = ReviewCategory.values().length + 1;

		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Extracted text"));
		when(pdfService.extractImagesFromPdf(any(Resource.class))).thenReturn(List.of());
		// Whatever the provider says — endpoints, models, quota details — stays server-side.
		when(categoryReviewer.review(anyList(), anyList()))
				.thenThrow(new RuntimeException("401 Unauthorized calling https://internal-llm.example/v1/chat/completions"));

		ReviewRequest request = new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdfResource, progressReporter);
		assertThatThrownBy(() -> reviewer.review(request)).isInstanceOf(IllegalStateException.class);

		ArgumentCaptor<String> reported = ArgumentCaptor.forClass(String.class);
		verify(progressReporter, atLeastOnce()).stepFailed(any(), anyInt(), eq(total), reported.capture());
		assertThat(reported.getAllValues())
				.isNotEmpty()
				.allSatisfy(message -> assertThat(message).isEqualTo("This step could not be completed"));
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
		when(callResponseSpec.entity(anyReviewResultConverter()))
				.thenReturn(new ReviewResult(AssessmentCategory.GOOD, 90, "Fine.", List.of()));

		textOnly.review(new ReviewRequest(ReviewType.THESIS, GUIDELINES, pdfResource));

		verify(pdfService, times(0)).extractImagesFromPdf(any(Resource.class));
		verify(categoryReviewer, times(ReviewCategory.values().length)).review(List.of("Page one."), List.of());
	}

	/**
	 * Matches the structured-output converter the reviewer hands to {@code entity(...)}: a lenient
	 * one built for {@link ReviewResult} rather than the plain class literal.
	 */
	private static StructuredOutputConverter<ReviewResult> anyReviewResultConverter() {
		return any();
	}
}
