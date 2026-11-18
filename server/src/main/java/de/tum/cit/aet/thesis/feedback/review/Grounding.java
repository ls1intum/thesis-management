package de.tum.cit.aet.thesis.feedback.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

	/** A hyphen between two letters with a line break after it, as extraction leaves a split word. */
	private static final Pattern LINE_END_HYPHEN = Pattern.compile("(?<=\\p{L})-[ \\t]*\\R[ \\t]*(?=\\p{L})");

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
		List<String> haystacks = acrossPageBreaks(pages).stream()
				.flatMap(Grounding::lineBreakVariants)
				.map(Grounding::normalize)
				.toList();
		if (haystacks.stream().anyMatch(text -> text.contains(needle))) {
			return true;
		}
		// Second pass with ligatures folded. Extraction routinely renders a ligature glyph as its
		// first letter alone — "different" comes out "diferent", "fires" comes out "fres" — and a
		// model quoting the sentence writes the word correctly, because that is what a reader sees.
		// Comparing the two literally then rejects a perfectly real quote. Both sides are folded, so
		// the comparison is between forms that agree however the glyph survived extraction.
		String folded = foldLigatures(needle);
		return haystacks.stream().anyMatch(text -> foldLigatures(text).contains(folded));
	}

	/**
	 * A page as extracted, plus the two readings of a hyphen at the end of a line. Extraction keeps
	 * the line break, so {@code inter-\noperability} would normalize to {@code inter- operability}
	 * and match neither spelling a model writes. Whether that hyphen splits a word or belongs to a
	 * compound cannot be told from the text, so both are offered: joined ({@code interoperability})
	 * and kept ({@code state-of-the-art}).
	 */
	private static Stream<String> lineBreakVariants(String text) {
		return Stream.of(text,
				LINE_END_HYPHEN.matcher(text).replaceAll(""),
				LINE_END_HYPHEN.matcher(text).replaceAll("-"))
				.distinct();
	}

	/**
	 * The text a quote is searched in: every pair of consecutive pages joined by a space, so a
	 * sentence that runs across a page break still matches. Each pair contains both its pages, so a
	 * quote within one page is found as before; a single-page document is searched as it is.
	 */
	private static List<String> acrossPageBreaks(List<String> pages) {
		if (pages.size() < 2) {
			return pages;
		}
		List<String> pairs = new ArrayList<>(pages.size() - 1);
		for (int i = 0; i + 1 < pages.size(); i++) {
			pairs.add(pages.get(i) + " " + pages.get(i + 1));
		}
		return pairs;
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
