package de.tum.cit.aet.thesis.feedback.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The review dimensions the AI feedback feature knows about. Each value pairs a URL/JSON-friendly
 * slug with a display name and a short description; the slugs key a research group's per-category
 * guidelines, so they are part of the stored data format and must stay stable.
 *
 * <p>Deliberately carries no prompt text: how a category is turned into instructions for a model
 * is a concern of the reviewer implementation (see {@code feedback.review.Prompts}), which lets a
 * different strategy — agentic, single-pass, human-in-the-loop — reuse these categories with its
 * own prompting.
 */
public enum ReviewCategory {
	STRUCTURE("structure", "Structure & Completeness",
			"Required sections, overall and per-section length limits, and the objectives structure."),
	PROBLEM_MOTIVATION_OBJECTIVES("problem-motivation-objectives", "Problem, Motivation & Objectives",
			"Quality of the problem, motivation, and objectives sections (actors, no solutions in the problem, action-form objectives)."),
	BIBLIOGRAPHY("bibliography", "Bibliography & Citations",
			"Number and quality of references, peer-reviewed-only sources, citation style and placement."),
	FIGURES("figures", "Figures & Diagrams",
			"Number, type, readability, captions, referencing, and format of figures and diagrams."),
	WRITING_STYLE("writing-style", "Writing Style",
			"Active voice, filler words and superlatives, contractions, forbidden sentence starters, abbreviations."),
	WRITING_STRUCTURE("writing-structure", "Paragraph Structure",
			"Paragraph length, one idea per paragraph, prose over bullet points, subsection depth, text before subsections."),
	WRITING_FORMATTING("writing-formatting", "Formatting & Terminology",
			"Title-case headings and consistent terminology."),
	AI_TRANSPARENCY("ai-transparency", "AI Transparency Statement",
			"Presence, first-person voice, specificity, tools/purposes, and review confirmation of the AI transparency statement."),
	SCHEDULE("schedule", "Schedule Quality",
			"Iteration length, measurable deliverables, vertically integrated features, agile principles.");

	/** Every known slug, for validating category keys that arrive from a client or a model. */
	public static final Set<String> SLUGS = Arrays.stream(values())
			.map(ReviewCategory::getSlug)
			.collect(Collectors.toUnmodifiableSet());

	private final String slug;
	private final String displayName;
	private final String description;

	ReviewCategory(String slug, String displayName, String description) {
		this.slug = slug;
		this.displayName = displayName;
		this.description = description;
	}

	/**
	 * Returns the URL/JSON-friendly slug for this category.
	 *
	 * @return the slug used to key this category in API payloads and stored guidelines
	 */
	public String getSlug() {
		return slug;
	}

	/**
	 * Returns the human-readable name of this category, used in the UI and in prompts.
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
