package de.tum.cit.aet.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.utility.AbstractExtractor;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class AiAbstractExtractorTest {

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

	private AiAbstractExtractor extractor;

	@BeforeEach
	void setUp() {
		when(chatClientBuilder.build()).thenReturn(chatClient);
		extractor = new AiAbstractExtractor(pdfService, chatClientBuilder);
	}

	@Test
	void extract_textRichPdf_usesTextOnlyPathWithoutRenderingImages() {
		// Born-digital front matter carries plenty of embedded text, so the extractor must NOT pay
		// the cost of rendering page images — the text-only path is used.
		MultipartFile file = new MockMultipartFile("file", "thesis.pdf",
				"application/pdf", "any-bytes".getBytes());
		List<String> pages = List.of(
				"Abstract. " + "This is a sufficiently long first page of embedded thesis text. ".repeat(3),
				"More born-digital body text on the second page of the document.");
		AbstractExtractor.Result llmResult = new AbstractExtractor.Result(
				AbstractExtractor.Confidence.CONFIDENT,
				"<p>A clearly extractable abstract that is comfortably longer than fifty characters.</p>");

		when(pdfService.extractTextFromPdf(any(MultipartFile.class))).thenReturn(pages);
		stubChatClient(llmResult);

		AbstractExtractor.Result result = extractor.extract(file);

		assertThat(result).isEqualTo(llmResult);
		verify(pdfService).extractTextFromPdf(file);
		verify(pdfService, never()).extractImagesFromPdf(any(MultipartFile.class), any(Integer.class));
		verify(chatClient).prompt();
		verify(callResponseSpec).entity(AbstractExtractor.Result.class);
	}

	@Test
	void extract_imageOnlyScan_rendersPagesAndSendsImagesToVisionModel() {
		// A scanned PDF yields blank embedded text. The extractor must fall back to rendered page
		// images so a vision-capable model can still read the abstract, rather than sending the
		// model nothing but empty page markers.
		MultipartFile file = new MockMultipartFile("file", "scan.pdf",
				"application/pdf", "any-bytes".getBytes());
		List<Media> images = List.of(
				Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data("img-1".getBytes()).name("page_1.png").build(),
				Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data("img-2".getBytes()).name("page_2.png").build());
		AbstractExtractor.Result llmResult = new AbstractExtractor.Result(
				AbstractExtractor.Confidence.CONFIDENT,
				"<p>An abstract read from the rendered page images that exceeds fifty characters.</p>");

		when(pdfService.extractTextFromPdf(any(MultipartFile.class))).thenReturn(List.of("", "   "));
		when(pdfService.extractImagesFromPdf(any(MultipartFile.class), eq(AiAbstractExtractor.MAX_PAGES)))
				.thenReturn(images);
		stubChatClient(llmResult);

		AbstractExtractor.Result result = extractor.extract(file);

		assertThat(result).isEqualTo(llmResult);
		verify(pdfService).extractImagesFromPdf(file, AiAbstractExtractor.MAX_PAGES);
		assertThat(capturedUserMedia()).containsExactlyElementsOf(images);
	}

	@Test
	void extract_emptyPages_shortCircuitsToNone() {
		// If the PDF text extractor returns no pages, there is nothing to feed the LLM — skip the
		// chat call entirely and surface a NONE result so the caller clears any stale suggestion.
		MultipartFile file = new MockMultipartFile("file", "thesis.pdf",
				"application/pdf", "any-bytes".getBytes());
		when(pdfService.extractTextFromPdf(any(MultipartFile.class))).thenReturn(List.of());

		AbstractExtractor.Result result = extractor.extract(file);

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.NONE);
		assertThat(result.html()).isEmpty();
		verify(chatClient, org.mockito.Mockito.never()).prompt();
	}

	@Test
	void extract_llmReturnsNullResult_sanitizesToNone() {
		// A model that fails structured decoding shouldn't crash the upload flow. The extractor
		// coerces the null result into a benign NONE outcome the caller can safely apply.
		MultipartFile file = new MockMultipartFile("file", "thesis.pdf",
				"application/pdf", "any-bytes".getBytes());
		when(pdfService.extractTextFromPdf(any(MultipartFile.class))).thenReturn(List.of("page"));
		stubChatClient(null);

		AbstractExtractor.Result result = extractor.extract(file);

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.NONE);
		assertThat(result.html()).isEmpty();
	}

	@Test
	void extract_llmReturnsNullHtml_sanitizesToEmptyString() {
		// Keep the html field a non-null String so apply()'s isBlank/normalizeText helpers don't
		// have to guard on null.
		MultipartFile file = new MockMultipartFile("file", "thesis.pdf",
				"application/pdf", "any-bytes".getBytes());
		when(pdfService.extractTextFromPdf(any(MultipartFile.class))).thenReturn(List.of("page"));
		stubChatClient(new AbstractExtractor.Result(AbstractExtractor.Confidence.NONE, null));

		AbstractExtractor.Result result = extractor.extract(file);

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.NONE);
		assertThat(result.html()).isEmpty();
	}

	@Test
	void fenceFrontMatter_capsPagesAndWrapsInTags() {
		// Front matter is capped at MAX_PAGES so the prompt cost stays bounded even for long
		// theses whose front matter is longer than expected.
		List<String> pages = java.util.stream.IntStream.rangeClosed(1, AiAbstractExtractor.MAX_PAGES + 5)
				.mapToObj(index -> "page-" + index)
				.toList();

		String fenced = AiAbstractExtractor.fenceFrontMatter(pages);

		assertThat(fenced).startsWith("<pdf-front-matter>\n");
		assertThat(fenced).endsWith("\n</pdf-front-matter>");
		assertThat(fenced).contains("=== PAGE 1 ===\npage-1");
		assertThat(fenced).contains("=== PAGE " + AiAbstractExtractor.MAX_PAGES + " ===\npage-" + AiAbstractExtractor.MAX_PAGES);
		assertThat(fenced).doesNotContain("page-" + (AiAbstractExtractor.MAX_PAGES + 1));
	}

	@Test
	void extract_confidentButTooShort_downgradesToUncertain() {
		// The prompt asks for a confident abstract of at least fifty characters, but prompt rules are
		// not validation: a short confident result must be downgraded so it is offered as a
		// suggestion rather than auto-filled into an empty abstract.
		AbstractExtractor.Result result = runWithLlmResponse(new AbstractExtractor.Result(
				AbstractExtractor.Confidence.CONFIDENT, "<p>Too short.</p>"));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.UNCERTAIN);
		assertThat(result.html()).isEqualTo("<p>Too short.</p>");
	}

	@Test
	void extract_confidentButTooLong_downgradesToUncertain() {
		String overlong = "<p>" + "word ".repeat(500) + "</p>";
		AbstractExtractor.Result result = runWithLlmResponse(new AbstractExtractor.Result(
				AbstractExtractor.Confidence.CONFIDENT, overlong));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.UNCERTAIN);
	}

	@Test
	void extract_confidentWithDisallowedMarkup_downgradesToUncertain() {
		// Non-paragraph markup (here an injected script tag) breaks the paragraph-only contract, so a
		// confident result carrying it is never auto-filled silently.
		AbstractExtractor.Result result = runWithLlmResponse(new AbstractExtractor.Result(
				AbstractExtractor.Confidence.CONFIDENT,
				"<p>A plausible looking abstract that is long enough to pass the length check.</p><script>alert(1)</script>"));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.UNCERTAIN);
	}

	@Test
	void extract_confidentWithEmptyHtml_downgradesToNone() {
		AbstractExtractor.Result result = runWithLlmResponse(new AbstractExtractor.Result(
				AbstractExtractor.Confidence.CONFIDENT, "<p>   </p>"));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.NONE);
		assertThat(result.html()).isEmpty();
	}

	@Test
	void extract_confidentAndPlausible_isLeftConfident() {
		AbstractExtractor.Result plausible = new AbstractExtractor.Result(
				AbstractExtractor.Confidence.CONFIDENT,
				"<p>A perfectly plausible abstract that comfortably clears the fifty character minimum.</p>");

		AbstractExtractor.Result result = runWithLlmResponse(plausible);

		assertThat(result).isEqualTo(plausible);
	}

	/** Runs the extractor over a text-rich page so only the given model response is under test. */
	private AbstractExtractor.Result runWithLlmResponse(AbstractExtractor.Result response) {
		MultipartFile file = new MockMultipartFile("file", "thesis.pdf",
				"application/pdf", "any-bytes".getBytes());
		when(pdfService.extractTextFromPdf(any(MultipartFile.class)))
				.thenReturn(List.of("Abstract heading and enough embedded body text to stay on the text-only path here."));
		stubChatClient(response);
		return extractor.extract(file);
	}

	/** Captures the media attached to the chat user message by replaying the captured consumer. */
	private List<Media> capturedUserMedia() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Consumer<ChatClient.PromptUserSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
		verify(chatClientRequestSpec).user(captor.capture());

		ChatClient.PromptUserSpec userSpec = mock(ChatClient.PromptUserSpec.class);
		when(userSpec.text(any(String.class))).thenReturn(userSpec);
		when(userSpec.media(any(Media[].class))).thenReturn(userSpec);
		captor.getValue().accept(userSpec);

		ArgumentCaptor<Media[]> mediaCaptor = ArgumentCaptor.forClass(Media[].class);
		verify(userSpec).media(mediaCaptor.capture());
		return List.of(mediaCaptor.getValue());
	}

	private void stubChatClient(AbstractExtractor.Result response) {
		when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any()))
				.thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()))
				.thenReturn(chatClientRequestSpec);
		when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.entity(AbstractExtractor.Result.class)).thenReturn(response);
	}
}
