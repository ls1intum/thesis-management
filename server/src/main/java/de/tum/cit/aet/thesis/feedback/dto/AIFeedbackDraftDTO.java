package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;

/**
 * A single AI-generated finding rendered into the shape the instructor UI needs: a plain feedback
 * text (title + description + location hint), a category, and a severity. Returned by the preview
 * endpoint so the instructor can edit / accept the item before saving.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AIFeedbackDraftDTO(
		String feedback,
		ThesisFeedbackCategory category,
		ThesisFeedbackSeverity severity
) {}
