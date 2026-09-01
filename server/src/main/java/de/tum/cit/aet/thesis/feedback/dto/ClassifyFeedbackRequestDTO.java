package de.tum.cit.aet.thesis.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for the feedback classification endpoint: a single feedback line an instructor
 * typed by hand and wants a category and severity suggestion for.
 *
 * <p>The thesis id only authorizes the call and resolves the research group's AI opt-in — the
 * classification itself reads the line alone, which is why no review type is required and why
 * proposal, thesis, and presentation feedback all work through the same endpoint.
 */
public record ClassifyFeedbackRequestDTO(
		@NotNull UUID thesisId,
		@NotBlank String feedback
) {}
