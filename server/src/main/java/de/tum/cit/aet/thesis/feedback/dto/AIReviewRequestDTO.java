package de.tum.cit.aet.thesis.feedback.dto;

import de.tum.cit.aet.thesis.feedback.model.ReviewType;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for the auto and preview AI review endpoints. The thesis's already-uploaded
 * proposal (or thesis) PDF is looked up server-side — the client does not re-upload the file.
 */
public record AIReviewRequestDTO(
		@NotNull UUID thesisId,
		@NotNull ReviewType reviewType,
		/**
		 * Optional, client-generated id correlating this run's websocket progress events
		 * ({@code /user/queue/ai-review-progress/{jobId}}). Absent when the caller does not want
		 * live progress.
		 */
		UUID jobId
) {}
