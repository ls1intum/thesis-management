package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;

import java.util.List;

/**
 * Response for the instructor-facing preview endpoint. The overall assessment and summary can
 * be surfaced above the feedback list; drafts are shown as editable entries the instructor can
 * accept, tweak, or discard before saving.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AIPreviewResponseDTO(
		AssessmentCategory assessment,
		Integer score,
		String summary,
		List<AIFeedbackDraftDTO> drafts
) {}
