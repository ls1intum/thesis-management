package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The distilled guideline rules for one fixed review category, as surfaced to the client so a
 * lead can verify what the preprocessing produced.
 *
 * @param category    the review category slug
 * @param displayName the human-readable category name
 * @param rules       the distilled, specific rules for this category
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CategoryGuidelinesDTO(
		String category,
		String displayName,
		List<String> rules
) {
}
