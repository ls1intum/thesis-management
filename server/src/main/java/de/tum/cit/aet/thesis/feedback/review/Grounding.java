package de.tum.cit.aet.thesis.feedback.review;

import java.util.List;
import java.util.Locale;

/**
 * Checks a model's quote against the document it claims to come from.
 *
 * <p>A fabricated quote is the failure mode that costs a review its credibility: a student who
 * cannot find the sentence they are being corrected on has no way to act on the finding, and no
 * reason to trust the next one. It is also the one claim in a finding that can be settled in code,
 * without asking a model to grade another model.
 *
 * <p>The comparison is deliberately forgiving about form and strict about substance. Extracted PDF
 * text is full of soft line breaks, doubled spaces, and typographic quotes and dashes that no model
 * reproduces exactly, and the review prompt itself asks for {@code **bold**} markers around the
 * offending part — so case, whitespace, emphasis markers and punctuation variants are normalized
 * away before the comparison, and a second pass folds the f-ligatures that PDF extraction mangles.
 * What is left is the wording, and that has to match.
 */
final class Grounding {

	/**
	 * Shortest quote worth checking. Below this a quote is a word or two — too short for an exact
	 * match to mean anything, and too likely to be defeated by hyphenation across a line break.
	 */
	private static final int MIN_CHECKED_LENGTH = 24;

	private Grounding() {
	}

	/**
	 * Whether a quote actually occurs in the document.
	 *
	 * @param pages the document's per-page extracted text
	 * @param quote the model's quote
	 * @return {@code true} when the quote occurs, or is too short to judge
	 */
	static boolean isGrounded(List<String> pages, String quote) {
		String needle = normalize(quote);
		if (needle.length() < MIN_CHECKED_LENGTH) {
			return true;
		}
		if (pages.stream().anyMatch(page -> normalize(page).contains(needle))) {
			return true;
		}
		// Second pass with ligatures folded. Extraction routinely renders a ligature glyph as its
		// first letter alone — "different" comes out "diferent", "fires" comes out "fres" — and a
		// model quoting the sentence writes the word correctly, because that is what a reader sees.
		// Comparing the two literally then rejects a perfectly real quote. Both sides are folded, so
		// the comparison is between forms that agree however the glyph survived extraction.
		String folded = foldLigatures(needle);
		return pages.stream().anyMatch(page -> foldLigatures(normalize(page)).contains(folded));
	}

	/**
	 * Collapses the f-ligatures to the single letter extraction leaves behind. Applied to both the
	 * quote and the document, so it only ever makes two spellings of the same word agree.
	 */
	private static String foldLigatures(String value) {
		return value
				.replace("\ufb03", "f").replace("\ufb04", "f")
				.replace("\ufb00", "f").replace("\ufb01", "f").replace("\ufb02", "f")
				.replace("ffi", "f").replace("ffl", "f")
				.replace("ff", "f").replace("fi", "f").replace("fl", "f");
	}

	/**
	 * Reduces text to what both sides can be expected to agree on: lower case, single spaces, no
	 * emphasis markers, and ASCII quotes and dashes.
	 *
	 * @param value the text to normalize
	 * @return the normalized text
	 */
	static String normalize(String value) {
		return value
				.replace("**", "")
				.replace('‘', '\'').replace('’', '\'')
				.replace('“', '"').replace('”', '"')
				.replace('–', '-').replace('—', '-').replace('−', '-')
				.replace(' ', ' ')
				.replaceAll("\\s+", " ")
				.strip()
				.toLowerCase(Locale.ROOT);
	}
}
