package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResearchGroupServiceTest {

	@Test
	void normalizeSearchQuery_returnsNullForBlankInput() {
		assertThat(ResearchGroupService.normalizeSearchQuery(null)).isNull();
		assertThat(ResearchGroupService.normalizeSearchQuery("")).isNull();
		assertThat(ResearchGroupService.normalizeSearchQuery("   ")).isNull();
	}

	@Test
	void normalizeSearchQuery_lowercases() {
		assertThat(ResearchGroupService.normalizeSearchQuery("FoO")).isEqualTo("foo");
	}

	@Test
	void normalizeSearchQuery_escapesLikeWildcards() {
		// Wildcards a user could submit must be escaped so they act literally
		// inside the repository's ILIKE ... ESCAPE '\' clause.
		assertThat(ResearchGroupService.normalizeSearchQuery("100%")).isEqualTo("100\\%");
		assertThat(ResearchGroupService.normalizeSearchQuery("foo_bar")).isEqualTo("foo\\_bar");
		assertThat(ResearchGroupService.normalizeSearchQuery("a\\b")).isEqualTo("a\\\\b");
	}

	@Test
	void normalizeSearchQuery_escapesBackslashBeforeOtherWildcards() {
		// A literal user backslash must not eat the subsequent escape — the
		// backslash must be doubled first.
		assertThat(ResearchGroupService.normalizeSearchQuery("\\%")).isEqualTo("\\\\\\%");
	}
}
