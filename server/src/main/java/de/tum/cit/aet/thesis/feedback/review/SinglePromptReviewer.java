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

import java.util.ArrayList;
import java.util.List;

/**
 * The naive {@link ThesisReviewer}: one LLM call carrying every criterion and the whole document,
 * returning the consolidated review directly.
 *
 * <p>It exists as the baseline the other strategies are measured against, so it differs from
 * {@link CategoryFanOutReviewer} in orchestration only. The system prompt is assembled from the
 * very same parts — the shared reviewer instructions, every category's task prompt, and the group's
 * rules for every category — and the output instructions repeat the merge step's ranking, limit,
 * assessment and score rules. Whatever separates the two in a measurement is then the decomposition
 * itself, not a difference in what the model was told to check.
 *
 * <p>The trade it represents: one call, one stage of depth, and the document sent once, against
 * nine heterogeneous checks competing for the attention of a single long context. Nothing is
 * verified and nothing is merged, because there is no intermediate result to verify or merge.
 *
 * <p>Selected by {@code thesis-management.ai.reviewer=single-prompt}; {@link CategoryFanOutReviewer}
 * is the default.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
@ConditionalOnProperty(name = "thesis-management.ai.reviewer", havingValue = "single-prompt")
public class SinglePromptReviewer implements ThesisReviewer {
	private static final Logger log = LoggerFactory.getLogger(SinglePromptReviewer.class);

	/** Progress step id for the one call this strategy makes. */
	private static final String STEP_ID = "review";

	/** Same fixed text as {@link CategoryFanOutReviewer}, for the same reason: provider errors stay in the log. */
	private static final String STEP_FAILURE_MESSAGE = "This step could not be completed";

	private final PdfService pdfService;
	private final ChatClient chatClient;
	private final ReviewMetricsRecorder metricsRecorder;
	private final boolean includeImages;

	/**
	 * Creates the reviewer and builds the underlying {@link ChatClient}.
	 *
	 * @param pdfService            service used to extract text and page images from the PDF
	 * @param chatClientBuilder     Spring AI builder used to construct the chat client
	 * @param metricsRecorder       where each finished run's measured cost goes
	 * @param includeImagesOverride when set, forces images on ({@code true}) or off ({@code false});
	 *                              when unset, capability is inferred from the configured chat model
	 * @param chatModel             configured chat model name, used to auto-detect vision support
	 */
	public SinglePromptReviewer(PdfService pdfService, ChatClient.Builder chatClientBuilder,
			ReviewMetricsRecorder metricsRecorder,
			@Value("${thesis-management.ai.review-include-images:#{null}}") Boolean includeImagesOverride,
			@Value("${spring.ai.openai.chat.model:}") String chatModel) {
		this.pdfService = pdfService;
		this.chatClient = chatClientBuilder.build();
		this.metricsRecorder = metricsRecorder;
		this.includeImages = includeImagesOverride != null
				? includeImagesOverride
				: VisionModels.supportsVision(chatModel);
		log.info("AI review runs the single-prompt strategy (images {}, model: {})",
				this.includeImages ? "enabled" : "disabled", chatModel);
	}

	@Override
	public ReviewResult review(ReviewRequest request) {
		List<String> pages = pdfService.extractTextFromPdf(request.document());
		List<Media> images = includeImages ? pdfService.extractImagesFromPdf(request.document()) : List.of();
		ReviewMeter meter = new ReviewMeter();
		ProgressReporter progress = request.progress();

		progress.stepStarted(STEP_ID, "Reviewing the document", 1, 1);
		ReviewResult result;
		try {
			ResponseEntity<ChatResponse, ReviewResult> response = chatClient.prompt()
					.system(systemMessage -> systemMessage.text(systemPrompt(request)))
					.user(userMessage -> userMessage
							.text(CategoryReviewer.fencedPages(pages))
							.media(images.toArray(new Media[0])))
					.call()
					.responseEntity(LenientOutputConverter.forType(ReviewResult.class));
			meter.recordCall(response.getResponse());
			result = response.getEntity();
		} catch (RuntimeException e) {
			// One call is the whole review, so unlike the fan-out there is nothing left to keep.
			log.warn("AI review failed in the single-prompt strategy ({})", request.type(), e);
			progress.stepFailed(STEP_ID, 1, 1, STEP_FAILURE_MESSAGE);
			throw e;
		}
		progress.stepCompleted(STEP_ID, 1, 1);
		meter.addDepth(1);

		// Every category's rules were in the prompt, so every category counts as checked; whether
		// the model actually attended to all of them is what the evaluation measures, not something
		// this strategy can observe.
		metricsRecorder.record(meter.snapshot(strategy(), ReviewCategory.values().length,
				ReviewCategory.values().length));
		return result;
	}

	/**
	 * The one system prompt: the upload boundary, the shared reviewer instructions, every category's
	 * task in {@link ReviewCategory} order, every category's rules, and the output instructions.
	 */
	String systemPrompt(ReviewRequest request) {
		ReviewType type = request.type();
		List<String> parts = new ArrayList<>();
		parts.add(CategoryReviewer.SECURITY_PROMPT);
		parts.add(Prompts.SHARED.getPrompt(type));
		parts.add("You perform ALL of the following " + ReviewCategory.values().length
				+ " checks in this single pass. Each check lists the rules it covers; apply every check to the whole document.");
		for (ReviewCategory category : ReviewCategory.values()) {
			parts.add(Prompts.taskPromptFor(category, type));
		}
		parts.add(GuidelinesPrompt.forAllCategories(request.guidelines()));
		parts.add(Prompts.SINGLE_PROMPT.getPrompt(type));
		return String.join("\n\n", parts);
	}
}
