package de.tum.cit.aet.thesis.feedback.service.reviewer;

/**
 * Enumerates the review dimensions exercised by the AI feedback pipeline. Each value pairs a
 * URL/JSON-friendly slug with the {@link Prompts} task prompt that drives the LLM for that
 * category. The concrete prompt text is chosen at call time based on the {@link ReviewType}
 * (proposal versus final thesis).
 */
public enum ReviewCategory {
	STRUCTURE("structure", "Structure & Completeness",
			"Required sections, overall and per-section length limits, and the objectives structure.", Prompts.STRUCTURE),
	PROBLEM_MOTIVATION_OBJECTIVES("problem-motivation-objectives", "Problem, Motivation & Objectives",
			"Quality of the problem, motivation, and objectives sections (actors, no solutions in the problem, action-form objectives).", Prompts.PROBLEM_MOTIVATION_OBJECTIVES),
	BIBLIOGRAPHY("bibliography", "Bibliography & Citations",
			"Number and quality of references, peer-reviewed-only sources, citation style and placement.", Prompts.BIBLIOGRAPHY),
	FIGURES("figures", "Figures & Diagrams",
			"Number, type, readability, captions, referencing, and format of figures and diagrams.", Prompts.FIGURES),
	WRITING_STYLE("writing-style", "Writing Style",
			"Active voice, filler words and superlatives, contractions, forbidden sentence starters, abbreviations.", Prompts.WRITING_STYLE),
	WRITING_STRUCTURE("writing-structure", "Paragraph Structure",
			"Paragraph length, one idea per paragraph, prose over bullet points, subsection depth, text before subsections.", Prompts.WRITING_STRUCTURE),
	WRITING_FORMATTING("writing-formatting", "Formatting & Terminology",
			"Title-case headings and consistent terminology.", Prompts.WRITING_FORMATTING),
	AI_TRANSPARENCY("ai-transparency", "AI Transparency Statement",
			"Presence, first-person voice, specificity, tools/purposes, and review confirmation of the AI transparency statement.", Prompts.AI_TRANSPARENCY),
	SCHEDULE("schedule", "Schedule Quality",
			"Iteration length, measurable deliverables, vertically integrated features, agile principles.", Prompts.SCHEDULE);

	private final String slug;
	private final String displayName;
	private final String description;
	private final Prompts prompt;

	ReviewCategory(String slug, String displayName, String description, Prompts prompt) {
		this.slug = slug;
		this.displayName = displayName;
		this.description = description;
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
	 * Returns the human-readable name of this category, used in the UI and preprocessing prompts.
	 *
	 * @return the display name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns a short description of what this category covers, used to guide guideline
	 * preprocessing.
	 *
	 * @return the category description
	 */
	public String getDescription() {
		return description;
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
