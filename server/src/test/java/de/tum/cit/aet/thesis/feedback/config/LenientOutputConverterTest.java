package de.tum.cit.aet.thesis.feedback.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.util.List;

class LenientOutputConverterTest {

	/** A stand-in for the records the reviewers ask the model to fill in. */
	record Answer(String title, String description, List<String> tags) {}

	@Test
	void parsesAStringValueContainingARawLineBreak() {
		// The exact shape that used to abort a whole review: Jackson's strict parser rejects the
		// line break with "Illegal unquoted character ((CTRL-CHAR, code 10))".
		Answer answer = LenientOutputConverter.forType(Answer.class).convert("""
				{"title": "Run-on sentence", "description": "The sentence runs on.
				Split it in two."}""");

		assertThat(answer.title()).isEqualTo("Run-on sentence");
		assertThat(answer.description()).isEqualTo("The sentence runs on.\nSplit it in two.");
	}

	@Test
	void parsesATabInsideAStringValue() {
		Answer answer = LenientOutputConverter.forType(Answer.class).convert("{\"title\": \"a\tb\"}");

		assertThat(answer.title()).isEqualTo("a\tb");
	}

	@Test
	void parsesTrailingCommasSingleQuotesAndComments() {
		Answer answer = LenientOutputConverter.forType(Answer.class).convert("""
				{
				// the model explains itself
				'title': 'Missing abstract',
				"tags": ["structure", "completeness",],
				}""");

		assertThat(answer.title()).isEqualTo("Missing abstract");
		assertThat(answer.tags()).containsExactly("structure", "completeness");
	}

	@Test
	void stripsMarkdownFencesLikeTheDefaultConverterDoes() {
		Answer answer = LenientOutputConverter.forType(Answer.class).convert("""
				```json
				{"title": "Fenced"}
				```""");

		assertThat(answer.title()).isEqualTo("Fenced");
	}

	@Test
	void ignoresPropertiesTheTargetTypeDoesNotHave() {
		Answer answer = LenientOutputConverter.forType(Answer.class)
				.convert("{\"title\": \"Known\", \"confidence\": 0.9}");

		assertThat(answer.title()).isEqualTo("Known");
	}

	@Test
	void stillRejectsOutputThatIsNotJsonAtAll() {
		// Leniency is about punctuation, not about inventing an answer: output with no JSON in it
		// still fails, and the caller still treats that call as failed.
		assertThatThrownBy(() -> LenientOutputConverter.forType(Answer.class)
				.convert("I was unable to review this document."))
				.isInstanceOf(JacksonException.class);
	}

	@Test
	void reusesOneConverterPerTargetType() {
		assertThat(LenientOutputConverter.forType(Answer.class))
				.isSameAs(LenientOutputConverter.forType(Answer.class));
	}
}
