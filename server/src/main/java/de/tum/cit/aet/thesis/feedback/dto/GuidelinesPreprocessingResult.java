package de.tum.cit.aet.thesis.feedback.dto;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;

import java.util.List;

/**
 * Structured output returned by the LLM when preprocessing a research group's raw guidelines.
 *
 * <p>The model first judges whether the raw guidelines are specific and actionable enough to
 * drive an automated review ({@link #specific}). Only when they are does it distill them into
 * concrete, per-category rules ({@link #categories}) plus a short {@link #overview}. When they
 * are not, {@link #reason} explains what is missing so the lead can improve the input.
 *
 * @param specific   whether the raw guidelines are specific enough to be usable
 * @param reason     when not specific, a short explanation of what is too vague or missing
 * @param overview   a short, category-independent summary of the group's expectations
 * @param categories the distilled rules per review category (only meaningful when specific)
 */
public record GuidelinesPreprocessingResult(
		boolean specific,
		String reason,
		String overview,
		List<CategoryGuidelines> categories
) {
}
