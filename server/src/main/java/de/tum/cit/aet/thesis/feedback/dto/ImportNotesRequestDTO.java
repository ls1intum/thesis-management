package de.tum.cit.aet.thesis.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for the note import endpoint: the block of notes a supervisor wrote offline while
 * reading, to be split into individual feedback entries.
 *
 * <p>Like {@link ClassifyFeedbackRequestDTO}, the thesis id only authorizes the call and resolves
 * the research group's AI opt-in — the split reads the notes alone, which is why no review type is
 * required and why proposal, thesis, and presentation feedback all work through the same endpoint.
 */
public record ImportNotesRequestDTO(
		@NotNull UUID thesisId,
		@NotBlank String notes
) {}
