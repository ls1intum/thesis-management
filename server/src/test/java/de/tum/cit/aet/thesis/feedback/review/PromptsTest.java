package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import org.junit.jupiter.api.Test;

public class PromptsTest {

	@Test
	void everyCategoryHasATaskPromptForBothReviewTypes() {
		for (ReviewCategory category : ReviewCategory.values()) {
			// isNotBlank() already fails on null, so it covers the "variant missing" case too.
			assertThat(Prompts.taskPromptFor(category, ReviewType.PROPOSAL))
					.as("Proposal task prompt for %s", category).isNotBlank();
			assertThat(Prompts.taskPromptFor(category, ReviewType.THESIS))
					.as("Thesis task prompt for %s", category).isNotBlank();
		}
	}

	@Test
	void everyCategoryGetsItsOwnTaskPrompt() {
		// A copy-paste slip in the category-to-prompt switch would hand two categories the same
		// instructions, which no other test would notice.
		for (ReviewCategory category : ReviewCategory.values()) {
			for (ReviewCategory other : ReviewCategory.values()) {
				if (category != other) {
					assertThat(Prompts.taskPromptFor(category, ReviewType.PROPOSAL))
							.as("%s and %s share a proposal prompt", category, other)
							.isNotEqualTo(Prompts.taskPromptFor(other, ReviewType.PROPOSAL));
				}
			}
		}
	}

	@Test
	void everyPromptHasBothVariants() {
		for (Prompts prompt : Prompts.values()) {
			assertThat(prompt.getPrompt(ReviewType.PROPOSAL)).as("Proposal variant for %s", prompt).isNotBlank();
			assertThat(prompt.getPrompt(ReviewType.THESIS)).as("Thesis variant for %s", prompt).isNotBlank();
		}
	}
}
