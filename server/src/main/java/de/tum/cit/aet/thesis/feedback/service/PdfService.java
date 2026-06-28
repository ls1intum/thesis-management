package de.tum.cit.aet.thesis.feedback.service;

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
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

	/**
	 * Extracts the text content of each page of the uploaded PDF.
	 *
	 * @param file uploaded PDF file
	 * @return one string per page in document order
	 */
	public List<String> extractTextFromPdf(MultipartFile file) {
		log.debug("Extracting text from PDF file: {}", file.getOriginalFilename());
		ByteArrayResource resource;
		try {
			resource = new ByteArrayResource(file.getBytes());
		} catch (IOException e) {
			throw new RuntimeException("Failed to extract text of file", e);
		}

		PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build();
		PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, config);

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
		List<Media> images = new ArrayList<>();

		try (PDDocument document = Loader.loadPDF(file.getBytes())) {
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
}
