package de.tum.cit.aet.thesis.feedback.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One feedback entry the splitter carved out of a supervisor's raw notes.
 *
 * <p>{@code category} and {@code severity} are free-form strings for the same reason they are on
 * {@link Finding}: they come straight out of a model's structured output, and an unexpected token
 * must not fail deserialization of an otherwise usable split. Both are expected to be absent
 * whenever the note itself does not say enough to judge — the instructor then picks the value by
 * hand or asks for a classification afterwards.
 *
 * @param feedback the entry text, phrased in the supervisor's own words
 * @param category the model's category token (structure, citation, writing, ...); may be
 *                 {@code null}
 * @param severity the model's severity token (CRITICAL / MAJOR / MINOR / SUGGESTION); may be
 *                 {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record NoteEntry(
		String feedback,
		String category,
		String severity
) {}
