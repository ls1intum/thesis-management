package de.tum.cit.aet.thesis.feedback.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Field;
import java.time.Duration;

/**
 * Guards the workaround in {@link ChatClientTimeoutConfig}. Spring AI derives the per-request
 * timeout from the chat options rather than from {@code spring.ai.openai.timeout}, and defaults
 * that option to 60s, so without the customizer every call is cancelled after a minute.
 */
class ChatClientTimeoutConfigTest {

	private ApplicationContextRunner runner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ToolCallingAutoConfiguration.class,
					OpenAiChatAutoConfiguration.class, ChatClientAutoConfiguration.class))
			.withPropertyValues(
					"spring.ai.openai.base-url=https://example.invalid/v1",
					"spring.ai.openai.api-key=test-placeholder",
					"spring.ai.openai.timeout=10m",
					"spring.ai.openai.chat.model=test-model");
	}

	@Test
	void appliesConfiguredTimeoutToEveryRequest() {
		runner()
			.withPropertyValues("thesis-management.ai.enabled=true")
			.withUserConfiguration(ChatClientTimeoutConfig.class)
			.run(context -> assertThat(defaultRequestTimeoutOf(context.getBean(ChatClient.Builder.class)))
				.isEqualTo(Duration.ofMinutes(10)));
	}

	@Test
	void prefersTheChatLevelTimeoutLikeTheHttpClientDoes() {
		runner()
			.withPropertyValues("thesis-management.ai.enabled=true", "spring.ai.openai.chat.timeout=3m")
			.withUserConfiguration(ChatClientTimeoutConfig.class)
			.run(context -> assertThat(defaultRequestTimeoutOf(context.getBean(ChatClient.Builder.class)))
				.isEqualTo(Duration.ofMinutes(3)));
	}

	@Test
	void isInactiveWhenAiFeaturesAreDisabled() {
		runner()
			.withPropertyValues("thesis-management.ai.enabled=false")
			.withUserConfiguration(ChatClientTimeoutConfig.class)
			.run(context -> assertThat(context).doesNotHaveBean("openAiTimeoutCustomizer"));
	}

	/**
	 * Documents the framework gap this configuration exists to close: the configured 10m reaches
	 * the shared HTTP client, but the chat options Spring AI derives each request's timeout from
	 * stay at 60s. If this ever reports 10m, Spring AI has fixed it and the workaround can go.
	 */
	@Test
	void springAiLeavesTheRequestTimeoutAtOneMinute() {
		runner().run(context -> {
			assertThat(context.getBean(OpenAiChatModel.class).getOptions().getTimeout())
				.isEqualTo(Duration.ofMinutes(1));
			assertThat(read(context.getBean(ChatClient.Builder.class), "defaultRequest", "optionsCustomizer"))
				.as("no default chat options, so the 60s model default is what each request carries")
				.isNull();
		});
	}

	/** Builds the default options the client puts on every prompt and returns their timeout. */
	private static Duration defaultRequestTimeoutOf(ChatClient.Builder builder) {
		Object customizer = read(builder, "defaultRequest", "optionsCustomizer");
		assertThat(customizer).as("default chat options builder").isNotNull();
		return ((OpenAiChatOptions) ((ChatOptions.Builder<?>) customizer).build()).getTimeout();
	}

	/** Reads a chain of (possibly inherited, possibly private) fields. */
	private static Object read(Object root, String... fieldNames) {
		Object current = root;
		for (String fieldName : fieldNames) {
			if (current == null) {
				return null;
			}
			current = readField(current, fieldName);
		}
		return current;
	}

	private static Object readField(Object target, String fieldName) {
		for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			try {
				Field field = c.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field.get(target);
			}
			catch (NoSuchFieldException e) {
				// declared further up the hierarchy
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException("Cannot read " + fieldName, e);
			}
		}
		throw new IllegalStateException("No field " + fieldName + " on " + target.getClass());
	}
}
