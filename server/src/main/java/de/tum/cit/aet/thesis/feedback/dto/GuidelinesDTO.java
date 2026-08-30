package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.feedback.entity.GuidelinesStatus;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewCategory;

import java.time.Instant;
import java.util.List;

/**
 * Full view of a research group's AI review guidelines returned to the lead, including the raw
 * input, the distilled per-category rules, the processing status, and — when preprocessing
 * rejected the input — the failure reason.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record GuidelinesDTO(
		GuidelinesStatus status,
		String rawGuidelines,
		String overview,
		List<CategoryGuidelinesDTO> categories,
		String failureReason,
		Instant processedAt,
		Instant updatedAt
) {
	/**
	 * Builds the DTO from a persisted entity, expanding the structured guidelines into per-category
	 * DTOs in the fixed category order and attaching each category's display name.
	 *
	 * @param entity the persisted guidelines
	 * @return the API representation
	 */
	/**
	 * Returns an empty representation used when a research group has no guidelines yet. With
	 * {@code NON_EMPTY} serialization this becomes an empty JSON object, which the client reads
	 * as the "not configured" state.
	 *
	 * @return an empty guidelines DTO
	 */
	public static GuidelinesDTO empty() {
		return new GuidelinesDTO(null, null, null, List.of(), null, null, null);
	}

	public static GuidelinesDTO fromEntity(ResearchGroupGuidelines entity) {
		StructuredGuidelines structured = entity.getStructuredGuidelines();
		List<CategoryGuidelinesDTO> categories = List.of();
		String overview = null;

		if (structured != null) {
			overview = structured.overview();
			categories = java.util.Arrays.stream(ReviewCategory.values())
					.map(category -> new CategoryGuidelinesDTO(
							category.getSlug(),
							category.getDisplayName(),
							structured.rulesForCategory(category.getSlug())))
					.toList();
		}

		return new GuidelinesDTO(
				entity.getStatus(),
				entity.getRawGuidelines(),
				overview,
				categories,
				entity.getFailureReason(),
				entity.getProcessedAt(),
				entity.getUpdatedAt());
	}
}
