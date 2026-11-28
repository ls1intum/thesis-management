package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.review.EarlierFeedbackVerdicts.Verdict;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

class EarlierFeedbackCheckerTest {

	private static final List<String> PAGES = List.of(
			"Abstract\nThe system don't handle edge cases properly when multiple users connect.",
			"Introduction\nWe present a novel approach to scheduling.");

	private static final Instant COMPLETED_AT = Instant.parse("2026-11-01T10:00:00Z");

	private static final EarlierFeedback GRAMMAR = new EarlierFeedback("E1", UUID.randomUUID(),
			"Grammar: subject-verb agreement in the abstract.", "WRITING", "MINOR", COMPLETED_AT, 0, false);

	private static final EarlierFeedback FIGURES = new EarlierFeedback("E2", UUID.randomUUID(),
			"Number every figure.", "FIGURES", "MINOR", COMPLETED_AT, 0, false);

	private static final String GROUNDED = "The system don't handle edge cases properly when multiple users connect.";

	@Test
	void aPersistingDefectWithAGroundedQuoteBecomesAFollowUpWithTheOriginalsLabels() {
		EarlierFeedbackChecker.Outcome outcome = EarlierFeedbackChecker.interpret(List.of(
				new Verdict("e1", "persists", 1, "Abstract", GROUNDED, "Agreement still wrong",
						"Write 'doesn't'. For example: 'The system doesn't handle edge cases ...'.")),
				List.of(GRAMMAR), PAGES);

		assertThat(outcome.persistRefs()).containsExactly("E1");
		assertThat(outcome.followUps()).singleElement().satisfies(followUp -> {
			assertThat(followUp.ref()).isEqualTo("E1");
			assertThat(followUp.finding().category()).isEqualTo("WRITING");
			assertThat(followUp.finding().severity()).isEqualTo("MINOR");
			assertThat(followUp.finding().title()).isEqualTo("Agreement still wrong");
			assertThat(followUp.finding().locations()).singleElement().satisfies(location -> {
				assertThat(location.page()).isEqualTo(1);
				assertThat(location.quote()).isEqualTo(GROUNDED);
			});
		});
	}

	@Test
	void aFollowUpPointsAtThePageItsQuoteIsOnNotThePageTheModelClaimed() {
		String onPageTwo = "We present a novel approach to scheduling.";
		EarlierFeedbackChecker.Outcome outcome = EarlierFeedbackChecker.interpret(List.of(
				new Verdict("E1", "PERSISTS", 1, "Introduction", onPageTwo, "t", "e")),
				List.of(GRAMMAR), PAGES);

		assertThat(outcome.followUps().getFirst().finding().locations().getFirst().page()).isEqualTo(2);
	}

	@Test
	void aPersistingVerdictWithAnInventedQuoteDecidesNothing() {
		EarlierFeedbackChecker.Outcome outcome = EarlierFeedbackChecker.interpret(List.of(
				new Verdict("E1", "PERSISTS", 1, null, "This passage was never part of the uploaded document at all.",
						"t", "e")),
				List.of(GRAMMAR), PAGES);

		assertThat(outcome.followUps()).isEmpty();
		assertThat(outcome.persistRefs()).isEmpty();
		assertThat(outcome.resolvedRefs()).isEmpty();
	}

	@Test
	void aMissingThingPersistsWithoutAQuote() {
		EarlierFeedbackChecker.Outcome outcome = EarlierFeedbackChecker.interpret(List.of(
				new Verdict("E2", "PERSISTS", 99, null, null, null, "Still unnumbered.")),
				List.of(FIGURES), PAGES);

		assertThat(outcome.followUps()).singleElement().satisfies(followUp -> {
			// A page outside the document is dropped rather than shown.
			assertThat(followUp.finding().locations()).isEmpty();
			assertThat(followUp.finding().title()).isNotBlank();
		});
	}

	@Test
	void unknownRefsRepeatedVerdictsAndUnclearAnswersAreIgnored() {
		EarlierFeedbackChecker.Outcome outcome = EarlierFeedbackChecker.interpret(List.of(
				new Verdict("E7", "RESOLVED", null, null, null, null, null),
				new Verdict("E1", "RESOLVED", null, null, null, null, null),
				new Verdict("E1", "PERSISTS", 1, null, GROUNDED, "t", "e"),
				new Verdict("E2", "UNCLEAR", null, null, null, null, null)),
				List.of(GRAMMAR, FIGURES), PAGES);

		assertThat(outcome.resolvedRefs()).containsExactly("E1");
		assertThat(outcome.followUps()).isEmpty();
	}

	@Test
	void theCheckPromptFencesTheEntriesAndTheUploadSeparately() {
		String prompt = EarlierFeedbackPrompt.checkSystemPrompt(ReviewType.THESIS);
		String entries = EarlierFeedbackPrompt.fenced(List.of(GRAMMAR), entry -> "completed");

		assertThat(prompt).contains("<earlier-feedback>").contains("<student-upload-page-text>").contains("thesis");
		assertThat(entries).startsWith("<earlier-feedback>\n").endsWith("\n</earlier-feedback>")
				.contains("- ref: E1 | status: completed | category: WRITING | severity: MINOR\n")
				.contains("text: Grammar: subject-verb agreement in the abstract.");
	}
}
