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

@Service
@Conditional(AIFeaturesEnabled.class)
public class ReviewService {
	private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

	private final PdfService pdfService;
	private final ChatClient chatClient;

	public ReviewService(PdfService pdfService, ChatClient.Builder chatClientBuilder) {
		this.pdfService = pdfService;
		this.chatClient = chatClientBuilder.build();
	}

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
