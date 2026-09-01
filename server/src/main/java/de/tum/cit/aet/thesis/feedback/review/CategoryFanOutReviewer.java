package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The default {@link ThesisReviewer}: a fixed fan-out with a merge step. Every
 * {@link ReviewCategory} gets its own LLM call, all of them run concurrently, and a final call
 * consolidates the per-category findings into one ranked, deduplicated result. Control flow is
 * entirely in code — the model decides what to report, never what to do next.
 *
 * <p>Selected by {@code thesis-management.ai.reviewer=category-fan-out}, which is also the default.
 * A different strategy replaces this bean by implementing {@link ThesisReviewer} and declaring its
 * own value for that property.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
@ConditionalOnProperty(name = "thesis-management.ai.reviewer", havingValue = "category-fan-out", matchIfMissing = true)
public class CategoryFanOutReviewer implements ThesisReviewer {
	private static final Logger log = LoggerFactory.getLogger(CategoryFanOutReviewer.class);

	/** Fence tag wrapping the JSON-serialized per-category findings in the merger user message. */
	static final String FINDINGS_FENCE_TAG = "intermediate-findings";

	private final PdfService pdfService;
	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;
	private final boolean includeImages;

	/**
	 * Dedicated executor for the concurrent per-category LLM calls. Virtual threads are ideal here
	 * because each call is IO-bound (waiting on the remote LLM) and cheap to fan out — we do not
	 * want to starve the common ForkJoinPool with blocking network waits.
	 */
	private final ExecutorService reviewExecutor = Executors.newVirtualThreadPerTaskExecutor();

	/**
	 * Creates the reviewer and builds the underlying {@link ChatClient}.
	 *
	 * @param pdfService            service used to extract text and page images from the PDF
	 * @param chatClientBuilder     Spring AI builder used to construct the chat client
	 * @param objectMapper          Spring-managed Jackson mapper used to serialize the per-category
	 *                              findings as untrusted JSON for the merge step
	 * @param includeImagesOverride when set, forces images on ({@code true}) or off ({@code false});
	 *                              when unset, capability is inferred from the configured chat model
	 * @param chatModel             configured chat model name, used to auto-detect vision support
	 */
	public CategoryFanOutReviewer(PdfService pdfService, ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
			@Value("${thesis-management.ai.review-include-images:#{null}}") Boolean includeImagesOverride,
			@Value("${spring.ai.openai.chat.model:}") String chatModel) {
		this.pdfService = pdfService;
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = objectMapper;
		this.includeImages = includeImagesOverride != null
				? includeImagesOverride
				: VisionModels.supportsVision(chatModel);
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

	@Override
	public ReviewResult review(ReviewRequest request) {
		List<String> pages = pdfService.extractTextFromPdf(request.document());
		List<Media> images = includeImages ? pdfService.extractImagesFromPdf(request.document()) : List.of();

		return merge(request.type(), reviewEachCategory(request, pages, images));
	}

	/**
	 * Fans one LLM call out per category on virtual threads. Each category is independent and
	 * IO-bound, so this cuts wall-clock time from N * latency down to roughly one latency.
	 *
	 * @return the findings keyed by category slug, in {@link ReviewCategory} declaration order so
	 *         the merge prompt is deterministic
	 */
	private Map<String, CategoryFindings> reviewEachCategory(ReviewRequest request, List<String> pages,
			List<Media> images) {
		Map<ReviewCategory, CompletableFuture<CategoryFindings>> futures = new EnumMap<>(ReviewCategory.class);
		for (ReviewCategory category : ReviewCategory.values()) {
			String guidelinesPrompt = GuidelinesPrompt.forCategory(request.guidelines(), category);
			futures.put(category, CompletableFuture.supplyAsync(() -> {
				log.debug("Reviewing category {} ({})", category.getSlug(), request.type());
				return createReviewer(category, request.type(), guidelinesPrompt).review(pages, images);
			}, reviewExecutor));
		}

		Map<String, CategoryFindings> results = new LinkedHashMap<>();
		futures.forEach((category, future) -> results.put(category.getSlug(), await(category, future)));
		return results;
	}

	private ReviewResult merge(ReviewType reviewType, Map<String, CategoryFindings> perCategory) {
		String mergerSystemPrompt = Prompts.MERGER.getPrompt(reviewType);
		return chatClient.prompt()
				.system(systemMessage -> systemMessage.text(mergerSystemPrompt))
				.user(userMessage -> userMessage.text(buildMergePrompt(perCategory)))
				.call()
				.entity(ReviewResult.class);
	}

	/**
	 * Serializes the per-category findings as JSON inside a fenced tag so the merger LLM treats
	 * every string value (title, description, quote, ...) as untrusted data rather than as raw
	 * prompt text. The MERGER prompt repeats this instruction explicitly.
	 */
	String buildMergePrompt(Map<String, CategoryFindings> perCategory) {
		String json = objectMapper.writeValueAsString(perCategory);
		return "<" + FINDINGS_FENCE_TAG + ">\n" + json + "\n</" + FINDINGS_FENCE_TAG + ">\n";
	}

	/** Overridable so tests can substitute the per-category LLM call. */
	protected CategoryReviewer createReviewer(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt) {
		return new CategoryReviewer(
				Prompts.SHARED.getPrompt(reviewType),
				Prompts.taskPromptFor(category, reviewType),
				guidelinesPrompt,
				chatClient);
	}

	private static CategoryFindings await(ReviewCategory category, CompletableFuture<CategoryFindings> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Category review interrupted for " + category.getSlug(), e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Category review failed for " + category.getSlug(), e.getCause());
		}
	}
}
