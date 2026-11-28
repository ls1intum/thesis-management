package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ReviewToolboxComparisonTest {

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Group overview.",
			List.of(new CategoryGuidelines("structure", List.of("Every proposal must contain an Abstract."))));

	private static final List<String> PAGES = List.of(
			"Abstract\nThe system don't handle edge cases properly when multiple users connect.",
			"Introduction\nWe present a novel approach to scheduling.");

	private static final EarlierFeedback COMPLETED = new EarlierFeedback("E1", UUID.randomUUID(),
			"Grammar: subject-verb agreement in the abstract.", "WRITING", "MINOR", Instant.parse("2026-11-01T10:00:00Z"),
			0, false);

	private static final EarlierFeedback OPEN = new EarlierFeedback("E2", UUID.randomUUID(),
			"Add a related work section.", "STRUCTURE", "MAJOR", null, 0, false);

	@Mock
	private ProgressReporter progress;

	private ReviewToolbox toolbox(List<EarlierFeedback> earlier) {
		return new ReviewToolbox(PAGES, GUIDELINES, ReviewType.PROPOSAL, earlier, progress, new ReviewMeter());
	}

	private static String call(ReviewToolbox toolbox, String tool, String arguments) {
		ToolCallback callback = toolbox.callbacks().stream()
				.filter(candidate -> candidate.getToolDefinition().name().equals(tool))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No such tool: " + tool));
		return callback.call(arguments);
	}

	private static List<String> toolNames(ReviewToolbox toolbox) {
		return toolbox.callbacks().stream().map(callback -> callback.getToolDefinition().name()).toList();
	}

	@Test
	void aFirstReviewIsNotOfferedTheComparisonTools() {
		ReviewToolbox toolbox = toolbox(List.of());

		assertThat(toolbox.compares()).isFalse();
		assertThat(toolNames(toolbox)).doesNotContain("get_earlier_feedback", "record_follow_up", "mark_resolved");
		assertThat(toolbox.earlierFeedbackUnchecked()).isFalse();
	}

	@Test
	void earlierFeedbackComesBackFencedWithItsStatus() {
		ReviewToolbox toolbox = toolbox(List.of(COMPLETED, OPEN));
		assertThat(toolNames(toolbox)).contains("get_earlier_feedback", "record_follow_up", "mark_resolved");
		assertThat(toolbox.earlierFeedbackUnchecked()).isTrue();

		String listed = call(toolbox, "get_earlier_feedback", "{}");

		assertThat(listed).startsWith("<earlier-feedback>").endsWith("</earlier-feedback>");
		assertThat(listed).contains("ref: E1 | status: completed").contains("ref: E2 | status: open");
		assertThat(toolbox.earlierFeedbackUnchecked()).isFalse();
	}

	@Test
	void theComparisonIsAnnouncedAsAStepAndFailsWhenTheAgentSkipsIt() {
		ReviewToolbox toolbox = toolbox(List.of(COMPLETED));
		int total = ReviewCategory.values().length + 2;

		toolbox.close();

		verify(progress).stepStarted(eq(ReviewToolbox.EARLIER_FEEDBACK_STEP_ID), anyString(), eq(total - 1), eq(total));
		verify(progress).stepFailed(eq(ReviewToolbox.EARLIER_FEEDBACK_STEP_ID), eq(total - 1), eq(total), anyString());
	}

	@Test
	void aFollowUpToACompletedEntryIsRecordedWithTheOriginalsLabels() {
		ReviewToolbox toolbox = toolbox(List.of(COMPLETED));

		String answer = call(toolbox, "record_follow_up", """
				{"ref": "e1", "title": "Subject-verb agreement is still off",
				"description": "Use 'doesn't' with a singular subject, e.g. 'The system doesn't handle ...'.",
				"section": "Abstract", "page": 1,
				"quote": "The system don't handle edge cases properly when multiple users connect."}
				""");
		ReviewResult result = toolbox.close();

		assertThat(answer).startsWith("Recorded");
		assertThat(result.findings()).isEmpty();
		assertThat(result.followUps()).singleElement().satisfies(followUp -> {
			assertThat(followUp.ref()).isEqualTo("E1");
			assertThat(followUp.finding().category()).isEqualTo("WRITING");
			assertThat(followUp.finding().severity()).isEqualTo("MINOR");
			assertThat(followUp.finding().locations()).singleElement()
					.satisfies(location -> assertThat(location.page()).isEqualTo(1));
		});
	}

	@Test
	void aFollowUpToAnOpenEntryIsRefused() {
		ReviewToolbox toolbox = toolbox(List.of(OPEN));

		String answer = call(toolbox, "record_follow_up", "{\"ref\": \"E2\", \"title\": \"Still no related work\"}");

		assertThat(answer).startsWith("Rejected").contains("still open");
		assertThat(toolbox.close().followUps()).isEmpty();
	}

	@Test
	void aFollowUpToAnUnknownEntryOrWithAnInventedQuoteIsRefused() {
		ReviewToolbox toolbox = toolbox(List.of(COMPLETED));

		assertThat(call(toolbox, "record_follow_up", "{\"ref\": \"E9\", \"title\": \"x\"}")).startsWith("Rejected");
		assertThat(call(toolbox, "record_follow_up", """
				{"ref": "E1", "title": "x", "quote": "This sentence appears nowhere in the uploaded document at all."}
				""")).startsWith("Rejected").contains("quote");
		assertThat(toolbox.close().followUps()).isEmpty();
	}

	@Test
	void onlyCompletedEntriesCanBeMarkedResolvedAndAFollowUpOverridesTheMark() {
		EarlierFeedback another = new EarlierFeedback("E3", UUID.randomUUID(), "Number the figures.", "FIGURES",
				"MINOR", Instant.parse("2026-11-01T10:00:00Z"), 0, false);
		ReviewToolbox toolbox = toolbox(List.of(COMPLETED, OPEN, another));

		assertThat(call(toolbox, "mark_resolved", "{\"ref\": \"E2\"}")).startsWith("Ignored");
		call(toolbox, "mark_resolved", "{\"ref\": \"E3\"}");
		call(toolbox, "mark_resolved", "{\"ref\": \"E1\"}");
		call(toolbox, "record_follow_up", "{\"ref\": \"E1\", \"title\": \"Agreement still wrong\"}");
		ReviewResult result = toolbox.close();

		assertThat(result.resolvedRefs()).containsExactly("E3");
		assertThat(result.followUps()).extracting(followUp -> followUp.ref()).containsExactly("E1");
	}
}
