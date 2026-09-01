package de.tum.cit.aet.thesis.feedback.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/** Overall verdict a review run reaches about the document as a whole. */
public enum AssessmentCategory {
	GOOD,
	ACCEPTABLE,
	NEEDS_WORK;

	/**
	 * Lenient deserialization for a model's assessment token. Prompts ask for {@code "good"} /
	 * {@code "acceptable"} / {@code "needs-work"}, but a schema-driven response may instead echo the
	 * enum names — accept both spellings (and {@code needs_work}) regardless of case. Serialization
	 * is left untouched, so the value is still written back to the client as the enum name.
	 *
	 * <p>Unrecognized values resolve to {@code null} rather than throwing, so an unexpected token
	 * never fails the whole review after every LLM call has already completed.
	 *
	 * @param value the raw token from the model's structured output
	 * @return the matching category, or {@code null} when the token is blank or unrecognized
	 */
	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static AssessmentCategory fromJson(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return switch (value.trim().replace('-', '_').toUpperCase(Locale.ROOT)) {
			case "GOOD" -> GOOD;
			case "ACCEPTABLE" -> ACCEPTABLE;
			case "NEEDS_WORK" -> NEEDS_WORK;
			default -> null;
		};
	}
}
