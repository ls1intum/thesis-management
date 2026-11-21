package de.tum.cit.aet.thesis.feedback.entity.jsonb;

import java.util.List;

/**
 * The distilled, specific guideline rules that apply to a single fixed review category.
 *
 * @param category the slug of the {@code ReviewCategory} these rules belong to
 * @param rules    the concrete, actionable rules the AI reviewer must check for this category
 */
public record CategoryGuidelines(
		String category,
		List<String> rules
) {
	public CategoryGuidelines(String category, List<String> rules) {
		this.category = category;
		this.rules = rules == null ? List.of() : List.copyOf(rules);
	}
}
