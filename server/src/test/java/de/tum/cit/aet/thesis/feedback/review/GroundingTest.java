package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.List;

class GroundingTest {

	private static final List<String> PAGES = List.of(
			"Abstract\nThe system  don't handle edge\ncases properly when multiple users connect.",
			"Introduction\nWe present a “novel” approach — one that scales.");

	@Test
	void aQuoteCopiedVerbatimIsGrounded() {
		assertThat(Grounding.isGrounded(PAGES,
				"The system don't handle edge cases properly when multiple users connect.")).isTrue();
	}

	@Test
	void theEmphasisMarkersTheReviewPromptAsksForDoNotBreakTheMatch() {
		// The shared prompt tells the model to wrap the offending part in **bold**, so a quote that
		// follows the instruction must still count as grounded.
		assertThat(Grounding.isGrounded(PAGES,
				"The system **don't** handle edge cases properly when multiple users connect.")).isTrue();
	}

	@Test
	void typographicPunctuationAndSoftLineBreaksDoNotBreakTheMatch() {
		// Extracted PDF text is full of these and no model reproduces them exactly.
		assertThat(Grounding.isGrounded(PAGES, "We present a \"novel\" approach - one that scales.")).isTrue();
	}

	@Test
	void aQuoteSpelledCorrectlyMatchesTextWhoseLigaturesExtractionDestroyed() {
		// PDF extraction renders a ligature glyph as its first letter: "different" arrives as
		// "diferent", "fires" as "fres". A model quoting the sentence writes the word correctly,
		// because that is what a reader sees — rejecting it loses a real finding.
		List<String> mangled = List.of("Four groups of people each pay a diferent cost when access never expires.");

		assertThat(Grounding.isGrounded(mangled,
				"Four groups of people each pay a different cost when access never expires.")).isTrue();
	}

	@Test
	void ligatureFoldingDoesNotMakeUnrelatedSentencesMatch() {
		assertThat(Grounding.isGrounded(PAGES,
				"Four groups of people each pay a different cost when access never expires.")).isFalse();
	}

	@Test
	void aSentenceThatIsNotInTheDocumentIsNotGrounded() {
		assertThat(Grounding.isGrounded(PAGES,
				"The evaluation compares three scheduling strategies across two datasets.")).isFalse();
	}

	@Test
	void aSentenceRunningAcrossAPageBreakIsGrounded() {
		List<String> pages = List.of(
				"Intro\nThe evaluation compares three scheduling",
				"strategies across two datasets.\nResults follow.",
				"Conclusion\nDone.");

		assertThat(Grounding.isGrounded(pages,
				"The evaluation compares three scheduling strategies across two datasets.")).isTrue();
	}

	@Test
	void joiningPagesOnlyBridgesNeighbours() {
		// Pages one and three are not adjacent, so text split between them is not one sentence.
		List<String> pages = List.of(
				"The evaluation compares three scheduling",
				"Unrelated middle page.",
				"strategies across two datasets.");

		assertThat(Grounding.isGrounded(pages,
				"The evaluation compares three scheduling strategies across two datasets.")).isFalse();
	}

	@Test
	void aWordSplitByALineEndHyphenMatchesItsWholeSpelling() {
		List<String> pages = List.of("The platform improves inter-\noperability between the existing services.");

		assertThat(Grounding.isGrounded(pages,
				"The platform improves interoperability between the existing services.")).isTrue();
	}

	@Test
	void aCompoundHyphenAtALineEndStillMatchesWithItsHyphen() {
		// Whether the hyphen splits a word or belongs to it cannot be told from the text, so a real
		// compound must not be broken by the joining.
		List<String> pages = List.of("We compare against the state-of-\nthe-art baselines in the field.");

		assertThat(Grounding.isGrounded(pages,
				"We compare against the state-of-the-art baselines in the field.")).isTrue();
	}

	@Test
	void aQuoteTooShortToJudgeIsAccepted() {
		// Two words prove nothing either way, and hyphenation across a line break would defeat an
		// exact match anyway — rejecting here would cost real findings.
		assertThat(Grounding.isGrounded(PAGES, "scheduling")).isTrue();
	}
}
