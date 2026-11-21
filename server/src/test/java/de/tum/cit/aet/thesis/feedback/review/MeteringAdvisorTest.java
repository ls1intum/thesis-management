package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * Runs a {@link CategoryReviewer} through a real {@link ChatClient}, so the call is counted the way
 * production counts it rather than the way a mocked chain does.
 */
class MeteringAdvisorTest {

	/** Answers with the given text and reports 120 prompt and 30 completion tokens. */
	private static ChatModel answering(String text) {
		return new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				return new ChatResponse(List.of(new Generation(new AssistantMessage(text))),
						ChatResponseMetadata.builder().usage(new DefaultUsage(120, 30)).build());
			}
		};
	}

	private static CategoryReviewer reviewer(ChatModel model, ReviewMeter meter) {
		return new CategoryReviewer("Shared.", "Task.", "Rules.", ChatClient.builder(model).build(), meter);
	}

	@Test
	void aCallWhoseAnswerCannotBeParsedIsStillCounted() {
		// Truncated JSON: the provider billed the call, and conversion then fails.
		ReviewMeter meter = new ReviewMeter();

		assertThatThrownBy(() -> reviewer(answering("{\"findings\":["), meter).review(List.of("Page."), List.of()))
				.isInstanceOf(RuntimeException.class);

		ReviewRunMetrics metrics = meter.snapshot("test", 1, 1);
		assertThat(metrics.llmCalls()).isEqualTo(1);
		assertThat(metrics.promptTokens()).isEqualTo(120);
		assertThat(metrics.completionTokens()).isEqualTo(30);
	}

	@Test
	void aSuccessfulCallIsCountedOnce() {
		ReviewMeter meter = new ReviewMeter();

		reviewer(answering("{\"findings\": []}"), meter).review(List.of("Page."), List.of());

		assertThat(meter.llmCalls()).isEqualTo(1);
	}
}
