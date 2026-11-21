package de.tum.cit.aet.thesis.feedback.config;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.util.JacksonUtils;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structured-output converters that parse what an LLM actually emits rather than what RFC 8259
 * allows.
 *
 * <p>Spring AI's {@code ChatClient.entity(Class)} builds a {@link BeanOutputConverter} with a
 * strict Jackson mapper. Models reliably break that: the most common failure is a literal line
 * break inside a JSON string — an answer whose {@code "description"} spans several lines parses as
 * {@code Illegal unquoted character ((CTRL-CHAR, code 10))} and takes the whole call down, even
 * though the intended value is perfectly recoverable. Trailing commas, single-quoted strings, and
 * stray {@code //} comments are the same kind of defect.
 *
 * <p>So every structured-output call goes through a converter built on a mapper that accepts those
 * four deviations. Nothing else is loosened — unknown properties are still ignored exactly as
 * Spring AI's default mapper ignores them, and genuinely unparseable output still throws. This is
 * about not discarding a usable answer over punctuation, not about accepting anything.
 */
public final class LenientOutputConverter {

	/**
	 * Mirrors {@code BeanOutputConverter.getJsonMapper()} — the same modules and the same
	 * {@code FAIL_ON_UNKNOWN_PROPERTIES} setting — and adds the read features that make LLM output
	 * parse. Jackson mappers are immutable and thread-safe once built, so one instance serves every
	 * converter.
	 */
	private static final JsonMapper LENIENT_MAPPER = JsonMapper.builder()
			.addModules(JacksonUtils.instantiateAvailableModules())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
			.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
			.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
			.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
			.build();

	/**
	 * Converters by target type. Building one generates the type's JSON schema, which is pure
	 * reflection over an unchanging class, so the result is cached and shared.
	 */
	private static final Map<Class<?>, BeanOutputConverter<?>> CONVERTERS = new ConcurrentHashMap<>();

	private LenientOutputConverter() {
	}

	/**
	 * The lenient converter for a structured-output type.
	 *
	 * <p>Pass it to {@code ChatClient.CallResponseSpec.entity(StructuredOutputConverter)} in place
	 * of {@code entity(Class)}: the request is built identically — same schema, same format
	 * instructions — only the response is parsed leniently.
	 *
	 * @param type the type the model's answer is deserialized into
	 * @param <T>  that type
	 * @return a cached, thread-safe converter for {@code type}
	 */
	@SuppressWarnings("unchecked")
	public static <T> BeanOutputConverter<T> forType(Class<T> type) {
		return (BeanOutputConverter<T>) CONVERTERS.computeIfAbsent(type,
				key -> new BeanOutputConverter<>(key, LENIENT_MAPPER));
	}
}
