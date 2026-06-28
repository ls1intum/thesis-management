package de.tum.cit.aet.thesis.feedback.service.reviewer;

import de.tum.cit.aet.thesis.feedback.dto.IntermediateReviewResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Single-category LLM reviewer: prompts the chat model with per-page text and page images
 * using a shared prompt, a category-specific task prompt, and the official guidelines, and
 * returns the parsed intermediate result.
 */
public class LlmReviewer {
	private final String sharedPrompt;
	private final String taskPrompt;
	private final String guidelinesPrompt;
	private final ChatClient chatClient;

	/**
	 * Creates a reviewer with explicit prompt components, primarily for testing.
	 *
	 * @param sharedPrompt      shared instructions prepended to every review
	 * @param taskPrompt        category-specific task instructions
	 * @param guidelinesPrompt  reference guidelines appended to every review
	 * @param chatClient        Spring AI chat client used to call the LLM
	 */
	public LlmReviewer(String sharedPrompt, String taskPrompt, String guidelinesPrompt, ChatClient chatClient) {
		this.sharedPrompt = sharedPrompt;
		this.taskPrompt = taskPrompt;
		this.guidelinesPrompt = guidelinesPrompt;
		this.chatClient = chatClient;
	}

	/**
	 * Creates a reviewer using the default shared and guidelines prompts from {@link Prompts}.
	 *
	 * @param taskPrompt   category-specific task instructions
	 * @param chatClient   Spring AI chat client used to call the LLM
	 */
	public LlmReviewer(String taskPrompt, ChatClient chatClient) {
		this(Prompts.SHARED.getPrompt(), taskPrompt, Prompts.GUIDELINES.getPrompt(), chatClient);
	}

	/**
	 * Runs a single review pass over the provided pages and images.
	 *
	 * @param pages   per-page extracted text in document order
	 * @param images  per-page rendered images in document order
	 * @return the intermediate review result parsed from the LLM response
	 */
	public IntermediateReviewResult review(List<String> pages, List<Media> images) {
		String pagesText = IntStream.range(0, pages.size())
				.mapToObj(index -> "=== PAGE " + (index + 1) + " ===\n" + pages.get(index))
				.collect(Collectors.joining("\n\n"));

		return chatClient.prompt().system(systemMessage -> systemMessage.text(sharedPrompt + "\n\n" + taskPrompt + "\n\n" + guidelinesPrompt))
				.user(userMessage -> userMessage.text(pagesText).media(images.toArray(new Media[0]))).call().entity(IntermediateReviewResult.class);
	}
}
