package de.tum.cit.aet.thesis.feedback.service.reviewer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PromptsTest {
	@Test
	void testFromSlugResolvesProposalPrompt() {
		for (ReviewCategory category : ReviewCategory.values()) {
			assertEquals(
					Prompts.valueOf(category.name()).getPrompt(ReviewType.PROPOSAL),
					ReviewCategory.fromSlug(category.getSlug()).getPrompt(ReviewType.PROPOSAL));
		}
		assertThrows(IllegalArgumentException.class, () -> ReviewCategory.fromSlug("invalid-prompt"));
	}

	@Test
	void testFromSlugResolvesThesisPrompt() {
		for (ReviewCategory category : ReviewCategory.values()) {
			assertEquals(
					Prompts.valueOf(category.name()).getPrompt(ReviewType.THESIS),
					ReviewCategory.fromSlug(category.getSlug()).getPrompt(ReviewType.THESIS));
		}
	}

	@Test
	void everyPromptHasBothVariants() {
		for (Prompts prompt : Prompts.values()) {
			// isNotBlank() already fails on null, so it covers the "variant missing" case too.
			assertThat(prompt.getPrompt(ReviewType.PROPOSAL)).as("Proposal variant for %s", prompt).isNotBlank();
			assertThat(prompt.getPrompt(ReviewType.THESIS)).as("Thesis variant for %s", prompt).isNotBlank();
		}
	}
}
