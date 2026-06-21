package de.tum.cit.aet.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;

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

		assertThat(thesis.getAbstractField()).isEqualTo("<p>Extracted abstract.</p>");
		assertThat(thesis.getAbstractSource()).isEqualTo(ThesisAbstractSource.EXTRACTED);
		assertThat(thesis.getAbstractSuggestion()).isNull();
	}

	@Test
	void apply_confidentAndPreviouslyExtractedDiffers_offersSuggestion() {
		// A previously auto-filled (EXTRACTED) abstract is real content now — replacing it must be
		// confirmed by the user, so a differing upload only offers a suggestion.
		Thesis thesis = thesisWith("<p>Old extracted.</p>", ThesisAbstractSource.EXTRACTED);

		service.apply(thesis, confident("<p>New extracted abstract.</p>"));

		assertThat(thesis.getAbstractField()).isEqualTo("<p>Old extracted.</p>");
		assertThat(thesis.getAbstractSource()).isEqualTo(ThesisAbstractSource.EXTRACTED);
		assertThat(thesis.getAbstractSuggestion()).isEqualTo("<p>New extracted abstract.</p>");
	}

	@Test
	void apply_extractionMatchesCurrentAbstract_clearsStaleSuggestion() {
		// No change: the extracted text equals the current abstract — nothing to confirm, and any
		// previously staged suggestion must be cleared so a stale modal can't resurface.
		Thesis thesis = thesisWith("<p>Same abstract text.</p>", ThesisAbstractSource.MANUAL);
		thesis.setAbstractSuggestion("<p>stale</p>");

		service.apply(thesis, confident("<p>Same abstract text.</p>"));

		assertThat(thesis.getAbstractField()).isEqualTo("<p>Same abstract text.</p>");
		assertThat(thesis.getAbstractSource()).isEqualTo(ThesisAbstractSource.MANUAL);
		assertThat(thesis.getAbstractSuggestion()).isNull();
	}

	@Test
	void apply_uncertainAndBlankAbstract_offersSuggestion() {
		// Even into a blank abstract, an uncertain extraction is never auto-filled — it is offered
		// for confirmation so a possibly-wrong abstract is never stored silently.
		Thesis thesis = thesisWith("", ThesisAbstractSource.MANUAL);

		service.apply(thesis, new AbstractExtractor.Result(
				AbstractExtractor.Confidence.UNCERTAIN, "<p>Maybe an abstract.</p>"));

		assertThat(thesis.getAbstractField()).isNullOrEmpty();
		assertThat(thesis.getAbstractSuggestion()).isEqualTo("<p>Maybe an abstract.</p>");
	}

	@Test
	void apply_confidentButManualAbstract_storesSuggestionWithoutOverwriting() {
		Thesis thesis = thesisWith("<p>Human written abstract.</p>", ThesisAbstractSource.MANUAL);

		service.apply(thesis, confident("<p>Extracted abstract.</p>"));

		assertThat(thesis.getAbstractField()).isEqualTo("<p>Human written abstract.</p>");
		assertThat(thesis.getAbstractSource()).isEqualTo(ThesisAbstractSource.MANUAL);
		assertThat(thesis.getAbstractSuggestion()).isEqualTo("<p>Extracted abstract.</p>");
	}

	@Test
	void apply_uncertain_storesSuggestionAndLeavesAbstractUntouched() {
		Thesis thesis = thesisWith("<p>Human written abstract.</p>", ThesisAbstractSource.MANUAL);

		service.apply(thesis, new AbstractExtractor.Result(
				AbstractExtractor.Confidence.UNCERTAIN, "<p>Maybe an abstract.</p>"));

		assertThat(thesis.getAbstractField()).isEqualTo("<p>Human written abstract.</p>");
		assertThat(thesis.getAbstractSource()).isEqualTo(ThesisAbstractSource.MANUAL);
		assertThat(thesis.getAbstractSuggestion()).isEqualTo("<p>Maybe an abstract.</p>");
	}

	@Test
	void apply_none_clearsStaleSuggestionButKeepsAbstract() {
		// Nothing found: the abstract stays, but any previously staged suggestion is cleared so a
		// later no-op upload doesn't keep an outdated modal alive.
		Thesis thesis = thesisWith("<p>Human written abstract.</p>", ThesisAbstractSource.MANUAL);
		thesis.setAbstractSuggestion("<p>stale</p>");

		service.apply(thesis, new AbstractExtractor.Result(AbstractExtractor.Confidence.NONE, ""));

		assertThat(thesis.getAbstractField()).isEqualTo("<p>Human written abstract.</p>");
		assertThat(thesis.getAbstractSource()).isEqualTo(ThesisAbstractSource.MANUAL);
		assertThat(thesis.getAbstractSuggestion()).isNull();
	}

	@Test
	void process_confidentPdfAndBlankAbstract_autoFills() {
		Thesis thesis = thesisWith("", ThesisAbstractSource.MANUAL);
		MockMultipartFile file = new MockMultipartFile("file", "thesis.pdf",
				"application/pdf", buildConfidentPdf());

		service.process(thesis, file);

		assertThat(thesis.getAbstractSource()).isEqualTo(ThesisAbstractSource.EXTRACTED);
		assertThat(thesis.getAbstractField())
				.isEqualTo("<p>This is a clearly extractable abstract for the thesis document.</p>");
	}

	@Test
	void process_unreadableBytes_isNoOp() {
		Thesis thesis = thesisWith("<p>Existing.</p>", ThesisAbstractSource.MANUAL);
		MockMultipartFile file = new MockMultipartFile("file", "broken.pdf",
				"application/pdf", "not a real pdf".getBytes());

		service.process(thesis, file);

		assertThat(thesis.getAbstractField()).isEqualTo("<p>Existing.</p>");
		assertThat(thesis.getAbstractSource()).isEqualTo(ThesisAbstractSource.MANUAL);
		assertThat(thesis.getAbstractSuggestion()).isNull();
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
