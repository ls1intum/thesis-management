package de.tum.cit.aet.thesis.feedback.review;

import java.util.Locale;
import java.util.Set;

/**
 * Best-effort detection of whether the configured chat model accepts image input, so a reviewer can
 * decide on its own whether rendering PDF pages to images is worth the cost. Deployments that run a
 * model this list does not know about can force the answer with
 * {@code thesis-management.ai.review-include-images}.
 */
public final class VisionModels {

	/**
	 * Substrings identifying chat models known to accept image inputs. Matched case-insensitively
	 * against the configured model name. Extend as new vision-capable models are adopted. Covers
	 * proprietary (OpenAI, Anthropic, Google) and open-source (Gemma 3/4, Llama vision, Qwen VL,
	 * LLaVA, Pixtral, etc.) multimodal families.
	 */
	private static final Set<String> KEYWORDS = Set.of(
			// Any model whose name advertises vision explicitly
			"vision",
			// OpenAI
			"gpt-4o", "gpt-4.1", "gpt-4-turbo", "gpt-5",
			// Anthropic (Claude 3+ are all multimodal)
			"claude-3", "claude-4", "claude-opus", "claude-sonnet", "claude-haiku",
			// Google
			"gemini-1.5", "gemini-2", "gemma-3", "gemma-4",
			// Meta
			"llama-4",
			// Alibaba
			"qwen-vl", "qwen2-vl", "qwen2.5-vl",
			// Mistral
			"pixtral",
			// Other open-source multimodal models
			"llava", "internvl", "minicpm-v", "molmo", "idefics"
	);

	private VisionModels() {
	}

	/**
	 * Whether the named model is known to accept image input.
	 *
	 * @param model the configured chat model name; may be {@code null} or blank
	 * @return {@code true} when the name matches a known multimodal family
	 */
	public static boolean supportsVision(String model) {
		if (model == null || model.isBlank()) {
			return false;
		}
		String lower = model.toLowerCase(Locale.ROOT);
		return KEYWORDS.stream().anyMatch(lower::contains);
	}
}
