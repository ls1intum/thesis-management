package de.tum.cit.aet.thesis.feedback.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReviewCategoryTest {

	@Test
	void resolvesEveryCategoryFromItsSlug() {
		for (ReviewCategory category : ReviewCategory.values()) {
			assertThat(ReviewCategory.fromSlug(category.getSlug())).isSameAs(category);
		}
	}

	@Test
	void rejectsAnUnknownSlug() {
		assertThatThrownBy(() -> ReviewCategory.fromSlug("invalid-category"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void slugsCoverEveryCategory() {
		// Stored guidelines and client payloads are validated against SLUGS, so it must not drift
		// from the enum — a duplicated slug literal would silently shrink the set.
		assertThat(ReviewCategory.SLUGS).hasSize(ReviewCategory.values().length);
	}
}
