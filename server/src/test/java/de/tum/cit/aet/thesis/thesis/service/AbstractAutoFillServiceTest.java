package de.tum.cit.aet.thesis.thesis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import de.tum.cit.aet.thesis.core.utility.AbstractExtractor;
import de.tum.cit.aet.thesis.thesis.constants.ThesisAbstractSource;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

class AbstractAutoFillServiceTest {

	private final AbstractAutoFillService service = new AbstractAutoFillService();

	private static Thesis thesisWith(String abstractText, ThesisAbstractSource source) {
		Thesis thesis = new Thesis();
		thesis.setAbstractField(abstractText);
		thesis.setAbstractSource(source);
		return thesis;
	}

	private static AbstractExtractor.Result confident(String html) {
		return new AbstractExtractor.Result(AbstractExtractor.Confidence.CONFIDENT, html);
	}

	@Test
	void apply_confidentAndBlankAbstract_autoFillsAndMarksExtracted() {
		Thesis thesis = thesisWith("", ThesisAbstractSource.MANUAL);

		service.apply(thesis, confident("<p>Extracted abstract.</p>"));

		assertEquals("<p>Extracted abstract.</p>", thesis.getAbstractField());
		assertEquals(ThesisAbstractSource.EXTRACTED, thesis.getAbstractSource());
		assertNull(thesis.getAbstractSuggestion());
	}

	@Test
	void apply_confidentAndPreviouslyExtracted_refreshesAbstract() {
		Thesis thesis = thesisWith("<p>Old extracted.</p>", ThesisAbstractSource.EXTRACTED);

		service.apply(thesis, confident("<p>New extracted abstract.</p>"));

		assertEquals("<p>New extracted abstract.</p>", thesis.getAbstractField());
		assertEquals(ThesisAbstractSource.EXTRACTED, thesis.getAbstractSource());
		assertNull(thesis.getAbstractSuggestion());
	}

	@Test
	void apply_confidentButManualAbstract_storesSuggestionWithoutOverwriting() {
		Thesis thesis = thesisWith("<p>Human written abstract.</p>", ThesisAbstractSource.MANUAL);

		service.apply(thesis, confident("<p>Extracted abstract.</p>"));

		assertEquals("<p>Human written abstract.</p>", thesis.getAbstractField());
		assertEquals(ThesisAbstractSource.MANUAL, thesis.getAbstractSource());
		assertEquals("<p>Extracted abstract.</p>", thesis.getAbstractSuggestion());
	}

	@Test
	void apply_uncertain_storesSuggestionAndLeavesAbstractUntouched() {
		Thesis thesis = thesisWith("<p>Human written abstract.</p>", ThesisAbstractSource.MANUAL);

		service.apply(thesis, new AbstractExtractor.Result(
				AbstractExtractor.Confidence.UNCERTAIN, "<p>Maybe an abstract.</p>"));

		assertEquals("<p>Human written abstract.</p>", thesis.getAbstractField());
		assertEquals(ThesisAbstractSource.MANUAL, thesis.getAbstractSource());
		assertEquals("<p>Maybe an abstract.</p>", thesis.getAbstractSuggestion());
	}

	@Test
	void apply_none_changesNothing() {
		Thesis thesis = thesisWith("<p>Human written abstract.</p>", ThesisAbstractSource.MANUAL);
		thesis.setAbstractSuggestion("<p>stale</p>");

		service.apply(thesis, new AbstractExtractor.Result(AbstractExtractor.Confidence.NONE, ""));

		assertEquals("<p>Human written abstract.</p>", thesis.getAbstractField());
		assertEquals(ThesisAbstractSource.MANUAL, thesis.getAbstractSource());
		assertEquals("<p>stale</p>", thesis.getAbstractSuggestion());
	}

	@Test
	void process_confidentPdfAndBlankAbstract_autoFills() {
		Thesis thesis = thesisWith("", ThesisAbstractSource.MANUAL);
		MockMultipartFile file = new MockMultipartFile("file", "thesis.pdf",
				"application/pdf", buildConfidentPdf());

		service.process(thesis, file);

		assertEquals(ThesisAbstractSource.EXTRACTED, thesis.getAbstractSource());
		assertEquals("<p>This is a clearly extractable abstract for the thesis document.</p>",
				thesis.getAbstractField());
	}

	@Test
	void process_unreadableBytes_isNoOp() {
		Thesis thesis = thesisWith("<p>Existing.</p>", ThesisAbstractSource.MANUAL);
		MockMultipartFile file = new MockMultipartFile("file", "broken.pdf",
				"application/pdf", "not a real pdf".getBytes());

		service.process(thesis, file);

		assertEquals("<p>Existing.</p>", thesis.getAbstractField());
		assertEquals(ThesisAbstractSource.MANUAL, thesis.getAbstractSource());
		assertNull(thesis.getAbstractSuggestion());
	}

	private static byte[] buildConfidentPdf() {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
				PdfPage page = pdf.addNewPage();
				PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
				PdfCanvas canvas = new PdfCanvas(page);
				drawLine(canvas, font, "Abstract", 14f, 780f);
				drawLine(canvas, font, "This is a clearly extractable abstract for the thesis document.", 11f, 750f);
				drawLine(canvas, font, "1 Introduction", 14f, 715f);
				drawLine(canvas, font, "Body of the introduction.", 11f, 690f);
			}
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to build test PDF", e);
		}
	}

	private static void drawLine(PdfCanvas canvas, PdfFont font, String text, float size, float y) {
		canvas.beginText().setFontAndSize(font, size).moveText(56f, y).showText(text).endText();
	}
}
