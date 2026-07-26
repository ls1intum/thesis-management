package de.tum.cit.aet.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.utility.AbstractExtractor;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.web.MockMultipartFile;
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
	void extract_pullsPagesFromPdfServiceAndReturnsLlmResult() {
		MultipartFile file = new MockMultipartFile("file", "thesis.pdf",
				"application/pdf", "any-bytes".getBytes());
		List<String> pages = List.of("page-1-text", "page-2-text");
		AbstractExtractor.Result llmResult = new AbstractExtractor.Result(
				AbstractExtractor.Confidence.CONFIDENT, "<p>LLM-picked abstract.</p>");

		when(pdfService.extractTextFromPdf(any(MultipartFile.class))).thenReturn(pages);
		stubChatClient(llmResult);

		AbstractExtractor.Result result = extractor.extract(file);

		assertThat(result).isEqualTo(llmResult);
		verify(pdfService).extractTextFromPdf(file);
		verify(chatClient).prompt();
		verify(callResponseSpec).entity(AbstractExtractor.Result.class);
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
