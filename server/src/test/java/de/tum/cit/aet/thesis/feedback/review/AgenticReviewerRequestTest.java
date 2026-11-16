package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
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

	/** Records every prompt and answers with plain text, which ends the agent loop after one turn. */
	private static final class RecordingChatModel implements ChatModel {
		private final List<Prompt> prompts = new ArrayList<>();

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Done."))));
		}

		@Override
		public ChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}
	}

	@Test
	void everyToolboxToolIsSentToTheModel() {
		RecordingChatModel model = new RecordingChatModel();
		PdfService pdfService = mock(PdfService.class);
		Resource document = new ByteArrayResource("pdf-content".getBytes());
		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Abstract\nSome text."));

		AgenticReviewer reviewer = new AgenticReviewer(pdfService, ChatClient.builder(model),
				ToolCallingManager.builder().build(), metrics -> {}, 40);
		try {
			reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, document, mock(ProgressReporter.class)));
		} catch (IllegalStateException expected) {
			// The stub model never calls a tool, so the run checks nothing; only the request matters.
		}

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
}
