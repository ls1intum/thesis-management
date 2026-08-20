package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Response for the instructor-facing preview endpoint. The overall assessment and summary can
 * be surfaced above the feedback list; drafts are shown as editable entries the instructor can
 * accept, tweak, or discard before saving.
 *
 * <p>{@code previewToken} is an opaque, server-signed proof that this preview really ran. The
 * client echoes it back when saving so the server can stamp accepted drafts as
 * {@code AI_REVIEWED_BY_HUMAN}; without it a saved row is recorded as {@code HUMAN}.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AIPreviewResponseDTO(
		AssessmentCategory assessment,
		String summary,
		List<AIFeedbackDraftDTO> drafts,
		String previewToken
) {}
