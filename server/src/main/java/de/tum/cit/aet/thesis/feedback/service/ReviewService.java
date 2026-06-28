package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.FindingDTO;
import de.tum.cit.aet.thesis.feedback.dto.IntermediateReviewResult;
import de.tum.cit.aet.thesis.feedback.dto.ReviewRequestDTO;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.service.reviewer.LlmReviewer;
import de.tum.cit.aet.thesis.feedback.service.reviewer.Prompts;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates AI review of an uploaded thesis PDF: runs each {@link ReviewCategory} through
 * an {@link LlmReviewer} and merges the intermediate findings into a single result via a final
 * LLM call.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class ReviewService {
	private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

	private final PdfService pdfService;
	private final ChatClient chatClient;

	/**
	 * Creates the service and builds the underlying {@link ChatClient}.
	 *
	 * @param pdfService          service used to extract text and page images from the PDF
	 * @param chatClientBuilder   Spring AI builder used to construct the chat client
	 */
	public ReviewService(PdfService pdfService, ChatClient.Builder chatClientBuilder) {
		this.pdfService = pdfService;
		this.chatClient = chatClientBuilder.build();
	}

	/**
	 * Runs the per-category review pipeline against the uploaded PDF and merges the findings.
	 *
	 * @param request review request carrying the uploaded file and provider category
	 * @return the merged review result containing the assessment, overall summary, and findings
	 */
	public ReviewResultDTO review(ReviewRequestDTO request) {
		List<String> pagesText = pdfService.extractTextFromPdf(request.file());
		List<Media> pagesImages = pdfService.extractImagesFromPdf(request.file());

		Map<String, IntermediateReviewResult> reviewResults = new HashMap<>();

		// TODO: Parallelize the review process for each category to improve performance
		for (ReviewCategory category : ReviewCategory.values()) {
			log.debug("Reviewing category: {}", category.getSlug());
			LlmReviewer reviewer = createReviewer(category.getPrompt());
			IntermediateReviewResult intermediateResult = reviewer.review(pagesText, pagesImages);
			log.debug("Review result for category {}: {}", category.getSlug(), intermediateResult);
			reviewResults.put(category.getSlug(), intermediateResult);
		}

		return chatClient.prompt().system(systemMessage -> systemMessage.text(Prompts.MERGER.getPrompt()))
				.user(userMessage -> userMessage.text(buildMergePrompt(reviewResults))).call().entity(ReviewResultDTO.class);
	}

	String buildMergePrompt(Map<String, IntermediateReviewResult> reviewResults) {
		StringBuilder builder = new StringBuilder();

		for (Map.Entry<String, IntermediateReviewResult> entry : reviewResults.entrySet()) {
			builder.append("# Category: ").append(entry.getKey()).append("\n");
			for (FindingDTO finding : entry.getValue().findings()) {
				builder.append("## [").append(finding.severity()).append("] ").append(finding.title()).append("\n");
				builder.append("Category: ").append(finding.category()).append("\n");
				builder.append("Description: ").append(finding.description()).append("\n");
				if (finding.locations() != null && !finding.locations().isEmpty()) {
					builder.append("Locations:\n");
					for (var loc : finding.locations()) {
						builder.append("  - Page ").append(loc.page());
						if (loc.section() != null) {
							builder.append(", Section: ").append(loc.section());
						}
						if (loc.quote() != null) {
							builder.append(", Quote: \"").append(loc.quote()).append("\"");
						}
						builder.append("\n");
					}
				}
				builder.append("\n");
			}
			builder.append("\n");
		}

		return builder.toString();
	}

	protected LlmReviewer createReviewer(String taskPrompt) {
		return new LlmReviewer(taskPrompt, chatClient);
	}
}
