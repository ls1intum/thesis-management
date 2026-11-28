package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;

import java.util.List;
import java.util.UUID;

/**
 * Response for the instructor-facing preview endpoint. The overall assessment and summary can
 * be surfaced above the feedback list; drafts are shown as editable entries the instructor can
 * accept, tweak, or discard before saving.
 *
 * @param assessment          the overall verdict
 * @param score               the 0-100 score, or {@code null}
 * @param summary             the review summary
 * @param drafts              the editable drafts, follow-ups included
 * @param resolvedFeedbackIds completed earlier entries the review found resolved
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AIPreviewResponseDTO(
		AssessmentCategory assessment,
		Integer score,
		String summary,
		List<AIFeedbackDraftDTO> drafts,
		List<UUID> resolvedFeedbackIds
) {
	/**
	 * A preview without any comparison against earlier feedback.
	 *
	 * @param assessment the overall verdict
	 * @param score      the 0-100 score
	 * @param summary    the review summary
	 * @param drafts     the editable drafts
	 */
	public AIPreviewResponseDTO(AssessmentCategory assessment, Integer score, String summary,
			List<AIFeedbackDraftDTO> drafts) {
		this(assessment, score, summary, drafts, List.of());
	}
}
