package de.tum.cit.aet.thesis.feedback.controller.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload a research group lead submits to set or replace the group's AI review guidelines.
 * The raw text is preprocessed server-side into the fixed review categories.
 *
 * @param rawGuidelines the free-text (Markdown) guidelines authored by the lead
 */
public record UpdateGuidelinesPayload(
		@NotBlank
		@Size(max = 50_000)
		String rawGuidelines
) {
}
