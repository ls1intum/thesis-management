package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Exercises everything around the agent loop — coverage bookkeeping, partial results, the
 * incomplete-coverage warning — by substituting the one model-driven step.
 */
@ExtendWith(MockitoExtension.class)
class AgenticReviewerTest {

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Group overview.",
			List.of(new CategoryGuidelines("structure", List.of("Every proposal must contain an Abstract."))));

	private static final List<String> PAGES = List.of(
			"Abstract\nThe system don't handle edge cases properly when multiple users connect.");

	@Mock
	private PdfService pdfService;

	@Mock
	private ChatClient.Builder chatClientBuilder;

	@Mock
	private ChatClient chatClient;

	@Mock
	private ToolCallingManager toolCallingManager;

	@Mock
	private ProgressReporter progress;

	private final List<ReviewRunMetrics> recorded = new ArrayList<>();

	private final Resource document = new ByteArrayResource("pdf-content".getBytes());

	@BeforeEach
	void setUp() {
		when(chatClientBuilder.build()).thenReturn(chatClient);
	}

	/**
	 * Builds a reviewer whose agent turn is the given script: it is handed the toolbox and the
	 * meter and does whatever a real agent would have done through its tools.
	 */
	private AgenticReviewer reviewerRunning(BiConsumer<ReviewToolbox, ReviewMeter> script) {
		return new AgenticReviewer(pdfService, chatClientBuilder, toolCallingManager, recorded::add, 40) {
			@Override
			protected void runAgent(ReviewToolbox toolbox, ReviewMeter meter, ReviewType type, List<String> pages) {
				script.accept(toolbox, meter);
			}
		};
	}

	private static void call(ReviewToolbox toolbox, String tool, String arguments) {
		ToolCallback callback = toolbox.callbacks().stream()
				.filter(candidate -> candidate.getToolDefinition().name().equals(tool))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No such tool: " + tool));
		callback.call(arguments);
	}

	private static void coverEveryCategory(ReviewToolbox toolbox) {
		for (ReviewCategory category : ReviewCategory.values()) {
			call(toolbox, "get_category_rules", "{\"category\": \"" + category.getSlug() + "\"}");
		}
	}

	private ReviewResult review(AgenticReviewer reviewer) {
		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(PAGES);
		return reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, document, progress));
	}

	@Test
	void aRunThatCoveredEveryCategoryKeepsTheAgentsVerdictUntouched() {
		ReviewResult result = review(reviewerRunning((toolbox, meter) -> {
			coverEveryCategory(toolbox);
			call(toolbox, "record_finding", """
					{"severity": "MAJOR", "category": "WRITING", "title": "Subject-verb disagreement",
					"description": "Use does not.", "page": 1,
					"quote": "The system don't handle edge cases properly when multiple users connect."}""");
			call(toolbox, "finish_review",
					"{\"assessment\": \"needs-work\", \"score\": 55, \"summary\": \"Promising but unfinished.\"}");
		}));

		assertThat(result.findings()).hasSize(1);
		assertThat(result.normalizedScore()).isEqualTo(55);
		assertThat(result.summary()).isEqualTo("Promising but unfinished.");
	}

	@Test
	void aRunThatSkippedCategoriesGivesUpItsScoreAndAssessmentAndSaysWhichChecksAreMissing() {
		ReviewResult result = review(reviewerRunning((toolbox, meter) -> {
			call(toolbox, "get_category_rules", "{\"category\": \"structure\"}");
			call(toolbox, "record_finding", """
					{"severity": "CRITICAL", "category": "STRUCTURE", "title": "No Schedule section",
					"description": "Add a Schedule section."}""");
			// The agent declares itself done having looked at one dimension of nine.
			call(toolbox, "finish_review",
					"{\"assessment\": \"good\", \"score\": 92, \"summary\": \"A solid proposal.\"}");
		}));

		// The finding is real and survives; the two claims one check cannot support do not, and the
		// caveat is in the summary rather than only in a log line.
		assertThat(result.findings()).hasSize(1);
		assertThat(result.assessment()).isNull();
		assertThat(result.normalizedScore()).isNull();
		assertThat(result.summary())
				.startsWith("Incomplete review: " + (ReviewCategory.values().length - 1) + " of "
						+ ReviewCategory.values().length + " checks could not be completed (")
				.contains("no overall score or assessment is given")
				.endsWith("A solid proposal.");
	}

	@Test
	void anAgentThatDiesMidRunKeepsTheFindingsItAlreadyRecorded() {
		ReviewResult result = review(reviewerRunning((toolbox, meter) -> {
			coverEveryCategory(toolbox);
			call(toolbox, "record_finding", """
					{"severity": "MINOR", "category": "WRITING", "title": "Wordy", "description": "Trim it."}""");
			throw new IllegalStateException("502 Bad Gateway");
		}));

		// A long agentic run is expensive; throwing away its findings over the last turn is not an
		// option, so the error costs the run its verdict and nothing else.
		assertThat(result.findings()).hasSize(1);
		assertThat(result.assessment()).isNull();
		assertThat(result.normalizedScore()).isNull();
	}

	@Test
	void anAgentThatDidNothingAtAllIsAFailureRatherThanAnEmptyReview() {
		AgenticReviewer reviewer = reviewerRunning((toolbox, meter) -> {
			throw new IllegalStateException("401 Unauthorized");
		});
		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(PAGES);
		ReviewRequest request = new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, document, progress);

		assertThatThrownBy(() -> reviewer.review(request)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void everyCategoryIsReportedAsAStepAndTheSkippedOnesAsChecksThatDidNotHappen() {
		int total = ReviewCategory.values().length + 1;

		review(reviewerRunning((toolbox, meter) -> {
			call(toolbox, "get_category_rules", "{\"category\": \"structure\"}");
			call(toolbox, "finish_review", "{\"assessment\": \"good\", \"score\": 80, \"summary\": \"Fine.\"}");
		}));

		for (ReviewCategory category : ReviewCategory.values()) {
			verify(progress).stepStarted(category.getSlug(), category.getDisplayName(), category.ordinal() + 1, total);
		}
		verify(progress).stepCompleted(eq("structure"), anyInt(), eq(total));
		verify(progress).stepFailed(eq("schedule"), anyInt(), eq(total), eq(ReviewToolbox.NOT_CHECKED_MESSAGE));
		verify(progress).stepCompleted(ReviewToolbox.FINALIZE_STEP_ID, total, total);
	}

	@Test
	void theDocumentIsReadOnceAndNoPageImagesAreRendered() {
		review(reviewerRunning((toolbox, meter) -> coverEveryCategory(toolbox)));

		verify(pdfService).extractTextFromPdf(document);
		// Text only on purpose: the agent re-sends its conversation every turn, and page images
		// carried across a dozen turns cost more than the figure checks they enable.
		verify(pdfService, org.mockito.Mockito.never()).extractImagesFromPdf(any(Resource.class));
	}
}
