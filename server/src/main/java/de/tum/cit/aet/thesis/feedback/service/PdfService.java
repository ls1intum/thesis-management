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
		return docs.stream().map(Document::getText).toList();
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
