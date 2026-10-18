package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ReviewToolboxTest {

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Group overview.",
			List.of(new CategoryGuidelines("structure", List.of("Every proposal must contain an Abstract."))));

	private static final List<String> PAGES = List.of(
			"Abstract\nThe system don't handle edge cases properly when multiple users connect.",
			"Introduction\nWe present a novel approach to scheduling.");

	@Mock
	private ProgressReporter progress;

	private ReviewToolbox toolbox() {
		return new ReviewToolbox(PAGES, GUIDELINES, ReviewType.PROPOSAL, progress, new ReviewMeter());
	}

	private static String call(ReviewToolbox toolbox, String tool, String arguments) {
		ToolCallback callback = toolbox.callbacks().stream()
				.filter(candidate -> candidate.getToolDefinition().name().equals(tool))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No such tool: " + tool));
		return callback.call(arguments);
	}

	@Test
	void everyToolTheSystemPromptNamesIsActuallyRegistered() {
		List<String> names = toolbox().callbacks().stream()
				.map(callback -> callback.getToolDefinition().name())
				.toList();

		assertThat(names).containsExactly("list_review_categories", "get_category_rules", "get_page_text",
				"search_document", "record_finding", "finish_review");
	}

	@Test
	void finishingTheReviewReturnsDirectlySoTheAgentLoopEndsOnTheVerdict() {
		// Without this the loop would make one more call just to learn there is nothing left to do.
		ToolCallback finish = toolbox().callbacks().stream()
				.filter(callback -> callback.getToolDefinition().name().equals("finish_review"))
				.findFirst()
				.orElseThrow();

		assertThat(finish.getToolMetadata().returnDirect()).isTrue();
	}

	@Test
	void fetchingACategorysRulesIsWhatCountsItAsChecked() {
		ReviewToolbox toolbox = toolbox();
		int total = ReviewCategory.values().length + 1;

		String rules = call(toolbox, "get_category_rules", "{\"category\": \"structure\"}");

		// The group's rules exist nowhere but here, which is what makes the call an honest record
		// that the dimension was looked at.
		assertThat(rules).contains("Every proposal must contain an Abstract.");
		assertThat(toolbox.coveredCount()).isEqualTo(1);
		assertThat(toolbox.uncovered()).doesNotContain(ReviewCategory.STRUCTURE);
		verify(progress).stepCompleted(eq("structure"), anyInt(), eq(total));
	}

	@Test
	void everyCategoryIsAnnouncedUpFrontAndTheUncheckedOnesFailWhenTheRunCloses() {
		ReviewToolbox toolbox = toolbox();
		int total = ReviewCategory.values().length + 1;

		for (ReviewCategory category : ReviewCategory.values()) {
			verify(progress).stepStarted(category.getSlug(), category.getDisplayName(), category.ordinal() + 1, total);
		}

		call(toolbox, "get_category_rules", "{\"category\": \"structure\"}");
		toolbox.close();

		// A dimension the agent never asked about is reported as a check that did not happen, not
		// silently dropped.
		verify(progress, never()).stepFailed(eq("structure"), anyInt(), anyInt(), eq(ReviewToolbox.NOT_CHECKED_MESSAGE));
		verify(progress).stepFailed(eq("bibliography"), anyInt(), eq(total), eq(ReviewToolbox.NOT_CHECKED_MESSAGE));
		verify(progress).stepStarted(ReviewToolbox.FINALIZE_STEP_ID, "Consolidating findings", total, total);
		verify(progress).stepCompleted(ReviewToolbox.FINALIZE_STEP_ID, total, total);
	}

	@Test
	void anUnknownCategoryIdIsAnsweredWithGuidanceRatherThanAnError() {
		// The agent can recover from this on its own turn; throwing would end the whole review.
		String answer = call(toolbox(), "get_category_rules", "{\"category\": \"spelling\"}");

		assertThat(answer).contains("Unknown dimension 'spelling'").contains("list_review_categories");
	}

	@Test
	void pageTextComesBackFencedAsUntrustedUploadContent() {
		String answer = call(toolbox(), "get_page_text", "{\"page\": 1}");

		assertThat(answer)
				.startsWith("<" + ReviewToolbox.PAGE_TEXT_TAG + ">")
				.endsWith("</" + ReviewToolbox.PAGE_TEXT_TAG + ">")
				.contains("=== PAGE 1 ===")
				.contains("The system don't handle edge cases");
	}

	@Test
	void aPageOutsideTheDocumentIsAnsweredWithItsActualRange() {
		String answer = call(toolbox(), "get_page_text", "{\"page\": 7}");

		assertThat(answer).isEqualTo("Page 7 does not exist; the document has pages 1-2.");
	}

	@Test
	void searchFindsATermAcrossPagesAndReportsWhereItIs() {
		String answer = call(toolbox(), "search_document", "{\"query\": \"novel approach\"}");

		assertThat(answer).contains("page 2").contains("novel approach");
	}

	@Test
	void aFindingWithAQuoteFromTheDocumentIsRecorded() {
		ReviewToolbox toolbox = toolbox();

		String answer = call(toolbox, "record_finding", """
				{"severity": "major", "category": "writing", "title": "Subject-verb disagreement",
				"description": "Use does not.", "section": "Abstract", "page": 1,
				"quote": "The system **don't** handle edge cases properly when multiple users connect."}""");

		assertThat(answer).isEqualTo("Recorded finding #1.");
		ReviewResult result = toolbox.close();
		assertThat(result.findings()).singleElement().satisfies(finding -> {
			// Tokens are upper-cased here so FeedbackMapper sees the same vocabulary the fan-out
			// gives it.
			assertThat(finding.severity()).isEqualTo("MAJOR");
			assertThat(finding.category()).isEqualTo("WRITING");
			assertThat(finding.title()).isEqualTo("Subject-verb disagreement");
			assertThat(finding.locations()).containsExactly(new Location(1, "Abstract",
					"The system **don't** handle edge cases properly when multiple users connect."));
		});
	}

	@Test
	void aFindingWhoseQuoteIsNotInTheDocumentIsRefusedRatherThanRecorded() {
		ReviewToolbox toolbox = toolbox();

		String answer = call(toolbox, "record_finding", """
				{"severity": "CRITICAL", "category": "CITATION", "title": "Fabricated claim",
				"description": "Nobody wrote this.",
				"quote": "This sentence appears nowhere in the uploaded proposal whatsoever."}""");

		// A quote the student cannot find is a finding they cannot act on, and it is the one claim
		// in a finding that can be settled without another model call.
		assertThat(answer).startsWith("Rejected:").contains("does not appear in the document");
		assertThat(toolbox.close().findings()).isEmpty();
	}

	@Test
	void aFindingAboutSomethingMissingNeedsNoQuote() {
		ReviewToolbox toolbox = toolbox();

		String answer = call(toolbox, "record_finding", """
				{"severity": "CRITICAL", "category": "STRUCTURE", "title": "No Schedule section",
				"description": "Add a Schedule section."}""");

		assertThat(answer).isEqualTo("Recorded finding #1.");
		assertThat(toolbox.close().findings()).singleElement()
				.extracting(Finding::locations).isEqualTo(List.of());
	}

	@Test
	void aFindingWithoutATitleIsRefused() {
		ReviewToolbox toolbox = toolbox();

		String answer = call(toolbox, "record_finding", "{\"severity\": \"MINOR\", \"description\": \"Something.\"}");

		assertThat(answer).startsWith("Rejected:");
		assertThat(toolbox.close().findings()).isEmpty();
	}

	@Test
	void finishingRecordsTheVerdictAndEndsTheRun() {
		ReviewToolbox toolbox = toolbox();
		assertThat(toolbox.finished()).isFalse();

		call(toolbox, "finish_review",
				"{\"assessment\": \"needs-work\", \"score\": 55, \"summary\": \"Promising but unfinished.\"}");

		assertThat(toolbox.finished()).isTrue();
		ReviewResult result = toolbox.close();
		assertThat(result.assessment()).isEqualTo(AssessmentCategory.NEEDS_WORK);
		assertThat(result.normalizedScore()).isEqualTo(55);
		assertThat(result.summary()).isEqualTo("Promising but unfinished.");
	}

	@Test
	void aRunThatNeverFinishesStillHandsOverWhatItRecorded() {
		ReviewToolbox toolbox = toolbox();
		call(toolbox, "record_finding",
				"{\"severity\": \"MINOR\", \"category\": \"WRITING\", \"title\": \"Wordy\", \"description\": \"Trim it.\"}");

		// No finish_review: the agent ran out of turns, or the connection died.
		ReviewResult result = toolbox.close();

		assertThat(result.findings()).hasSize(1);
		assertThat(result.assessment()).isNull();
		assertThat(result.summary()).isNull();
	}

	@Test
	void listingTheCategoriesNamesEveryDimensionTheRunMustCover() {
		String answer = call(toolbox(), "list_review_categories", "{}");

		for (ReviewCategory category : ReviewCategory.values()) {
			assertThat(answer).contains(category.getSlug()).contains(category.getDisplayName());
		}
	}
}
