package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.FollowUp;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The fan-out's comparison across iterations: the dedicated comparison call runs alongside the
 * categories, its failure is contained, and the merge learns which entries the student already has.
 */
// Lenient: the prompt-building test exercises no model call, so the shared call stubs go unused there.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryFanOutComparisonTest {

	@Mock
	PdfService pdfService;

	@Mock
	ChatClient.Builder chatClientBuilder;

	@Mock
	ChatClient chatClient;

	@Mock
	ChatClient.ChatClientRequestSpec requestSpec;

	@Mock
	ChatClient.CallResponseSpec callResponseSpec;

	@Mock
	CategoryReviewer categoryReviewer;

	@Mock
	EarlierFeedbackChecker checker;

	@Mock
	ProgressReporter progress;

	private CategoryFanOutReviewer reviewer;

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Group overview.",
			List.of(new CategoryGuidelines("structure", List.of("Every proposal must contain an Abstract."))));

	private static final EarlierFeedback COMPLETED = new EarlierFeedback("E1", UUID.randomUUID(),
			"Grammar in the abstract.", "WRITING", "MINOR", Instant.parse("2026-11-01T10:00:00Z"), 0, false);

	private static final EarlierFeedback OPEN = new EarlierFeedback("E2", UUID.randomUUID(),
			"Add a related work section.", "STRUCTURE", "MAJOR", null, 0, false);

	private final Resource pdf = new ByteArrayResource("pdf".getBytes());

	@BeforeEach
	void setUp() {
		when(chatClientBuilder.build()).thenReturn(chatClient);
		reviewer = new CategoryFanOutReviewer(pdfService, chatClientBuilder, new ObjectMapper(), metrics -> { },
				false, false, "logos/openai/gpt-oss-120b") {
			@Override
			protected CategoryReviewer createReviewer(ReviewCategory category, ReviewType reviewType,
					String guidelinesPrompt, ReviewMeter meter) {
				return categoryReviewer;
			}

			@Override
			protected EarlierFeedbackChecker createEarlierFeedbackChecker(ReviewMeter meter) {
				return checker;
			}
		};
		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Page text"));
		when(categoryReviewer.review(anyList(), anyList())).thenReturn(new CategoryFindings(List.of()));
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.advisors(any(Advisor[].class))).thenReturn(requestSpec);
		when(requestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any()))
				.thenReturn(requestSpec);
		when(requestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()))
				.thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.responseEntity(anyVerdictConverter())).thenReturn(new ResponseEntity<>(null,
				new ReviewVerdict(AssessmentCategory.ACCEPTABLE, 70, "Merged.", List.of())));
	}

	@Test
	void theComparisonRunsAlongsideTheCategoriesAndItsOutcomeReachesTheResult() {
		FollowUp followUp = new FollowUp("E1", new Finding("MINOR", "WRITING", "Still wrong", "Again.", List.of()));
		when(checker.check(eq(ReviewType.PROPOSAL), eq(List.of(COMPLETED)), anyList(), anyList()))
				.thenReturn(new EarlierFeedbackChecker.Outcome(List.of(followUp), List.of("E1"), List.of(), false));

		ReviewResult result = reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdf, progress,
				List.of(COMPLETED, OPEN)));

		assertThat(result.followUps()).containsExactly(followUp);
		assertThat(result.summary()).isEqualTo("Merged.");
		assertThat(result.summary()).doesNotContain(IncompleteCoverage.EARLIER_FEEDBACK_UNCHECKED);
		int total = ReviewCategory.values().length + 2;
		verify(progress).stepStarted(eq(CategoryFanOutReviewer.EARLIER_FEEDBACK_STEP_ID), anyString(), eq(total - 1),
				eq(total));
		verify(progress).stepCompleted(CategoryFanOutReviewer.EARLIER_FEEDBACK_STEP_ID, total - 1, total);
	}

	@Test
	void aFailedComparisonIsContainedAndTheReviewStillCompletes() {
		when(checker.check(any(), anyList(), anyList(), anyList())).thenThrow(new IllegalStateException("provider down"));

		ReviewResult result = reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdf, progress,
				List.of(COMPLETED)));

		assertThat(result.assessment()).isEqualTo(AssessmentCategory.ACCEPTABLE);
		assertThat(result.score()).isEqualTo(70);
		assertThat(result.followUps()).isEmpty();
		// The failure outlives the progress list: the saved summary says the comparison did not happen.
		assertThat(result.summary()).isEqualTo("Merged.\n\n" + IncompleteCoverage.EARLIER_FEEDBACK_UNCHECKED);
		verify(progress).stepFailed(eq(CategoryFanOutReviewer.EARLIER_FEEDBACK_STEP_ID), any(Integer.class),
				any(Integer.class), anyString());
	}

	@Test
	void onlyOpenEntriesNeedNoComparisonCall() {
		reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdf, progress, List.of(OPEN)));

		verify(checker, never()).check(any(), anyList(), anyList(), anyList());
		verify(progress, never()).stepStarted(eq(CategoryFanOutReviewer.EARLIER_FEEDBACK_STEP_ID), anyString(),
				any(Integer.class), any(Integer.class));
	}

	@Test
	void theMergePromptListsOpenAndPersistingEntriesInTheirOwnFence() {
		EarlierFeedback persisting = COMPLETED;
		String prompt = reviewer.buildMergePrompt(Map.of(), List.of(OPEN, persisting), List.of("E1"));

		assertThat(prompt).contains("<intermediate-findings>")
				.contains("<earlier-feedback>")
				.contains("ref: E2 | status: open")
				.contains("ref: E1 | status: persists");
		assertThat(reviewer.buildMergePrompt(Map.of())).doesNotContain("<earlier-feedback>");
		verify(chatClient, never()).prompt();
	}

	private static StructuredOutputConverter<ReviewVerdict> anyVerdictConverter() {
		return any();
	}
}
