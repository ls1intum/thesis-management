package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.IntermediateReviewResult;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.service.reviewer.LlmReviewer;
import de.tum.cit.aet.thesis.feedback.service.reviewer.Prompts;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates AI review of an uploaded thesis PDF: runs each {@link ReviewCategory} through
 * an {@link LlmReviewer} in parallel on a virtual-thread executor, then merges the intermediate
 * findings into a single result via a final LLM call.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class ReviewService {
	private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

	/** Fence tag wrapping the JSON-serialized intermediate findings in the merger user message. */
	static final String FINDINGS_FENCE_TAG = "intermediate-findings";

	/**
	 * Substrings identifying chat models known to accept image inputs. Matched case-insensitively
	 * against {@code spring.ai.openai.chat.model}. Extend as new vision-capable models are adopted.
	 * Covers proprietary (OpenAI, Anthropic, Google) and open-source (Gemma 3/4, Llama vision,
	 * Qwen VL, LLaVA, Pixtral, etc.) multimodal families.
	 */
	private static final Set<String> VISION_MODEL_KEYWORDS = Set.of(
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

	private final PdfService pdfService;
	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;
	private final boolean includeImages;
	/**
	 * Dedicated executor for concurrent per-category LLM calls. Virtual threads are ideal here
	 * because each category call is IO-bound (waiting on the remote LLM) and cheap to fan out —
	 * we do not want to starve the common ForkJoinPool with blocking network waits.
	 */
	private final ExecutorService reviewExecutor;

	/**
	 * Creates the service and builds the underlying {@link ChatClient}.
	 *
	 * @param pdfService             service used to extract text and page images from the PDF
	 * @param chatClientBuilder      Spring AI builder used to construct the chat client
	 * @param objectMapper           Spring-managed Jackson mapper used to serialize the
	 *                               intermediate findings as untrusted JSON for the merger step
	 * @param includeImagesOverride  when set, forces images on ({@code true}) or off ({@code false});
	 *                               when unset, capability is inferred from the configured chat model
	 * @param chatModel              configured chat model name, used to auto-detect vision support
	 */
	public ReviewService(PdfService pdfService, ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
			@Value("${thesis-management.ai.review-include-images:#{null}}") Boolean includeImagesOverride,
			@Value("${spring.ai.openai.chat.model:}") String chatModel) {
		this.pdfService = pdfService;
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = objectMapper;
		this.includeImages = includeImagesOverride != null ? includeImagesOverride : modelSupportsVision(chatModel);
		this.reviewExecutor = Executors.newVirtualThreadPerTaskExecutor();
		log.info("AI review image processing {} (model: {}, override: {})",
				this.includeImages ? "enabled" : "disabled", chatModel, includeImagesOverride);
	}

	/**
	 * Shuts down the dedicated review executor on bean destruction so its virtual threads do not
	 * outlive the application context and graceful shutdown stays predictable.
	 */
	@PreDestroy
	void shutdownReviewExecutor() {
		reviewExecutor.shutdown();
	}

	/**
	 * Runs the per-category review pipeline against a PDF loaded from a Spring {@link Resource}
	 * (i.e. an already-persisted thesis file), for either a proposal or a final thesis.
	 *
	 * @param pdfResource  PDF resource loaded from the thesis upload store
	 * @param reviewType   whether the document should be reviewed as a proposal or a thesis
	 * @param guidelines   the research group's structured guidelines that drive each category
	 * @return the merged review result containing the assessment, overall summary, and findings
	 */
	public ReviewResultDTO review(Resource pdfResource, ReviewType reviewType, StructuredGuidelines guidelines) {
		List<String> pagesText = pdfService.extractTextFromPdf(pdfResource);
		List<Media> pagesImages = includeImages ? pdfService.extractImagesFromPdf(pdfResource) : List.of();
		return review(pagesText, pagesImages, reviewType, guidelines);
	}

	private ReviewResultDTO review(List<String> pagesText, List<Media> pagesImages, ReviewType reviewType,
			StructuredGuidelines guidelines) {
		// Fan out one LLM call per category on virtual threads. Each category is independent and
		// IO-bound so this cuts wall-clock time from N * latency down to ~1 * latency.
		Map<String, CompletableFuture<IntermediateReviewResult>> futures = new LinkedHashMap<>();
		for (ReviewCategory category : ReviewCategory.values()) {
			String guidelinesPrompt = buildCategoryGuidelinesPrompt(guidelines, category);
			futures.put(category.getSlug(), CompletableFuture.supplyAsync(() -> {
				log.debug("Reviewing category: {} ({})", category.getSlug(), reviewType);
				LlmReviewer reviewer = createReviewer(category.getPrompt(reviewType), reviewType, guidelinesPrompt);
				IntermediateReviewResult intermediateResult = reviewer.review(pagesText, pagesImages);
				log.debug("Review result for category {}: {}", category.getSlug(), intermediateResult);
				return intermediateResult;
			}, reviewExecutor));
		}

		// Wait for all category futures. Use LinkedHashMap so the merge prompt has deterministic
		// ordering — the ChatClient sees categories in the order declared in ReviewCategory.
		Map<String, IntermediateReviewResult> reviewResults = new LinkedHashMap<>();
		for (Map.Entry<String, CompletableFuture<IntermediateReviewResult>> entry : futures.entrySet()) {
			try {
				reviewResults.put(entry.getKey(), entry.getValue().get());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Category review interrupted for " + entry.getKey(), e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Category review failed for " + entry.getKey(), e.getCause());
			}
		}

		String mergerSystemPrompt = Prompts.MERGER.getPrompt(reviewType);
		return chatClient.prompt().system(systemMessage -> systemMessage.text(mergerSystemPrompt))
				.user(userMessage -> userMessage.text(buildMergePrompt(reviewResults))).call().entity(ReviewResultDTO.class);
	}

	String buildMergePrompt(Map<String, IntermediateReviewResult> reviewResults) {
		// Serialize the intermediate findings as JSON inside a fenced tag so the merger LLM
		// can treat every string value (title, description, quote, ...) as untrusted data
		// rather than interpolating it as raw prompt text. The MERGER prompt repeats this
		// instruction explicitly.
		String json = objectMapper.writeValueAsString(reviewResults);
		return "<" + FINDINGS_FENCE_TAG + ">\n" + json + "\n</" + FINDINGS_FENCE_TAG + ">\n";
	}

	protected LlmReviewer createReviewer(String taskPrompt, ReviewType reviewType, String guidelinesPrompt) {
		return new LlmReviewer(Prompts.SHARED.getPrompt(reviewType), taskPrompt, guidelinesPrompt, chatClient);
	}

	/**
	 * Renders the research group's structured guidelines into the reference-guidelines section of
	 * the system prompt for a single category. Includes the category-independent overview plus the
	 * distilled rules that apply to this specific category, so each reviewer only sees the rules
	 * relevant to its check.
	 *
	 * @param guidelines the research group's structured guidelines
	 * @param category   the category being reviewed
	 * @return the guidelines prompt text for this category
	 */
	static String buildCategoryGuidelinesPrompt(StructuredGuidelines guidelines, ReviewCategory category) {
		StringBuilder sb = new StringBuilder("## Reference Guidelines\n\n");
		sb.append("The following are the official guidelines from the research group. ")
				.append("They are the authoritative rules for this review — apply them precisely ")
				.append("and keep your evaluation focused on the specific rules of your task above.\n");

		String overview = guidelines != null ? guidelines.overview() : null;
		if (overview != null && !overview.isBlank()) {
			sb.append("\n").append(overview.strip()).append("\n");
		}

		List<String> rules = guidelines != null ? guidelines.rulesForCategory(category.getSlug()) : List.of();
		sb.append("\n### Group rules for ").append(category.getDisplayName()).append("\n");
		if (rules.isEmpty()) {
			sb.append("The research group did not provide specific rules for this category. Apply only the task rules above.\n");
		} else {
			for (String rule : rules) {
				if (rule != null && !rule.isBlank()) {
					sb.append("- ").append(rule.strip()).append("\n");
				}
			}
		}
		return sb.toString();
	}

	private static boolean modelSupportsVision(String model) {
		if (model == null || model.isBlank()) {
			return false;
		}
		String lower = model.toLowerCase(Locale.ROOT);
		return VISION_MODEL_KEYWORDS.stream().anyMatch(lower::contains);
	}
}
