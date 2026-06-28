package de.tum.cit.aet.thesis.feedback.service.reviewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PromptsTest {
	@Test
	void testFromSlugResolvesPrompt() {
		assertEquals(Prompts.STRUCTURE.getPrompt(), ReviewCategory.fromSlug("structure").getPrompt());
		assertEquals(Prompts.PROBLEM_MOTIVATION_OBJECTIVES.getPrompt(), ReviewCategory.fromSlug("problem-motivation-objectives").getPrompt());
		assertEquals(Prompts.BIBLIOGRAPHY.getPrompt(), ReviewCategory.fromSlug("bibliography").getPrompt());
		assertEquals(Prompts.FIGURES.getPrompt(), ReviewCategory.fromSlug("figures").getPrompt());
		assertEquals(Prompts.WRITING_STYLE.getPrompt(), ReviewCategory.fromSlug("writing-style").getPrompt());
		assertEquals(Prompts.WRITING_STRUCTURE.getPrompt(), ReviewCategory.fromSlug("writing-structure").getPrompt());
		assertEquals(Prompts.WRITING_FORMATTING.getPrompt(), ReviewCategory.fromSlug("writing-formatting").getPrompt());
		assertEquals(Prompts.AI_TRANSPARENCY.getPrompt(), ReviewCategory.fromSlug("ai-transparency").getPrompt());
		assertEquals(Prompts.SCHEDULE.getPrompt(), ReviewCategory.fromSlug("schedule").getPrompt());
		assertThrows(IllegalArgumentException.class, () -> ReviewCategory.fromSlug("invalid-prompt"));
	}
}
