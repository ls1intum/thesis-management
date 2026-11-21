package de.tum.cit.aet.thesis.feedback.controller.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload a research group lead submits to manually refine the already-generated structured
 * guidelines, without re-running the LLM preprocessor. Lets the lead tweak, add, or remove
 * individual per-category rules (e.g. to reflect a new formal convention) after the automatic
 * distribution across categories has produced its first draft.
 *
 * @param overview   the (optionally edited) category-independent summary of the group's
 *                   expectations; may be blank to clear it
 * @param categories the edited per-category rules, keyed by review category slug
 */
public record UpdateStructuredGuidelinesPayload(
		@Size(max = 5_000)
		String overview,

		@NotNull
		@Size(max = 100)
		@Valid
		List<CategoryRules> categories
) {
	/**
	 * The edited rules for a single review category.
	 *
	 * @param category the review category slug the rules belong to
	 * @param rules    the concrete, actionable rules for this category (blank entries are dropped)
	 */
	public record CategoryRules(
			@NotBlank
			@Size(max = 100)
			String category,

			@Size(max = 200)
			List<@Size(max = 2_000) String> rules
	) {
	}
}
