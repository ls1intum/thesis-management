package de.tum.cit.aet.thesis.feedback.config;

import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAutoConfigurationUtil;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Carries the configured OpenAI timeout through to the actual HTTP call.
 *
 * <p>Since Spring AI 2.0.1, {@code OpenAiChatModel.buildRequestOptions} derives a per-request
 * timeout from the prompt's {@link OpenAiChatOptions} and hands it to the OpenAI SDK, which
 * re-applies it to the connect, read, write, and call timeouts of the OkHttp client for that one
 * call. That per-call value overrides whatever the shared client was built with, so
 * {@code spring.ai.openai.timeout} no longer has the last word: it still configures the shared
 * client, but every request then overwrites it.
 *
 * <p>Nothing populates that option from configuration — {@code OpenAiChatProperties.toOptions()}
 * never sets {@code timeout}, so {@code AbstractOpenAiOptions} falls back to its hard-coded 60s
 * default and no {@code spring.ai.openai.*} property can change it. A full thesis review needs
 * minutes, so the merge step was cancelled mid-generation ({@code IOException: Canceled}, logged
 * upstream as "client disconnected") and then retried three more times to no avail.
 *
 * <p>Setting the option as a builder default fixes it: the value is merged into the prompt options
 * of every call, is non-null, and therefore wins over the 60s default. Once Spring AI populates the
 * option from configuration itself, this class can go.
 */
@Configuration
@Conditional(AIFeaturesEnabled.class)
public class ChatClientTimeoutConfig {

	/**
	 * Applies the timeout to every {@code ChatClient} built from the autoconfigured builder.
	 *
	 * <p>Resolves it the same way the autoconfiguration resolves the timeout for the HTTP client
	 * itself, so the per-request budget always matches the client's and honours both
	 * {@code spring.ai.openai.timeout} and {@code spring.ai.openai.chat.timeout}.
	 *
	 * @param commonProperties the connection-level OpenAI properties
	 * @param chatProperties the chat-model-level OpenAI properties, which take precedence
	 * @return a customizer that puts the resolved timeout on the default chat options
	 */
	@Bean
	public ChatClientBuilderCustomizer openAiTimeoutCustomizer(OpenAiCommonProperties commonProperties,
			OpenAiChatProperties chatProperties) {
		Duration timeout = OpenAiAutoConfigurationUtil.resolveCommonProperties(commonProperties, chatProperties)
				.getTimeout();

		return builder -> builder.defaultOptions(OpenAiChatOptions.builder().timeout(timeout));
	}
}
