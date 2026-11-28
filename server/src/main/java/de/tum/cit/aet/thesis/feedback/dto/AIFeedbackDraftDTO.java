package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;

import java.util.UUID;

/**
 * A single AI-generated finding rendered into the shape the instructor UI needs: a plain feedback
 * text (title + description + location hint), a category, and a severity. Returned by the preview
 * endpoint so the instructor can edit / accept the item before saving.
 *
 * @param feedback             the feedback text
 * @param category             the suggested category, or {@code null}
 * @param severity             the suggested severity, or {@code null}
 * @param followsUpId          the earlier, completed entry whose defect this draft reports as
 *                             recurring; {@code null} for a new finding
 * @param deferredToSupervisor whether this follow-up hands the recurring defect to the supervisor
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AIFeedbackDraftDTO(
		String feedback,
		ThesisFeedbackCategory category,
		ThesisFeedbackSeverity severity,
		UUID followsUpId,
		boolean deferredToSupervisor
) {
	/**
	 * A draft for a new finding that follows up nothing.
	 *
	 * @param feedback the feedback text
	 * @param category the suggested category
	 * @param severity the suggested severity
	 */
	public AIFeedbackDraftDTO(String feedback, ThesisFeedbackCategory category, ThesisFeedbackSeverity severity) {
		this(feedback, category, severity, null, false);
	}
}
