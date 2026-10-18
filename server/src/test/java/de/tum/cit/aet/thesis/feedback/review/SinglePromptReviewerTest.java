package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class SinglePromptReviewerTest {

	@Mock
	private PdfService pdfService;

	@Mock
	private ChatClient.Builder chatClientBuilder;

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

	@Mock
	private ChatClient.CallResponseSpec callResponseSpec;

	@Mock
	private ChatClient.PromptSystemSpec promptSystemSpec;

	@Mock
	private ChatClient.PromptUserSpec promptUserSpec;

	@Mock
	private ProgressReporter progress;

	private final List<ReviewRunMetrics> recorded = new ArrayList<>();

	private SinglePromptReviewer reviewer;

	private final Resource pdf = new ByteArrayResource("pdf".getBytes());

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Group overview.",
			List.of(
					new CategoryGuidelines("structure", List.of("Every proposal must contain an Abstract.")),
					new CategoryGuidelines("schedule", List.of("Iterations last two to four weeks."))));

	@BeforeEach
	void setUp() {
		when(chatClientBuilder.build()).thenReturn(chatClient);
		reviewer = new SinglePromptReviewer(pdfService, chatClientBuilder, recorded::add, false, "test/model");
		when(pdfService.extractTextFromPdf(any(Resource.class))).thenReturn(List.of("Page one.", "Page two."));
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(anySystem())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(anyUser())).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
	}

	@Test
	void oneCallReturnsTheReviewAsTheModelConsolidatedIt() {
		ReviewResult expected = new ReviewResult(AssessmentCategory.ACCEPTABLE, 60, "Summary.", List.of());
		when(callResponseSpec.responseEntity(anyConverter())).thenReturn(new ResponseEntity<>(null, expected));

		ReviewResult actual = reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdf, progress));

		assertThat(actual).isSameAs(expected);
		verify(chatClient).prompt();
		verify(pdfService, never()).extractImagesFromPdf(any(Resource.class));
		verify(progress).stepStarted(eq("review"), anyString(), eq(1), eq(1));
		verify(progress).stepCompleted("review", 1, 1);

		assertThat(recorded).singleElement().satisfies(metrics -> {
			assertThat(metrics.strategy()).isEqualTo("SinglePromptReviewer");
			assertThat(metrics.llmCalls()).isEqualTo(1);
			assertThat(metrics.callDepth()).isEqualTo(1);
			assertThat(metrics.categoriesCovered()).isEqualTo(ReviewCategory.values().length);
		});
	}

	@Test
	void theSystemPromptCarriesEveryCategorysTaskAndRulesAndTheUploadStaysFenced() {
		when(callResponseSpec.responseEntity(anyConverter()))
				.thenReturn(new ResponseEntity<>(null, new ReviewResult(null, null, null, List.of())));
		when(promptSystemSpec.text(anyString())).thenReturn(promptSystemSpec);
		when(promptUserSpec.text(anyString())).thenReturn(promptUserSpec);
		when(promptUserSpec.media(any(Media[].class))).thenReturn(promptUserSpec);

		reviewer.review(new ReviewRequest(ReviewType.PROPOSAL, GUIDELINES, pdf));

		ArgumentCaptor<Consumer<ChatClient.PromptSystemSpec>> system = captor();
		ArgumentCaptor<Consumer<ChatClient.PromptUserSpec>> user = captor();
		verify(chatClientRequestSpec).system(system.capture());
		verify(chatClientRequestSpec).user(user.capture());
		system.getValue().accept(promptSystemSpec);
		user.getValue().accept(promptUserSpec);

		ArgumentCaptor<String> systemText = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> userText = ArgumentCaptor.forClass(String.class);
		verify(promptSystemSpec).text(systemText.capture());
		verify(promptUserSpec).text(userText.capture());

		// Same task text the fan-out sends category by category, so only the orchestration differs.
		for (ReviewCategory category : ReviewCategory.values()) {
			assertThat(systemText.getValue()).contains(Prompts.taskPromptFor(category, ReviewType.PROPOSAL));
		}
		assertThat(systemText.getValue())
				.contains(CategoryReviewer.SECURITY_PROMPT)
				.contains(Prompts.SHARED.getPrompt(ReviewType.PROPOSAL))
				.contains("Every proposal must contain an Abstract.")
				.contains("Iterations last two to four weeks.")
				.endsWith(Prompts.SINGLE_PROMPT.getPrompt(ReviewType.PROPOSAL));
		assertThat(userText.getValue())
				.startsWith("<student-upload-page-text>")
				.contains("=== PAGE 2 ===\nPage two.")
				.endsWith("</student-upload-page-text>");
	}

	@Test
	void aFailedCallFailsTheReviewAndMarksTheStep() {
		when(callResponseSpec.responseEntity(anyConverter())).thenThrow(new IllegalStateException("provider down"));

		assertThatThrownBy(() -> reviewer.review(new ReviewRequest(ReviewType.THESIS, GUIDELINES, pdf, progress)))
				.hasMessage("provider down");
		verify(progress).stepFailed(eq("review"), anyInt(), anyInt(), anyString());
		assertThat(recorded).isEmpty();
	}

	private static StructuredOutputConverter<ReviewResult> anyConverter() {
		return any();
	}

	private static Consumer<ChatClient.PromptSystemSpec> anySystem() {
		return any();
	}

	private static Consumer<ChatClient.PromptUserSpec> anyUser() {
		return any();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T> ArgumentCaptor<Consumer<T>> captor() {
		return ArgumentCaptor.forClass((Class) Consumer.class);
	}
}
