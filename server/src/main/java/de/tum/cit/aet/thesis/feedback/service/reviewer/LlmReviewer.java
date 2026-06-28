package de.tum.cit.aet.thesis.feedback.service.reviewer;

import de.tum.cit.aet.thesis.feedback.dto.IntermediateReviewResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LlmReviewer {
	private final String sharedPrompt;
	private final String taskPrompt;
	private final String guidelinesPrompt;
	private final ChatClient chatClient;

	public LlmReviewer(String sharedPrompt, String taskPrompt, String guidelinesPrompt, ChatClient chatClient) {
		this.sharedPrompt = sharedPrompt;
		this.taskPrompt = taskPrompt;
		this.guidelinesPrompt = guidelinesPrompt;
		this.chatClient = chatClient;
	}

	public LlmReviewer(String taskPrompt, ChatClient chatClient) {
		this(Prompts.SHARED.getPrompt(), taskPrompt, Prompts.GUIDELINES.getPrompt(), chatClient);
	}

	public IntermediateReviewResult review(List<String> pages, List<Media> images) {
		String pagesText = IntStream.range(0, pages.size())
				.mapToObj(index -> "=== PAGE " + (index + 1) + " ===\n" + pages.get(index))
				.collect(Collectors.joining("\n\n"));

		return chatClient.prompt().system(systemMessage -> systemMessage.text(sharedPrompt + "\n\n" + taskPrompt + "\n\n" + guidelinesPrompt))
				.user(userMessage -> userMessage.text(pagesText).media(images.toArray(new Media[0]))).call().entity(IntermediateReviewResult.class);
	}
}
