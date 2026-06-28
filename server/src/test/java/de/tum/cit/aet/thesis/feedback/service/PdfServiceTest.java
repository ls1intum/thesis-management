package de.tum.cit.aet.thesis.feedback.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.content.Media;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PdfServiceTest {
	private PdfService pdfService;
	private byte[] helloWorldContent;
	private byte[] proposalTemplateContent;

	@BeforeEach
	public void setUp() throws IOException {
		pdfService = new PdfService();
		helloWorldContent = Files.readAllBytes(Path.of("src/test/resources/pdfs/hello-world.pdf"));
		proposalTemplateContent = Files.readAllBytes(Path.of("src/test/resources/pdfs/proposal-template.pdf"));
	}

	@Test
	public void testExtractTextFromSinglePagePdf() {
		MockMultipartFile multipartFile = new MockMultipartFile("hello-world", "hello-world.pdf", "application/pdf", helloWorldContent);

		List<String> actual = pdfService.extractTextFromPdf(multipartFile);

		assertEquals(1, actual.size(), "Expected exactly one page of text to be extracted");
		assertEquals(List.of("""

												Hello      World                                                                                                                                                                    \s

												Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod                                                                                                     \s
												tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At                                                                                                      \s
												vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,                                                                                                    \s
												no sea takimata sanctus est Lorem ipsum dolor sit amet.          Lorem ipsum dolor                                                                                                  \s
												sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt                                                                                                       \s
												ut labore et dolore magna aliquyam erat, sed diam voluptua.           At vero eos et                                                                                                \s
												accusam et justo duo dolores et ea rebum.        Stet clita kasd gubergren, no sea                                                                                                  \s
												takimata sanctus est Lorem ipsum dolor sit amet.                                                                                                                                    \s

																						1                                                                                                                                         \s
				"""), actual, "Extracted text does not match expected content");
	}

	@Test
	public void testExtractTextFromMultiPagePdf() {
		MockMultipartFile multipartFile = new MockMultipartFile("proposalFile", "proposal.pdf", "application/pdf", proposalTemplateContent);

		List<String> actual = pdfService.extractTextFromPdf(multipartFile);

		assertEquals(4, actual.size(), "Expected four pages of text to be extracted");

		// Checking for a single keyword from each page to verify that the content is correctly extracted and ordered
		// We do not check for multiple keywords because the extracted text is rather noisy and contains many arbitrary line breaks and spacing.
		assertTrue(actual.getFirst().contains("Krümelmonster"), "First page should contain the name of the supervisor");
		assertTrue(actual.get(1).contains("Introduction"), "Second page should contain the introduction section");
		assertTrue(actual.get(2).contains("Bibliography"), "Third page should contain the bibliography section");
		assertTrue(actual.get(3).contains("AI"), "Fourth page should contain the transparency in the use of AI tools section");
	}

	@Test
	public void testExtractTextFromPdfFailed() throws IOException {
		MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
		when(multipartFile.getBytes()).thenThrow(IOException.class);

		assertThrows(RuntimeException.class, () -> pdfService.extractTextFromPdf(multipartFile), "Expected an IOException to be thrown for invalid PDF content");
	}

	@Test
	public void testExtractImagesFromMultiPagePdf() throws IOException {
		MockMultipartFile multipartFile = new MockMultipartFile("proposalFile", "proposal.pdf", "application/pdf", proposalTemplateContent);

		List<Media> actual = pdfService.extractImagesFromPdf(multipartFile);
		List<Path> createdSnapshots = new ArrayList<>();

		assertEquals(4, actual.size(), "Expected one rendered image per PDF page");

		for (int index = 0; index < actual.size(); index++) {
			Media image = actual.get(index);
			byte[] actualBytes = image.getDataAsByteArray();

			assertEquals("image/png", image.getMimeType().toString(), "Rendered page should be a PNG image");
			assertEquals("page_" + (index + 1) + ".png", image.getName(), "Rendered page should use the expected file name");

			Path expectedImagePath = Path.of("src/test/resources/pdfs/pdf-images/proposal-template-page-" + (index + 1) + ".png");
			assertImageMatchesSnapshot(expectedImagePath, actualBytes);
		}
	}

	private void assertImageMatchesSnapshot(Path expectedImagePath, byte[] actualBytes) throws IOException {
		if (Files.notExists(expectedImagePath)) {
			Files.createDirectories(expectedImagePath.getParent());
			Files.write(expectedImagePath, actualBytes);
			fail("Created missing snapshot: " + expectedImagePath + ". Please verify the image and re-run the test.");
		}

		assertArrayEquals(Files.readAllBytes(expectedImagePath), actualBytes, "Rendered image does not match snapshot " + expectedImagePath.getFileName());
	}

	@Test
	public void testExtractImagesFromPdfFailed() throws IOException {
		MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
		when(multipartFile.getBytes()).thenThrow(IOException.class);

		assertThrows(RuntimeException.class, () -> pdfService.extractImagesFromPdf(multipartFile), "Expected an IOException to be thrown for invalid PDF content");
	}
}
