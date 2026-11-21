package de.tum.cit.aet.thesis.feedback.entity.jsonb;

import java.util.List;

/**
 * Structured representation of a research group's writing guidelines, produced by preprocessing
 * the lead's free-text upload into the fixed set of review categories. This is the form the AI
 * review pipeline consumes: each {@link CategoryGuidelines} feeds the reviewer for its category.
 *
 * @param overview   a short, category-independent summary of the group's expectations
 * @param categories the per-category distilled rules, one entry per relevant review category
 */
public record StructuredGuidelines(
		String overview,
		List<CategoryGuidelines> categories
) {
	public StructuredGuidelines(String overview, List<CategoryGuidelines> categories) {
		this.overview = overview;
		this.categories = categories == null ? List.of() : List.copyOf(categories);
	}

	/**
	 * Returns the distilled rules for the given category slug, or an empty list if the category
	 * has no specific rules.
	 *
	 * @param categorySlug the review category slug to look up
	 * @return the rules for that category, never {@code null}
	 */
	public List<String> rulesForCategory(String categorySlug) {
		return categories.stream()
				.filter(entry -> entry.category().equals(categorySlug))
				.findFirst()
				.map(CategoryGuidelines::rules)
				.orElseGet(List::of);
	}
}
