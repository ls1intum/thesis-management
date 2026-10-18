package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts per-page text and renders per-page PNG images from an uploaded PDF so the LLM
 * pipeline can reason about both modalities.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class PdfService {
	private static final Logger log = LoggerFactory.getLogger(PdfService.class);

	/** Maximum number of PDF pages accepted for review. Long documents are rejected outright. */
	static final int MAX_PAGES = 120;

	/**
	 * Extracts the text content of each page of the uploaded PDF.
	 *
	 * @param file uploaded PDF file
	 * @return one string per page in document order
	 */
	public List<String> extractTextFromPdf(MultipartFile file) {
		log.debug("Extracting text from PDF file: {}", file.getOriginalFilename());
		return extractTextFromPdf(readBytes(file));
	}

	/**
	 * Extracts the text content of each page of the PDF loaded from a Spring {@link Resource}.
	 *
	 * @param resource PDF resource loaded from the thesis upload store
	 * @return one string per page in document order
	 */
	public List<String> extractTextFromPdf(Resource resource) {
		log.debug("Extracting text from resource: {}", resource.getDescription());
		return extractTextFromPdf(readBytes(resource));
	}

	private List<String> extractTextFromPdf(byte[] bytes) {
		assertWithinPageLimit(bytes);

		PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build();
		PagePdfDocumentReader reader = new PagePdfDocumentReader(new ByteArrayResource(bytes), config);

		List<Document> docs = reader.read();
		return docs.stream().map(Document::getText).map(PdfService::toValidText).toList();
	}

	/**
	 * Strips characters that cannot survive the trip to the model.
	 *
	 * <p>PDF text extraction yields Java strings, and Java strings are UTF-16: a glyph the document
	 * encoded oddly can come back as an <em>unpaired</em> surrogate, a code unit with no legal UTF-8
	 * encoding at all. It serializes into the request as an escaped half-character — syntactically
	 * valid JSON — and then kills the provider's tokenizer, which answers
	 * {@code 500 Internal Server Error} with no indication of what it objected to. One such code
	 * unit anywhere in a document therefore fails every call of every review strategy for that
	 * document, permanently and unrecoverably.
	 *
	 * <p>Verified against the gateway: a request carrying a lone high surrogate returns a 500, while
	 * the same request without it succeeds. A NUL byte, which extraction also produces, was
	 * accepted — it is dropped here anyway, because it carries no textual meaning and other
	 * providers are less forgiving.
	 *
	 * <p>Surrogate <em>pairs</em> are preserved: together they are an ordinary character outside the
	 * basic plane, and dropping them would quietly delete content.
	 *
	 * @param text one page's extracted text
	 * @return the text with unpaired surrogates and meaningless control characters removed
	 */
	static String toValidText(String text) {
		if (text == null) {
			return "";
		}

		StringBuilder clean = new StringBuilder(text.length());
		for (int index = 0; index < text.length(); index++) {
			char current = text.charAt(index);
			if (Character.isHighSurrogate(current)) {
				if (index + 1 < text.length() && Character.isLowSurrogate(text.charAt(index + 1))) {
					clean.append(current).append(text.charAt(index + 1));
					index++;
				}
			} else if (!Character.isLowSurrogate(current) && !isMeaninglessControl(current)) {
				clean.append(current);
			}
		}
		return clean.toString();
	}

	/** True for C0 controls that carry no textual meaning; line breaks and tabs are kept. */
	private static boolean isMeaninglessControl(char character) {
		return character < 0x20 && character != '\n' && character != '\r' && character != '\t';
	}

	/**
	 * Renders each page of the uploaded PDF to a PNG image at 300 DPI.
	 *
	 * @param file uploaded PDF file
	 * @return one PNG-encoded {@link Media} per page in document order
	 */
	public List<Media> extractImagesFromPdf(MultipartFile file) {
		log.debug("Extracting images from PDF file: {}", file.getOriginalFilename());
		return extractImagesFromPdf(readBytes(file));
	}

	/**
	 * Renders each page of the PDF loaded from a Spring {@link Resource} to a PNG image at 300 DPI.
	 *
	 * @param resource PDF resource loaded from the thesis upload store
	 * @return one PNG-encoded {@link Media} per page in document order
	 */
	public List<Media> extractImagesFromPdf(Resource resource) {
		log.debug("Extracting images from resource: {}", resource.getDescription());
		return extractImagesFromPdf(readBytes(resource));
	}

	private List<Media> extractImagesFromPdf(byte[] bytes) {
		List<Media> images = new ArrayList<>();

		try (PDDocument document = Loader.loadPDF(bytes)) {
			assertWithinPageLimit(document.getNumberOfPages());
			PDFRenderer renderer = new PDFRenderer(document);

			for (int page = 0; page < document.getNumberOfPages(); page++) {
				var image = renderer.renderImageWithDPI(page, 300);

				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				ImageIO.write(image, "png", stream);
				byte[] imageBytes = stream.toByteArray();

				ByteArrayResource resource = new ByteArrayResource(imageBytes);

				Media media = Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data(resource).name("page_" + (page + 1) + ".png").build();

				images.add(media);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to extract images of file", e);
		}

		return images;
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new RuntimeException("Failed to read PDF bytes", e);
		}
	}

	private byte[] readBytes(Resource resource) {
		try (InputStream in = resource.getInputStream()) {
			return in.readAllBytes();
		} catch (IOException e) {
			throw new RuntimeException("Failed to read PDF bytes", e);
		}
	}

	private void assertWithinPageLimit(byte[] pdfBytes) {
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			assertWithinPageLimit(document.getNumberOfPages());
		} catch (IOException e) {
			throw new RuntimeException("Failed to read PDF", e);
		}
	}

	private void assertWithinPageLimit(int pageCount) {
		if (pageCount > MAX_PAGES) {
			throw new ResourceInvalidParametersException(
					"PDF has " + pageCount + " pages, which exceeds the maximum of " + MAX_PAGES + ".");
		} else if (pageCount == 0) {
			throw new ResourceInvalidParametersException("PDF has no pages.");
		}
	}
}
