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
 * timeout from the chat options rather than from {@code spring.ai.openai.timeout}, and defaulted
 * that option to 60s up to 2.0.1, so without the customizer every call was cancelled after a minute.
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
	 * Documents the framework behaviour this configuration was written against. Spring AI 2.0.1
	 * defaulted the chat options' timeout to 60s, which overrode the configured 10m on every
	 * request; since 2.0.2 the option stays unset, so the shared HTTP client's timeout applies.
	 * This test now pins that unset default. If it fails again, Spring AI is populating the
	 * option itself and the workaround can go.
	 */
	@Test
	void springAiLeavesTheRequestTimeoutUnsetSoTheClientTimeoutApplies() {
		runner().run(context -> {
			assertThat(context.getBean(OpenAiChatModel.class).getOptions().getTimeout()).isNull();
			assertThat(read(context.getBean(ChatClient.Builder.class), "defaultRequest", "optionsCustomizer"))
				.as("no default chat options, so requests carry no per-call timeout of their own")
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
			} catch (NoSuchFieldException e) {
				// declared further up the hierarchy
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Cannot read " + fieldName, e);
			}
		}
		throw new IllegalStateException("No field " + fieldName + " on " + target.getClass());
	}
}
