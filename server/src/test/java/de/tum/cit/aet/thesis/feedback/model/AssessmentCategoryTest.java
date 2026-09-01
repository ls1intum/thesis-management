package de.tum.cit.aet.thesis.feedback.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Guards the JSON contract between the merger prompt and {@link AssessmentCategory}. The merger
 * prompt instructs the model to emit {@code "good"} / {@code "acceptable"} / {@code "needs-work"};
 * these must deserialize into the enum, otherwise both AI endpoints would fail after every LLM
 * call has already completed.
 */
class AssessmentCategoryTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void deserializesRealMergerTokensIntoReviewResult() {
		// "category" was the field name before it was renamed to "assessment"; @JsonAlias keeps
		// responses from an older prompt or a cached model deployment readable.
		String mergerJson = """
				{
					"category": "needs-work",
					"summary": "The proposal needs significant work before submission.",
					"findings": []
				}
				""";

		ReviewResult result = objectMapper.readValue(mergerJson, ReviewResult.class);

		assertThat(result.assessment()).isEqualTo(AssessmentCategory.NEEDS_WORK);
		assertThat(result.summary()).isEqualTo("The proposal needs significant work before submission.");
	}

	@Test
	void acceptsEveryMergerAssessmentToken() {
		assertThat(objectMapper.readValue("\"good\"", AssessmentCategory.class))
				.isEqualTo(AssessmentCategory.GOOD);
		assertThat(objectMapper.readValue("\"acceptable\"", AssessmentCategory.class))
				.isEqualTo(AssessmentCategory.ACCEPTABLE);
		assertThat(objectMapper.readValue("\"needs-work\"", AssessmentCategory.class))
				.isEqualTo(AssessmentCategory.NEEDS_WORK);
	}

	@Test
	void alsoAcceptsEnumNamesFromSchemaDrivenResponses() {
		assertThat(objectMapper.readValue("\"GOOD\"", AssessmentCategory.class))
				.isEqualTo(AssessmentCategory.GOOD);
		assertThat(objectMapper.readValue("\"NEEDS_WORK\"", AssessmentCategory.class))
				.isEqualTo(AssessmentCategory.NEEDS_WORK);
	}

	@Test
	void serializesBackToTheEnumNameForTheClient() {
		assertThat(objectMapper.writeValueAsString(AssessmentCategory.NEEDS_WORK))
				.isEqualTo("\"NEEDS_WORK\"");
	}

	@Test
	void unknownTokenResolvesToNullInsteadOfFailingTheReview() {
		assertThat(objectMapper.readValue("\"maybe-later\"", AssessmentCategory.class)).isNull();
	}
}
