package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * The strategy seam is a configuration switch, so the switch itself is worth a test: a typo in
 * either {@code @ConditionalOnProperty} value shows up as two reviewer beans (which fails
 * {@code AIFeedbackService}'s single-constructor injection) or as none at all, and neither failure
 * is visible until the application starts.
 */
class ReviewerSelectionTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(Stubs.class)
			.withBean(CategoryFanOutReviewer.class)
			.withBean(AgenticReviewer.class)
			.withBean(SinglePromptReviewer.class)
			.withPropertyValues(AIFeaturesEnabled.PROPERTY + "=true");

	@Test
	void theFanOutIsTheStrategyWhenNoneIsConfigured() {
		runner.run(context -> {
			assertThat(context).hasSingleBean(ThesisReviewer.class);
			assertThat(context.getBean(ThesisReviewer.class)).isInstanceOf(CategoryFanOutReviewer.class);
		});
	}

	@Test
	void theAgentIsSelectedByConfigurationAndReplacesTheFanOut() {
		runner.withPropertyValues("thesis-management.ai.reviewer=agentic").run(context -> {
			assertThat(context).hasSingleBean(ThesisReviewer.class);
			assertThat(context.getBean(ThesisReviewer.class)).isInstanceOf(AgenticReviewer.class);
		});
	}

	@Test
	void theSinglePromptIsSelectedByConfigurationAndReplacesTheFanOut() {
		runner.withPropertyValues("thesis-management.ai.reviewer=single-prompt").run(context -> {
			assertThat(context).hasSingleBean(ThesisReviewer.class);
			assertThat(context.getBean(ThesisReviewer.class)).isInstanceOf(SinglePromptReviewer.class);
		});
	}

	@Test
	void noReviewerIsCreatedAtAllWhenTheAiFeaturesAreOff() {
		new ApplicationContextRunner()
				.withUserConfiguration(Stubs.class)
				.withBean(CategoryFanOutReviewer.class)
				.withBean(AgenticReviewer.class)
				.withBean(SinglePromptReviewer.class)
				.run(context -> assertThat(context).doesNotHaveBean(ThesisReviewer.class));
	}

	/** The collaborators both reviewers need, stubbed: this test is about selection, not behaviour. */
	@Configuration(proxyBeanMethods = false)
	static class Stubs {

		@Bean
		PdfService pdfService() {
			return mock(PdfService.class);
		}

		@Bean
		ChatClient.Builder chatClientBuilder() {
			ChatClient.Builder builder = mock(ChatClient.Builder.class);
			org.mockito.Mockito.when(builder.build()).thenReturn(mock(ChatClient.class));
			return builder;
		}

		@Bean
		ToolCallingManager toolCallingManager() {
			return mock(ToolCallingManager.class);
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		ReviewMetricsRecorder reviewMetricsRecorder() {
			return metrics -> {
			};
		}
	}
}
