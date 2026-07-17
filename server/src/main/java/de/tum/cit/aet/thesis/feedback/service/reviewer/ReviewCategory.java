package de.tum.cit.aet.thesis.feedback.service.reviewer;

/**
 * Enumerates the review dimensions exercised by the AI feedback pipeline. Each value pairs a
 * URL/JSON-friendly slug with the {@link Prompts} task prompt that drives the LLM for that
 * category. The concrete prompt text is chosen at call time based on the {@link ReviewType}
 * (proposal versus final thesis).
 */
public enum ReviewCategory {
	STRUCTURE("structure", Prompts.STRUCTURE),
	PROBLEM_MOTIVATION_OBJECTIVES("problem-motivation-objectives", Prompts.PROBLEM_MOTIVATION_OBJECTIVES),
	BIBLIOGRAPHY("bibliography", Prompts.BIBLIOGRAPHY),
	FIGURES("figures", Prompts.FIGURES),
	WRITING_STYLE("writing-style", Prompts.WRITING_STYLE),
	WRITING_STRUCTURE("writing-structure", Prompts.WRITING_STRUCTURE),
	WRITING_FORMATTING("writing-formatting", Prompts.WRITING_FORMATTING),
	AI_TRANSPARENCY("ai-transparency", Prompts.AI_TRANSPARENCY),
	SCHEDULE("schedule", Prompts.SCHEDULE);

	private final String slug;
	private final Prompts prompt;

	ReviewCategory(String slug, Prompts prompt) {
		this.slug = slug;
		this.prompt = prompt;
	}

	/**
	 * Returns the URL/JSON-friendly slug for this category.
	 *
	 * @return the slug used to key this category in API payloads
	 */
	public String getSlug() {
		return slug;
	}

	/**
	 * Returns the task prompt text used to drive the LLM for this category and review type.
	 *
	 * @param type whether the review targets a proposal or a thesis
	 * @return the resolved prompt text
	 */
	public String getPrompt(ReviewType type) {
		return prompt.getPrompt(type);
	}

	/**
	 * Resolves a category from its slug.
	 *
	 * @param slug slug to look up
	 * @return the matching category
	 * @throws IllegalArgumentException if no category has the given slug
	 */
	public static ReviewCategory fromSlug(String slug) {
		for (ReviewCategory category : values()) {
			if (category.slug.equals(slug)) {
				return category;
			}
		}
		throw new IllegalArgumentException("Unknown category: " + slug);
	}
}
