package de.tum.cit.aet.thesis.feedback.eval;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What one evaluation sweep will do: the cross product of models and review strategies, each run
 * over every document for every seed.
 *
 * <p>Built from {@code --key=value} arguments, falling back to environment variables so a sweep
 * can be driven either from a Gradle invocation or from a shell script.
 *
 * @param models     chat models to evaluate, in the order they will run
 * @param approaches prompting techniques to evaluate, as {@link EvalApproach} ids
 * @param corpus     directory holding the documents to review
 * @param outputRoot directory the results tree is written under
 * @param seeds      how many seeded repetitions of each run
 * @param temperature sampling temperature, held constant across the sweep. Defaults to 1.0,
 *                    because reasoning models reject every other value ("Only the default (1)
 *                    value is supported") and a sweep must hold it constant across the ladder
 * @param baseUrl    OpenAI-compatible endpoint
 * @param apiKey     credential for that endpoint
 * @param anonymize  whether document ids in the results tree hide the corpus filenames
 * @param thinkingModels models whose chat template has a thinking switch that the endpoint turns off
 *                    by default (Qwen3 behind vLLM); each request to them carries
 *                    {@code chat_template_kwargs.enable_thinking=true}, so they reason before they
 *                    answer like the reasoning models they are compared with
 */
public record EvalPlan(
		List<String> models,
		List<String> approaches,
		Path corpus,
		Path outputRoot,
		int seeds,
		double temperature,
		String baseUrl,
		String apiKey,
		boolean anonymize,
		List<String> thinkingModels
) {

	/**
	 * Parses a plan from command-line arguments and the environment.
	 *
	 * @param args {@code --key=value} pairs
	 * @return the parsed plan
	 * @throws IllegalArgumentException if a required value is missing or an approach is unknown
	 */
	public static EvalPlan parse(String[] args) {
		Map<String, String> values = new LinkedHashMap<>();
		for (String arg : args) {
			if (!arg.startsWith("--") || !arg.contains("=")) {
				throw new IllegalArgumentException("Expected --key=value, got: " + arg);
			}
			int split = arg.indexOf('=');
			values.put(arg.substring(2, split), arg.substring(split + 1));
		}

		List<String> approaches = csv(resolve(values, "approaches", "EVAL_APPROACHES",
				String.join(",", EvalApproach.ids())));
		// Anything not listed is a typo worth failing on before a context is booted.
		approaches.forEach(EvalApproach::fromId);

		List<String> models = csv(require(values, "models", "EVAL_MODELS"));
		return new EvalPlan(
				models,
				approaches,
				Paths.get(require(values, "corpus", "EVAL_CORPUS")),
				Paths.get(resolve(values, "out", "EVAL_OUT", "eval-runs")),
				Integer.parseInt(resolve(values, "seeds", "EVAL_SEEDS", "1")),
				Double.parseDouble(resolve(values, "temperature", "EVAL_TEMPERATURE", "1.0")),
				require(values, "base-url", "OPENAI_BASE_URL"),
				require(values, "api-key", "OPENAI_API_KEY"),
				Boolean.parseBoolean(resolve(values, "anonymize", "EVAL_ANONYMIZE", "false")),
				optionalCsv(resolve(values, "thinking-models", "EVAL_THINKING_MODELS", "")));
	}

	/**
	 * Total number of reviews this plan will run, for progress reporting.
	 *
	 * @param documentCount how many documents the corpus holds
	 * @return the number of reviews
	 */
	public int totalRuns(int documentCount) {
		return models.size() * approaches.size() * documentCount * seeds;
	}

	/**
	 * A filesystem-safe form of a model name: {@code openai/gpt-oss-120b} names a directory, and a
	 * slash in it would silently nest one.
	 *
	 * @param model the model name
	 * @return the directory-safe form
	 */
	public static String slug(String model) {
		return model.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
	}

	private static List<String> optionalCsv(String value) {
		return value.isBlank() ? List.of() : csv(value);
	}

	private static List<String> csv(String value) {
		List<String> items = new ArrayList<>();
		for (String item : Arrays.asList(value.split(","))) {
			String trimmed = item.strip();
			if (!trimmed.isEmpty()) {
				items.add(trimmed);
			}
		}
		if (items.isEmpty()) {
			throw new IllegalArgumentException("Empty list where at least one value is required");
		}
		return List.copyOf(items);
	}

	private static String resolve(Map<String, String> values, String key, String env, String fallback) {
		String value = values.get(key);
		if (value == null || value.isBlank()) {
			value = System.getenv(env);
		}
		return value == null || value.isBlank() ? fallback : value;
	}

	private static String require(Map<String, String> values, String key, String env) {
		String value = resolve(values, key, env, null);
		if (value == null) {
			throw new IllegalArgumentException("Missing required --" + key + " (or " + env + ")");
		}
		return value;
	}
}
