package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the real request-building path of {@link AgenticReviewer#runAgent} through a real
 * {@link ChatClient} against a stub model, so a tool that never reaches the model fails here rather
 * than in production. {@link AgenticReviewerTest} replaces {@code runAgent} entirely and cannot
 * catch that.
 */
class AgenticReviewerRequestTest {

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Group overview.",
			List.of(new CategoryGuidelines("structure", List.of("Every proposal must contain an Abstract."))));

	/** Records every prompt and answers each with the same scripted response. */
	private static final class RecordingChatModel implements ChatModel {
		private final List<Prompt> prompts = new ArrayList<>();
		private final AssistantMessage answer;

		RecordingChatModel(AssistantMessage answer) {
			this.answer = answer;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			return new ChatResponse(List.of(new Generation(answer)));
		}

		@Override
		public ChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}
	}

	private static ReviewResult review(RecordingChatModel model, List<String> pages, int maxTurns) {
		PdfService pdfService = mock(PdfService.class);
		Resource document = new ByteArrayResource("pdf-content".getBytes());
		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(pages);
		AgenticReviewer reviewer = new AgenticReviewer(pdfService, ChatClient.builder(model),
				ToolCallingManager.builder().build(), metrics -> {}, maxTurns);
		return reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, document, mock(ProgressReporter.class)));
	}

	/** Runs a review whose model never calls a tool, so the run checks nothing; only the request matters. */
	private static RecordingChatModel reviewIgnoringTheOutcome(List<String> pages) {
		RecordingChatModel model = new RecordingChatModel(new AssistantMessage("Done."));
		try {
			review(model, pages, 40);
		} catch (IllegalStateException expected) {
			// Nothing was checked and nothing was found, which the reviewer reports as a failure.
		}
		return model;
	}

	@Test
	void everyToolboxToolIsSentToTheModel() {
		RecordingChatModel model = reviewIgnoringTheOutcome(List.of("Abstract\nSome text."));

		assertThat(model.prompts).hasSize(1);
		assertThat(model.prompts.getFirst().getOptions()).isInstanceOf(ToolCallingChatOptions.class);
		List<String> toolNames = ((ToolCallingChatOptions) model.prompts.getFirst().getOptions()).getToolCallbacks()
				.stream()
				.map(ToolCallback::getToolDefinition)
				.map(definition -> definition.name())
				.toList();
		assertThat(toolNames).containsExactlyInAnyOrder("list_review_categories", "get_category_rules",
				"get_page_text", "search_document", "record_finding", "finish_review");
	}

	@Test
	void bracesInThePageTextReachTheModelVerbatim() {
		// LaTeX, JSON and code listings are full of braces. ChatClient renders text as a template only
		// when parameters are set, which no reviewer does, so they must arrive untouched.
		String latex = "Let $\\frac{a}{b}$ hold for {x} and the config {\"key\": 1} with <tag>.";

		RecordingChatModel model = reviewIgnoringTheOutcome(List.of("Abstract\n" + latex));

		assertThat(model.prompts.getFirst().getUserMessage().getText()).contains(latex);
	}

	@Test
	void aTurnThatRecordsFindingsAndFinishesEndsTheRunWithoutAnotherModelCall() {
		// The common last turn: the final finding and the verdict together. Spring AI's returnDirect
		// is ANDed across a turn's tools, so it would not stop this batch on its own.
		AssistantMessage lastTurn = AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(
						new AssistantMessage.ToolCall("1", "function", "record_finding", """
								{"severity": "MINOR", "category": "WRITING", "title": "Wordy", "description": "Trim it."}"""),
						new AssistantMessage.ToolCall("2", "function", "finish_review",
								"{\"assessment\": \"good\", \"score\": 80, \"summary\": \"Final verdict.\"}")))
				.build();
		RecordingChatModel model = new RecordingChatModel(lastTurn);

		ReviewResult result = review(model, List.of("Abstract\nSome text."), 40);

		assertThat(model.prompts).hasSize(1);
		assertThat(result.findings()).singleElement().satisfies(finding -> assertThat(finding.title()).isEqualTo("Wordy"));
		assertThat(result.summary()).endsWith("Final verdict.");
	}

	@Test
	void theToolCallsOfTheTurnThatExhaustsTheBudgetStillRun() {
		// With a budget of one turn, the first turn is also the last. A finding recorded in it must
		// count, rather than being skipped because the loop stops there. No finish_review here, so
		// the budget, not the verdict, is what ends the run.
		AssistantMessage finalTurn = AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(
						new AssistantMessage.ToolCall("1", "function", "record_finding", """
								{"severity": "MINOR", "category": "WRITING", "title": "Wordy", "description": "Trim it."}""")))
				.build();
		RecordingChatModel model = new RecordingChatModel(finalTurn);

		ReviewResult result = review(model, List.of("Abstract\nSome text."), 1);

		assertThat(model.prompts).hasSize(1);
		assertThat(result.findings()).singleElement().satisfies(finding -> assertThat(finding.title()).isEqualTo("Wordy"));
		assertThat(result.summary()).startsWith("Incomplete review:");
	}
}
