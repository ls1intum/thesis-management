package de.tum.cit.aet.thesis.feedback.service.reviewer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
			assertNotNull(prompt.getPrompt(ReviewType.PROPOSAL), "Proposal variant missing for " + prompt);
			assertNotNull(prompt.getPrompt(ReviewType.THESIS), "Thesis variant missing for " + prompt);
			assertThat(prompt.getPrompt(ReviewType.PROPOSAL)).isNotBlank();
			assertThat(prompt.getPrompt(ReviewType.THESIS)).isNotBlank();
		}
	}
}
