package de.tum.cit.aet.thesis.feedback.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Structured output of the note splitting call: a supervisor's raw notes carved into individual
 * feedback entries.
 *
 * @param entries the entries in the order they appeared in the notes; never {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record NoteSplitResult(List<NoteEntry> entries) {
	/** Canonicalizes {@code entries}: a model may omit the list or put nulls in it. */
	public NoteSplitResult {
		entries = entries == null ? List.of()
				: entries.stream().filter(Objects::nonNull).toList();
	}
}
