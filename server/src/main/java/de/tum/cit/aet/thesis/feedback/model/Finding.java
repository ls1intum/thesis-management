package de.tum.cit.aet.thesis.feedback.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * One issue a review run reports about the document.
 *
 * <p>{@code severity} and {@code category} are free-form strings rather than enums on purpose:
 * they come straight out of a model's structured output, and a value outside the expected set must
 * not fail deserialization of an otherwise usable review. They are mapped onto the persisted enums
 * when the finding is turned into thesis feedback.
 *
 * @param severity    the model's severity token (CRITICAL / MAJOR / MINOR / SUGGESTION)
 * @param category    the model's category token (structure, citation, writing, ...)
 * @param title       a one-line summary of the issue
 * @param description what is wrong and what to do about it
 * @param locations   where the issue occurs; never {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Finding(
		String severity,
		String category,
		String title,
		String description,
		List<Location> locations
) {
	public Finding {
		locations = locations == null ? List.of()
				: locations.stream().filter(Objects::nonNull).toList();
	}
}
