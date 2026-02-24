package de.tum.cit.aet.thesis.utility;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.styledxmlparser.resolver.font.BasicFontProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for generating PDF documents with a heading, key-value data
 * pairs, and HTML content sections.
 */
public class PDFBuilder {
	private final String heading;
	private final String currentUserName;
	private final List<Section> sections;
	private final List<Data> data;

	private final List<String> headerItems = new ArrayList<>();

	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static final PdfFont normalFont = createNormalFont();
	private static final String THESISMANAGEMENT_URL = "https://thesis.aet.cit.tum.de/";

	// ----------------- Colors -----------------
	private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(0x50, 0x4c, 0x97);
	private static final DeviceRgb METADATA_COLOR = new DeviceRgb(0x8d, 0x8d, 0x8f);

	// ----------------- Font Sizes -----------------
	private static final float FONT_SIZE_METADATA = 7f;

	// ----------------- Spacing -----------------
	private static final float MARGIN_PDF_TOP_AND_BOTTOM = 8f;
	private static final float MARGIN_HEADER_ITEMS_BOTTOM = 8f;
	private static final float METADATA_MARGIN_LEFT_RIGHT = 15f;

	private record Data(String title, String value) {
	}

	private record Section(String heading, String htmlContent) {
	}

	/**
	 * Initializes a new PDF builder with the given main heading.
	 *
	 * @param heading the main heading for the PDF document
	 */
	public PDFBuilder(String heading, String currentUserName) {
		this.heading = heading;
		this.currentUserName = currentUserName;
		this.sections = new ArrayList<>();
		this.data = new ArrayList<>();
	}

	// ----------------- Header -----------------

	/**
	 * Adds a header item to be displayed above the main heading
	 *
	 * @param item the header item text
	 * @return this builder for method chaining
	 */
	public PDFBuilder addHeaderItem(String item) {
		headerItems.add(item);
		return this;
	}

	// ----------------- Overview -----------------

	/**
	 * Adds a key-value data pair to be rendered in the PDF document.
	 *
	 * @param title the data label
	 * @param value the data value
	 * @return this PDFBuilder instance for method chaining
	 */
	public PDFBuilder addData(String title, String value) {
		data.add(new Data(title, value));

		return this;
	}

	// ----------------- Section Groups -----------------

	/**
	 * Adds a titled section with HTML content to be rendered in the PDF document.
	 *
	 * @param heading     the section heading
	 * @param htmlContent the HTML content of the section
	 * @return this PDFBuilder instance for method chaining
	 */
	public PDFBuilder addSection(String heading, String htmlContent) {
		sections.add(new Section(heading, htmlContent));

		return this;
	}

	// ----------------- Build PDF -----------------

	/**
	 * Builds the PDF document and returns it as a byte array resource.
	 *
	 * @return the generated PDF as a Resource
	 */
	public Resource build() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		PdfWriter writer = new PdfWriter(outputStream);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);

		document.setTopMargin(MARGIN_PDF_TOP_AND_BOTTOM);

		// Header Items
		if (!headerItems.isEmpty()) {
			addHeaderItems(document);
		}

		Paragraph mainHeadingParagraph = new Paragraph(heading)
				.setFontSize(20)
				.simulateBold()
				.setMarginBottom(16);

		document.add(mainHeadingParagraph);

		ConverterProperties converterProperties = new ConverterProperties();
		converterProperties.setFontProvider(new BasicFontProvider(true, false, false));

		for (Data row : data) {
			Paragraph element = new Paragraph()
					.setFontSize(10)
					.setMarginBottom(2);

			if (row.title.isEmpty()) {
				element.add(new Text(""));
			} else {
				element.add(new Text(row.title + ": ").simulateBold())
						.add(new Text(row.value));
			}

			document.add(element);
		}

		for (Section row : sections) {
			Paragraph sectionHeading = new Paragraph(row.heading)
					.setFontSize(12)
					.simulateBold()
					.setMarginTop(8);
			document.add(sectionHeading);

			List<IElement> elements = HtmlConverter.convertToElements(row.htmlContent, converterProperties);
			for (IElement element : elements) {
				document.add((IBlockElement) element);
			}
		}

		// Metadata
		addMetadata(pdf, currentUserName);

		document.close();

		return new ByteArrayResource(outputStream.toByteArray());
	}

	// ----------------- Helper Methods -----------------

	/**
	 * Adds header items in a single line separated by |
	 */
	private void addHeaderItems(Document document) {
		Paragraph headerLine = new Paragraph()
				.setFont(normalFont)
				.setFontSize(FONT_SIZE_METADATA)
				.setFontColor(METADATA_COLOR)
				.setTextAlignment(TextAlignment.CENTER)
				.setMarginTop(0f)
				.setMarginBottom(MARGIN_HEADER_ITEMS_BOTTOM);

		for (int i = 0; i < headerItems.size(); i++) {
			headerLine.add(new Text(headerItems.get(i)));
			if (i < headerItems.size() - 1) {
				headerLine.add(new Text(" | "));
			}
		}

		document.add(headerLine);
	}

	/**
	 * Adds metadata text at the bottom of the last PDF page
	 */
	private void addMetadata(PdfDocument pdfDoc, String currentUserName) {
		int totalPages = pdfDoc.getNumberOfPages();

		for (int i = 1; i <= totalPages; i++) {
			PdfPage page = pdfDoc.getPage(i);
			Rectangle pageSize = page.getPageSize();

			Canvas canvas = new Canvas(page, pageSize);

			Paragraph metadataParagraph = new Paragraph()
					.setFont(normalFont)
					.setFontSize(FONT_SIZE_METADATA)
					.setFontColor(METADATA_COLOR)
					.setTextAlignment(TextAlignment.CENTER)
					.setWidth(pageSize.getWidth() - 8 * METADATA_MARGIN_LEFT_RIGHT);

			StringBuilder metadata = new StringBuilder();
			String currentDateTime = LocalDateTime.now().format(DATETIME_FORMATTER);
			metadata.append("This document was generated on ");
			metadata.append(currentDateTime);
			if (currentUserName != null && !currentUserName.isBlank()) {
				metadata.append(" by ").append(currentUserName);
			}
			metadata.append(" using ");
			metadataParagraph.add(new Text(metadata.toString()));

			// Add Thesis Management as a clickable Link
			Link thesisManagementLink = new Link("Thesis Management", PdfAction.createURI(THESISMANAGEMENT_URL));
			thesisManagementLink.setFontColor(PRIMARY_COLOR).setUnderline().setFont(normalFont)
					.setFontSize(FONT_SIZE_METADATA);

			metadataParagraph.add(thesisManagementLink);

			metadataParagraph.add(new Text("."));

			canvas.showTextAligned(metadataParagraph, pageSize.getWidth() / 2, MARGIN_PDF_TOP_AND_BOTTOM,
					TextAlignment.CENTER);

			// --- Page Number ---
			Paragraph pageNumber = new Paragraph(
					String.format("%s %d %s %d", "Page", i, "of", totalPages))
					.setFont(normalFont)
					.setFontSize(FONT_SIZE_METADATA)
					.setFontColor(METADATA_COLOR);

			canvas.showTextAligned(
					pageNumber,
					pageSize.getRight() - METADATA_MARGIN_LEFT_RIGHT,
					MARGIN_PDF_TOP_AND_BOTTOM,
					TextAlignment.RIGHT);

			canvas.close();
		}
	}

	/**
	 * Creates a normal font instance for use in the PDF document.
	 */
	private static PdfFont createNormalFont() {
		try {
			return PdfFontFactory.createFont(StandardFonts.HELVETICA);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create font", e);
		}
	}
}
