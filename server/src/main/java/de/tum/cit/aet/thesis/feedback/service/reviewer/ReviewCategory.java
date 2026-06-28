package de.tum.cit.aet.thesis.feedback.service.reviewer;

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

	public String getSlug() {
		return slug;
	}

	public String getPrompt() {
		return prompt.getPrompt();
	}

	public static ReviewCategory fromSlug(String slug) {
		for (ReviewCategory category : values()) {
			if (category.slug.equals(slug)) {
				return category;
			}
		}
		throw new IllegalArgumentException("Unknown category: " + slug);
	}
}
