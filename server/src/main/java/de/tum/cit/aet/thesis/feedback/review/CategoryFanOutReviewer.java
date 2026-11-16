package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.config.LenientOutputConverter;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The default {@link ThesisReviewer}: a fixed fan-out with a merge step. Every
 * {@link ReviewCategory} gets its own LLM call, all of them run concurrently, and a final call
 * consolidates the per-category findings into one ranked, deduplicated result. Control flow is
 * entirely in code — the model decides what to report, never what to do next.
 *
 * <p>Each category's findings are checked against that category's own rules before the merge, by
 * {@link FindingVerifier} — the structure hands every category a slot in the output, and a category
 * with nothing to report tends to fill it anyway. Disable with
 * {@code thesis-management.ai.verify-findings=false}.
 *
 * <p>The fan-out is fault-tolerant: a category whose call fails is dropped and the merge runs on
 * the categories that answered, so one flaky LLM call costs a slice of the review rather than all
 * of it. Only a run in which every category failed aborts. A run that lost categories is reported
 * as incomplete by {@link IncompleteCoverage}, so reduced coverage is never passed off as a clean
 * result.
 *
 * <p>Selected by {@code thesis-management.ai.reviewer=category-fan-out}, which is also the default;
 * {@link AgenticReviewer} is the model-driven alternative and {@link SinglePromptReviewer} the
 * one-call baseline. A further strategy replaces this bean by
 * implementing {@link ThesisReviewer} and declaring its own value for that property.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
@ConditionalOnProperty(name = "thesis-management.ai.reviewer", havingValue = "category-fan-out", matchIfMissing = true)
public class CategoryFanOutReviewer implements ThesisReviewer {
	private static final Logger log = LoggerFactory.getLogger(CategoryFanOutReviewer.class);

	/** Fence tag wrapping the JSON-serialized per-category findings in the merger user message. */
	static final String FINDINGS_FENCE_TAG = "intermediate-findings";

	/** Progress step id for the merge call, reported after every category has been reviewed. */
	private static final String MERGE_STEP_ID = "merge";

	/**
	 * What a failed step reports to the client. Deliberately fixed: the underlying exception comes
	 * from the LLM provider and its message can carry endpoint, model, or request details that have
	 * no place on a student's screen. The exception itself is logged server-side with its category
	 * or merge context instead.
	 */
	private static final String STEP_FAILURE_MESSAGE = "This step could not be completed";

	private final PdfService pdfService;
	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;
	private final ReviewMetricsRecorder metricsRecorder;
	private final boolean includeImages;
	private final boolean verifyFindings;

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
	 * @param metricsRecorder       where each finished run's measured cost goes
	 * @param verifyFindings        whether each category's findings are checked against its own
	 *                              rules before the merge; see {@link FindingVerifier}
	 * @param includeImagesOverride when set, forces images on ({@code true}) or off ({@code false});
	 *                              when unset, capability is inferred from the configured chat model
	 * @param chatModel             configured chat model name, used to auto-detect vision support
	 */
	public CategoryFanOutReviewer(PdfService pdfService, ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
			ReviewMetricsRecorder metricsRecorder,
			@Value("${thesis-management.ai.verify-findings:true}") boolean verifyFindings,
			@Value("${thesis-management.ai.review-include-images:#{null}}") Boolean includeImagesOverride,
			@Value("${spring.ai.openai.chat.model:}") String chatModel) {
		this.pdfService = pdfService;
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = objectMapper;
		this.metricsRecorder = metricsRecorder;
		this.verifyFindings = verifyFindings;
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
		ReviewMeter meter = new ReviewMeter();

		CategoryOutcomes outcomes = reviewEachCategory(request, pages, images, meter);
		ReviewResult merged = merge(request.type(), outcomes.findings(), request.progress(), meter);
		// A constant number of sequential stages however many categories there are: the fan-out is
		// concurrent, each category verifies its own findings inside that same concurrent task, and
		// the merge waits for all of them. Two stages, or three with verification on — still a
		// constant, which is the point of comparison with AgenticReviewer, whose depth is one stage
		// per model turn.
		meter.addDepth(verifyFindings ? 3 : 2);

		metricsRecorder.record(meter.snapshot(strategy(),
				ReviewCategory.values().length - outcomes.failed().size(), ReviewCategory.values().length));

		return IncompleteCoverage.qualify(merged, outcomes.failed());
	}

	/**
	 * Fans one LLM call out per category on virtual threads. Each category is independent and
	 * IO-bound, so this cuts wall-clock time from N * latency down to roughly one latency.
	 *
	 * @return what the categories that succeeded found, plus the ones that did not
	 */
	private CategoryOutcomes reviewEachCategory(ReviewRequest request, List<String> pages,
			List<Media> images, ReviewMeter meter) {
		ProgressReporter progress = request.progress();
		int total = ReviewCategory.values().length + 1;

		Map<ReviewCategory, CompletableFuture<CategoryFindings>> futures = new EnumMap<>(ReviewCategory.class);
		int index = 0;
		for (ReviewCategory category : ReviewCategory.values()) {
			index++;
			int stepIndex = index;
			String guidelinesPrompt = GuidelinesPrompt.forCategory(request.guidelines(), category);
			progress.stepStarted(category.getSlug(), category.getDisplayName(), stepIndex, total);
			futures.put(category, CompletableFuture.supplyAsync(() -> {
				log.debug("Reviewing category {} ({})", category.getSlug(), request.type());
				try {
					CategoryFindings generated = createReviewer(category, request.type(), guidelinesPrompt, meter)
							.review(pages, images);
					// Verified here, inside the category's own task, rather than as a stage of its
					// own after the fan-out: the checks run concurrently with the other categories'
					// reviews, so the whole pass costs one extra latency instead of one per category.
					CategoryCandidates candidates = verifyFindings
							? createVerifier(meter).check(category, request.type(), guidelinesPrompt, generated, pages)
							: CategoryCandidates.unverified(category, generated.findings());
					metricsRecorder.recordCandidates(candidates);
					progress.stepCompleted(category.getSlug(), stepIndex, total);
					return new CategoryFindings(candidates.kept());
				} catch (RuntimeException e) {
					log.warn("AI review failed for category {} ({})", category.getSlug(), request.type(), e);
					metricsRecorder.recordCategoryFailure(category, e);
					progress.stepFailed(category.getSlug(), stepIndex, total, STEP_FAILURE_MESSAGE);
					throw e;
				}
			}, reviewExecutor));
		}

		return collectSucceeded(futures);
	}

	/**
	 * Waits for every dispatched category and keeps the ones that came back.
	 *
	 * <p>A category that failed is dropped rather than failing the whole review. The categories are
	 * independent passes over the same document, so the ones that did answer still carry a useful
	 * review, and the student already sees the failed call marked as such in the live progress
	 * list. Only a run where every single category failed has nothing left to consolidate, and that
	 * one throws.
	 *
	 * <p>Every category is awaited even once one has failed: they were all dispatched up front and
	 * are already in flight, so there is nothing to save by abandoning them.
	 *
	 * @param futures the dispatched per-category calls, in {@link ReviewCategory} declaration order
	 * @return the findings of the successful categories, keyed by category slug and in that same
	 *         order, together with the categories that failed
	 */
	private CategoryOutcomes collectSucceeded(Map<ReviewCategory, CompletableFuture<CategoryFindings>> futures) {
		Map<String, CategoryFindings> results = new LinkedHashMap<>();
		List<ReviewCategory> failed = new ArrayList<>();
		Throwable firstFailure = null;

		for (Map.Entry<ReviewCategory, CompletableFuture<CategoryFindings>> entry : futures.entrySet()) {
			ReviewCategory category = entry.getKey();
			try {
				results.put(category.getSlug(), entry.getValue().get());
			} catch (InterruptedException e) {
				// The run itself is being cancelled — unlike a single category failing, there is no
				// point carrying on.
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Category review interrupted for " + category.getSlug(), e);
			} catch (ExecutionException e) {
				// The task already logged the cause and reported the step as failed.
				failed.add(category);
				if (firstFailure == null) {
					firstFailure = e.getCause();
				}
			}
		}

		if (results.isEmpty()) {
			throw new IllegalStateException("Category review failed for every category: " + slugsOf(failed),
					firstFailure);
		}
		if (!failed.isEmpty()) {
			log.warn("Continuing the review with {} of {} categories; {} failed and were dropped",
					results.size(), futures.size(), slugsOf(failed));
		}
		return new CategoryOutcomes(results, List.copyOf(failed));
	}

	private ReviewResult merge(ReviewType reviewType, Map<String, CategoryFindings> perCategory,
			ProgressReporter progress, ReviewMeter meter) {
		int total = ReviewCategory.values().length + 1;
		progress.stepStarted(MERGE_STEP_ID, "Consolidating findings", total, total);
		try {
			String mergerSystemPrompt = Prompts.MERGER.getPrompt(reviewType);
			ResponseEntity<ChatResponse, ReviewResult> response = chatClient.prompt()
					.system(systemMessage -> systemMessage.text(mergerSystemPrompt))
					.user(userMessage -> userMessage.text(buildMergePrompt(perCategory)))
					.call()
					.responseEntity(LenientOutputConverter.forType(ReviewResult.class));
			meter.recordCall(response.getResponse());
			progress.stepCompleted(MERGE_STEP_ID, total, total);
			return response.getEntity();
		} catch (RuntimeException e) {
			log.warn("AI review failed consolidating {} findings ({})", perCategory.size(), reviewType, e);
			progress.stepFailed(MERGE_STEP_ID, total, total, STEP_FAILURE_MESSAGE);
			throw e;
		}
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

	/** Overridable so tests can substitute the verification call. */
	protected FindingVerifier createVerifier(ReviewMeter meter) {
		return new FindingVerifier(chatClient, objectMapper, meter);
	}

	/** Overridable so tests can substitute the per-category LLM call. */
	protected CategoryReviewer createReviewer(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt,
			ReviewMeter meter) {
		return new CategoryReviewer(
				Prompts.SHARED.getPrompt(reviewType),
				Prompts.taskPromptFor(category, reviewType),
				guidelinesPrompt,
				chatClient,
				meter);
	}

	private static String slugsOf(List<ReviewCategory> categories) {
		return categories.stream().map(ReviewCategory::getSlug).collect(Collectors.joining(", "));
	}

	/**
	 * The fan-out's outcome: what each surviving category found, keyed by slug, and which
	 * categories failed outright.
	 *
	 * @param findings the findings of the categories that answered
	 * @param failed   the categories whose call failed; empty when every category answered
	 */
	private record CategoryOutcomes(Map<String, CategoryFindings> findings, List<ReviewCategory> failed) {
	}
}
