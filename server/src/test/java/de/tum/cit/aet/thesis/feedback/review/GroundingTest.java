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
	void aQuoteTooShortToJudgeIsAccepted() {
		// Two words prove nothing either way, and hyphenation across a line break would defeat an
		// exact match anyway — rejecting here would cost real findings.
		assertThat(Grounding.isGrounded(PAGES, "scheduling")).isTrue();
	}
}
