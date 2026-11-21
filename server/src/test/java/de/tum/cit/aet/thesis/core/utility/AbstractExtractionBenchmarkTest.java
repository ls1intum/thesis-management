package de.tum.cit.aet.thesis.core.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Synthetic, fully anonymous benchmark for {@link AbstractExtractor}.
 *
 * <p>Each scenario is a small multi-page PDF built in-test that reproduces a layout pattern
 * observed in a corpus of real proposals and theses — abstract pushed onto page six by thesis
 * front matter, line-end hyphenation, a same-size German "Zusammenfassung" boundary, footnote
 * definitions, section-numbered headings, indented paragraphs, over-length and missing abstracts.
 * It contains no real student content, so it is safe to commit and serves as the permanent
 * regression benchmark; the real corpus is kept locally only as a validation oracle.
 *
 * <p>One real-world trait cannot be reproduced through a synthetic PDF: some thesis fonts decode
 * their hyphen glyph as U+FFFD. Standard fonts drop that glyph at write time, so that case is
 * covered directly by {@code AbstractExtractorTest.normalizeHyphens_replacementCharGlyphBecomesHyphen}.
 */
class AbstractExtractionBenchmarkTest {

	private static final float MARGIN = 70f;
	private static final float INDENT = 88f;
	private static final float TITLE = 20f;
	private static final float HEADING = 16f;
	private static final float SUBHEADING = 14f;
	private static final float BODY = 11f;
	private static final float FOOTNOTE = 8f;

	@TestFactory
	Stream<DynamicTest> benchmark() {
		return Stream.of(
				dynamicTest("thesis: abstract on page 6, de-hyphenated, German excluded, two paragraphs",
						this::thesisStandard),
				dynamicTest("thesis: footnote definition line excluded from the abstract",
						this::thesisFootnote),
				dynamicTest("thesis: same-size Zusammenfassung recognised as the boundary",
						this::thesisSameSizeBoundary),
				dynamicTest("thesis: three indented paragraphs split without extra vertical gap",
						this::thesisIndentedParagraphs),
				dynamicTest("proposal: abstract on page 2 after a title page, Introduction boundary",
						this::proposalStandard),
				dynamicTest("proposal: section-numbered \"1. Abstract\" heading is found",
						this::proposalNumberedHeading),
				dynamicTest("proposal: \"Summary\" heading variant is accepted",
						this::proposalSummaryHeading),
				dynamicTest("line-end hyphen before an uppercase/digit continuation is kept",
						this::compoundHyphenKept),
				dynamicTest("project report without an abstract yields NONE",
						this::projectReportNoAbstract),
				dynamicTest("German-only abstract yields NONE (English-only extractor)",
						this::germanOnlyAbstract),
				dynamicTest("over-length abstract is downgraded to a suggestion",
						this::overlongAbstract));
	}

	private void thesisStandard() {
		List<List<Span>> pages = frontMatter(5);
		pages.add(List.of(
				new Span("Abstract", MARGIN, 790, HEADING),
				new Span("This thesis presents an auto-", INDENT, 765, BODY),
				new Span("mated approach to abstract extraction from thesis documents.", MARGIN, 747, BODY),
				new Span("A second paragraph adds further detail about the method used.", INDENT, 720, BODY),
				new Span("Zusammenfassung", MARGIN, 695, BODY),
				new Span("Diese Arbeit beschreibt einen automatisierten Ansatz dazu.", MARGIN, 677, BODY)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.CONFIDENT);
		assertThat(result.html())
				.contains("<p>This thesis presents an automated approach to abstract extraction "
						+ "from thesis documents.</p>")
				.contains("<p>A second paragraph adds further detail about the method used.</p>")
				.doesNotContain("Zusammenfassung")
				.doesNotContain("Diese Arbeit");
		assertThat(paragraphCount(result)).isEqualTo(2);
	}

	private void thesisFootnote() {
		String superscriptOne = String.valueOf((char) 0x00B9);
		List<List<Span>> pages = frontMatter(5);
		pages.add(List.of(
				new Span("Abstract", MARGIN, 790, HEADING),
				new Span("We present a system that is fully described within this abstract body.", MARGIN, 765, BODY),
				new Span(superscriptOne + "Speech model: https://example.org/whisper", MARGIN, 740, FOOTNOTE),
				new Span("Zusammenfassung", MARGIN, 715, BODY)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.CONFIDENT);
		assertThat(result.html())
				.contains("We present a system")
				.doesNotContain("example.org")
				.doesNotContain("Speech model");
	}

	private void thesisSameSizeBoundary() {
		List<List<Span>> pages = frontMatter(5);
		pages.add(List.of(
				new Span("Abstract", MARGIN, 790, HEADING),
				new Span("The English abstract is long enough to clear the confident length floor.", MARGIN, 765, BODY),
				new Span("Zusammenfassung", MARGIN, 740, BODY),
				new Span("Der deutsche Text darf im Ergebnis nicht erscheinen.", MARGIN, 715, BODY)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.CONFIDENT);
		assertThat(result.html())
				.contains("The English abstract is long enough")
				.doesNotContain("deutsche")
				.doesNotContain("Zusammenfassung");
	}

	private void thesisIndentedParagraphs() {
		List<List<Span>> pages = frontMatter(5);
		pages.add(List.of(
				new Span("Abstract", MARGIN, 790, HEADING),
				new Span("First paragraph line one runs across the column to the margin here.", INDENT, 765, BODY),
				new Span("first paragraph line two returns to the left margin of the column.", MARGIN, 747, BODY),
				new Span("Second paragraph starts with an indent on its first line of text.", INDENT, 729, BODY),
				new Span("second paragraph line two returns to the left margin once more.", MARGIN, 711, BODY),
				new Span("Third paragraph also begins with a clearly indented first line here.", INDENT, 693, BODY),
				new Span("third paragraph line two returns to the left margin a final time.", MARGIN, 675, BODY),
				new Span("Zusammenfassung", MARGIN, 650, BODY)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.CONFIDENT);
		assertThat(paragraphCount(result)).isEqualTo(3);
		assertThat(result.html()).contains("First paragraph line one runs across the column to the "
				+ "margin here. first paragraph line two returns to the left margin of the column.");
	}

	private void proposalStandard() {
		List<List<Span>> pages = new ArrayList<>();
		pages.add(List.of(
				new Span("Bachelor's Thesis Proposal", MARGIN, 700, TITLE),
				new Span("Working title of the proposed project", MARGIN, 660, BODY)));
		pages.add(List.of(
				new Span("Abstract", MARGIN, 790, HEADING),
				new Span("This proposal outlines the planned work and its expected contributions.", MARGIN, 765, BODY),
				new Span("It motivates the problem and sketches the intended evaluation approach.", MARGIN, 747, BODY),
				new Span("1 Introduction", MARGIN, 720, SUBHEADING)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.CONFIDENT);
		assertThat(result.html())
				.contains("This proposal outlines the planned work and its expected contributions.")
				.doesNotContain("Introduction");
	}

	private void proposalNumberedHeading() {
		List<List<Span>> pages = new ArrayList<>();
		pages.add(List.of(new Span("Project Proposal", MARGIN, 700, TITLE)));
		pages.add(List.of(
				new Span("1. Abstract", MARGIN, 790, HEADING),
				new Span("This proposal describes the planned work in sufficient detail to be valid.", MARGIN, 765, BODY),
				new Span("2. Introduction", MARGIN, 740, SUBHEADING)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.CONFIDENT);
		assertThat(result.html()).contains("This proposal describes the planned work");
	}

	private void proposalSummaryHeading() {
		List<List<Span>> pages = new ArrayList<>();
		pages.add(List.of(new Span("Project Proposal", MARGIN, 700, TITLE)));
		pages.add(List.of(
				new Span("Summary", MARGIN, 790, HEADING),
				new Span("This summary states the goal of the proposed project and its scope.", MARGIN, 765, BODY),
				new Span("1 Introduction", MARGIN, 740, SUBHEADING)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.CONFIDENT);
		assertThat(result.html()).contains("This summary states the goal of the proposed project");
	}

	private void compoundHyphenKept() {
		List<List<Span>> pages = new ArrayList<>();
		pages.add(List.of(
				new Span("Abstract", MARGIN, 790, HEADING),
				new Span("We evaluate the platform on Industry-", MARGIN, 765, BODY),
				new Span("4.0 manufacturing data sets across several realistic scenarios.", MARGIN, 747, BODY),
				new Span("1 Introduction", MARGIN, 720, SUBHEADING)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.html()).contains("Industry-4.0");
	}

	private void projectReportNoAbstract() {
		List<List<Span>> pages = new ArrayList<>();
		pages.add(List.of(new Span("Interdisciplinary Project Report", MARGIN, 700, TITLE)));
		pages.add(List.of(
				new Span("1 Introduction", MARGIN, 790, HEADING),
				new Span("This report goes straight into the introduction with no abstract section.", MARGIN, 765, BODY)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.NONE);
		assertThat(result.html()).isNullOrEmpty();
	}

	private void germanOnlyAbstract() {
		List<List<Span>> pages = frontMatter(5);
		pages.add(List.of(
				new Span("Zusammenfassung", MARGIN, 790, HEADING),
				new Span("Diese Arbeit untersucht die Extraktion von Abstracts aus Dokumenten.", MARGIN, 765, BODY),
				new Span("1 Einleitung", MARGIN, 740, SUBHEADING)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.NONE);
	}

	private void overlongAbstract() {
		StringBuilder longBody = new StringBuilder();
		for (int i = 0; i < 45; i++) {
			longBody.append("This sentence pads the abstract beyond the confident length cap. ");
		}
		List<List<Span>> pages = frontMatter(5);
		pages.add(List.of(
				new Span("Abstract", MARGIN, 790, HEADING),
				new Span(longBody.toString(), MARGIN, 765, BODY),
				new Span("Zusammenfassung", MARGIN, 740, BODY)));

		AbstractExtractor.Result result = AbstractExtractor.extract(doc(pages));

		assertThat(result.confidence()).isEqualTo(AbstractExtractor.Confidence.UNCERTAIN);
		assertThat(result.html()).contains("<p>");
	}

	private static int paragraphCount(AbstractExtractor.Result result) {
		String html = result.html();
		return html == null ? 0 : (int) html.chars().filter(c -> c == '<').count() / 2;
	}

	/** Sparse front-matter pages (one body-size line each), mirroring a thesis title/declaration run. */
	private static List<List<Span>> frontMatter(int pages) {
		List<List<Span>> result = new ArrayList<>();
		for (int i = 1; i <= pages; i++) {
			result.add(List.of(new Span("Front matter section " + i, MARGIN, 760, BODY)));
		}
		return result;
	}

	private record Span(String text, float x, float y, float size) {
	}

	private static byte[] doc(List<List<Span>> pages) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
				PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
				for (List<Span> page : pages) {
					PdfCanvas canvas = new PdfCanvas(pdf.addNewPage());
					for (Span span : page) {
						canvas.beginText()
								.setFontAndSize(font, span.size())
								.moveText(span.x(), span.y())
								.showText(span.text())
								.endText();
					}
				}
			}
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to build benchmark PDF", e);
		}
	}
}
