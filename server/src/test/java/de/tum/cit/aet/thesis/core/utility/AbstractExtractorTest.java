package de.tum.cit.aet.thesis.core.utility;

import static org.assertj.core.api.Assertions.assertThat;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

class AbstractExtractorTest {

	private static final float MARGIN_X = 56f;

	/** A single line of text drawn at an absolute baseline position with a given font size. */
	private record Line(String text, float fontSize, float y) {
	}

	/** Builds a one-page PDF placing each line at its exact baseline so layout is fully controlled. */
	private static byte[] buildPdf(List<Line> lines) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
				PdfPage page = pdf.addNewPage();
				PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
				PdfCanvas canvas = new PdfCanvas(page);

				for (Line line : lines) {
					canvas.beginText()
							.setFontAndSize(font, line.fontSize())
							.moveText(MARGIN_X, line.y())
							.showText(line.text())
							.endText();
				}
			}
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to build test PDF", e);
		}
	}

	@Test
	void extract_clearAbstractWithIntroductionBoundary_returnsConfidentDehyphenatedParagraphs() {
		byte[] pdf = buildPdf(List.of(
				new Line("Abstract", 14f, 780f),
				new Line("This paper presents a com-", 11f, 750f),
				new Line("prehensive study of ab-", 11f, 735f),
				new Line("stract extraction from PDFs.", 11f, 720f),
				new Line("A second paragraph adds more detail.", 11f, 690f),
				new Line("1 Introduction", 14f, 655f),
				new Line("Body text of the introduction section.", 11f, 630f)
		));

		AbstractExtractor.Result result = AbstractExtractor.extract(pdf);

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.CONFIDENT);
		assertThat(result.html()).isEqualTo(
				"<p>This paper presents a comprehensive study of abstract extraction from PDFs.</p>"
						+ "<p>A second paragraph adds more detail.</p>");
	}

	@Test
	void extract_noAbstractHeading_returnsNone() {
		byte[] pdf = buildPdf(List.of(
				new Line("1 Introduction", 14f, 780f),
				new Line("This document has no abstract section at all.", 11f, 750f)
		));

		AbstractExtractor.Result result = AbstractExtractor.extract(pdf);

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.NONE);
		assertThat(result.html()).isNullOrEmpty();
	}

	@Test
	void extract_germanHeadingOnly_returnsNoneBecauseEnglishOnly() {
		byte[] pdf = buildPdf(List.of(
				new Line("Zusammenfassung", 14f, 780f),
				new Line("Diese Arbeit untersucht die Extraktion von Abstracts.", 11f, 750f),
				new Line("1 Einleitung", 14f, 715f)
		));

		AbstractExtractor.Result result = AbstractExtractor.extract(pdf);

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.NONE);
	}

	@Test
	void extract_abstractTooLong_returnsUncertain() {
		StringBuilder filler = new StringBuilder();
		for (int i = 0; i < 60; i++) {
			filler.append("This sentence pads the abstract well beyond the confident length cap. ");
		}

		byte[] pdf = buildPdf(List.of(
				new Line("Abstract", 14f, 780f),
				new Line(filler.toString(), 11f, 750f),
				new Line("1 Introduction", 14f, 715f)
		));

		AbstractExtractor.Result result = AbstractExtractor.extract(pdf);

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.UNCERTAIN);
		assertThat(result.html()).contains("<p>");
	}

	@Test
	void extract_abstractHeadingButNoBoundary_returnsUncertain() {
		byte[] pdf = buildPdf(List.of(
				new Line("Summary", 14f, 780f),
				new Line("A short summary with no following section heading to bound it.", 11f, 750f)
		));

		AbstractExtractor.Result result = AbstractExtractor.extract(pdf);

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.UNCERTAIN);
	}

	@Test
	void extract_lineEndHyphenBeforeUppercase_keepsHyphen() {
		byte[] pdf = buildPdf(List.of(
				new Line("Abstract", 14f, 780f),
				new Line("We evaluate the system on Industry-", 11f, 750f),
				new Line("4.0 manufacturing data sets in detail here.", 11f, 735f),
				new Line("1 Introduction", 14f, 700f)
		));

		AbstractExtractor.Result result = AbstractExtractor.extract(pdf);

		assertThat(result.html())
				.as("Expected hyphen kept before an uppercase/digit continuation")
				.contains("Industry-4.0");
	}
}
