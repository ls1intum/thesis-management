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
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
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

	private final List<String> headerItems = new ArrayList<>();
	private final List<OverviewItem> overviewItems = new ArrayList<>();

	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static final PdfFont normalFont = createNormalFont();
	private static final PdfFont boldFont = createBoldFont();
	private static final String THESISMANAGEMENT_URL = "https://thesis.aet.cit.tum.de/";

	// ----------------- Colors -----------------
	private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(0x50, 0x4c, 0x97);
	private static final DeviceRgb METADATA_COLOR = new DeviceRgb(0x8d, 0x8d, 0x8f);

	// ----------------- Font Sizes -----------------
	private static final float FONT_SIZE_MAIN_HEADING = 18f;
	private static final float FONT_SIZE_GROUP_TITLE = 14f;
	private static final float FONT_SIZE_TEXT = 10f;
	private static final float FONT_SIZE_METADATA = 7f;

	// ----------------- Spacing -----------------
	private static final float CONTENT_INDENT = 15f;
	private static final float MARGIN_PDF_TOP_AND_BOTTOM = 8f;
	private static final float MARGIN_HEADER_ITEMS_BOTTOM = 8f;
	private static final float MARGIN_TITLE_BOTTOM = 8f;
	private static final float MARGIN_OVERVIEW_SECTION_BOTTOM = 0f;
	private static final float MARGIN_DATA_ROW_BOTTOM = 6f;
	private static final float HEADER_MARGIN_BOTTOM = 16f;
	private static final float LINE_LEADING = 1.0f;
	private static final float METADATA_MARGIN_LEFT_RIGHT = 15f;

	// ----------------- List & Text Layout -----------------
	private static final String BULLET_POINT_SYMBOL = "\u2022";
	private static final float LIST_SYMBOL_INDENT = 12f;

	private record OverviewItem(String title, String value) {
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
	 * Adds a key-value pair to be rendered in the PDF document.
	 *
	 * @param title the data label
	 * @param value the data value
	 * @return this PDFBuilder instance for method chaining
	 */
	public PDFBuilder addOverviewItem(String title, String value) {
		overviewItems.add(new OverviewItem(title, value));
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

		// Main Heading
		Paragraph mainHeadingParagraph = new Paragraph(heading)
				.setFont(boldFont)
				.setFontColor(PRIMARY_COLOR)
				.setFontSize(FONT_SIZE_MAIN_HEADING)
				.setTextAlignment(TextAlignment.CENTER)
				.setMarginBottom(HEADER_MARGIN_BOTTOM);
		document.add(mainHeadingParagraph);

		ConverterProperties converterProperties = new ConverterProperties();
		converterProperties.setFontProvider(new BasicFontProvider(true, false, false));

		// Overview Section
		if (!overviewItems.isEmpty()) {
			addOverviewSection(document);
		}

		for (Section row : sections) {
			Paragraph sectionHeading = new Paragraph(row.heading)
					.setFont(boldFont)
					.setFontSize(FONT_SIZE_GROUP_TITLE)
					.setFontColor(PRIMARY_COLOR)
					.setMarginBottom(MARGIN_TITLE_BOTTOM);
			document.add(sectionHeading);

			for (IBlockElement element : parseHtmlContent(row.htmlContent())) {
				if (element instanceof Paragraph para) {
					para.setMarginTop(0).setMarginLeft(CONTENT_INDENT);
				} else if (element instanceof com.itextpdf.layout.element.List list) {
					list.setMarginTop(0).setMarginLeft(CONTENT_INDENT);
				}
				document.add(element);
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
	 * Renders and adds the overview section to the PDF document.
	 * The section consists of a title and a two-column table of overview items.
	 * If the number of items is odd, the last entry spans both columns.
	 */
	private void addOverviewSection(Document document) {
		Div container = new Div().setMarginBottom(MARGIN_OVERVIEW_SECTION_BOTTOM);

		Paragraph title = new Paragraph("Overview")
				.setFont(boldFont)
				.setFontSize(FONT_SIZE_GROUP_TITLE)
				.setFontColor(PRIMARY_COLOR)
				.setMarginBottom(MARGIN_OVERVIEW_SECTION_BOTTOM);
		container.add(title);

		// Create and populate overview items table
		Table table = new Table(2);
		table.setWidth(UnitValue.createPercentValue(100));
		table.setBorder(Border.NO_BORDER);
		table.setMarginLeft(CONTENT_INDENT);

		for (int i = 0; i < overviewItems.size(); i++) {
			OverviewItem item = overviewItems.get(i);

			Paragraph cellContent = new Paragraph()
					.add(new Text(item.title + ": ").setFont(boldFont).setFontSize(FONT_SIZE_TEXT))
					.add(new Text(item.value).setFont(normalFont).setFontSize(FONT_SIZE_TEXT))
					.setMargin(0);

			Cell cell;
			if (i == overviewItems.size() - 1 && overviewItems.size() % 2 == 1) {
				cell = new Cell(1, 2);
			} else {
				cell = new Cell();
			}

			cell.add(cellContent).setBorder(Border.NO_BORDER).setPaddingRight(10f);

			table.addCell(cell);
		}

		container.add(table);

		document.add(container);
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
	 * Converts HTML content into styled iText block elements.
	 * If conversion fails, the content is rendered as plain text.
	 */
	private List<IBlockElement> parseHtmlContent(String html) {
		List<IBlockElement> elements = new ArrayList<>();
		try {
			String processedHtml = html.replaceAll("<ol>", "<ul>").replaceAll("</ol>", "</ul>");

			ConverterProperties props = new ConverterProperties();

			List<IElement> pdfElements = HtmlConverter.convertToElements(processedHtml, props);

			for (IElement element : pdfElements) {
				if (element instanceof IBlockElement block) {
					if (block instanceof Paragraph para) {
						para.setFont(normalFont)
								.setFontSize(FONT_SIZE_TEXT)
								.setMarginBottom(MARGIN_DATA_ROW_BOTTOM)
								.setMultipliedLeading(LINE_LEADING);
						// A direct import of iText's List class is required to distinguish it from
						// Java's List.
					} else if (block instanceof com.itextpdf.layout.element.List list) {
						list.setListSymbol(BULLET_POINT_SYMBOL)
								.setFont(normalFont)
								.setFontSize(FONT_SIZE_TEXT)
								.setMarginBottom(MARGIN_DATA_ROW_BOTTOM)
								.setPaddingLeft(0f)
								.setSymbolIndent(LIST_SYMBOL_INDENT);
						for (IElement item : list.getChildren()) {
							if (item instanceof ListItem listItem) {
								listItem.setFont(normalFont).setFontSize(FONT_SIZE_TEXT).setMarginLeft(CONTENT_INDENT);
							}
						}
					}
					elements.add(block);
				}
			}
		} catch (Exception e) {
			String plain = html.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
			elements.add(new Paragraph(plain).setFont(normalFont).setFontSize(FONT_SIZE_TEXT)
					.setMarginBottom(MARGIN_DATA_ROW_BOTTOM).setMultipliedLeading(LINE_LEADING));
		}
		return elements;
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

	/**
	 * Creates a bold font instance for use in the PDF document.
	 */
	private static PdfFont createBoldFont() {
		try {
			return PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create bold font", e);
		}
	}
}
