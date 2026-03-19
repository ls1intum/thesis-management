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
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AbstractElement;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
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

	/**
	 * Ordered list of all content blocks (HTML sections and tables),
	 * preserving the sequence in which they were added.
	 */
	private final List<ContentBlock> contentBlocks = new ArrayList<>();

	private final List<String> headerItems = new ArrayList<>();
	private final List<OverviewItem> overviewItems = new ArrayList<>();

	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	// Lazily initialized font holders, never accessed directly, use
	// getNormalFont() / getBoldFont()
	private volatile PdfFont normalFont;
	private volatile PdfFont boldFont;

	private static final String THESISMANAGEMENT_URL = "https://thesis.aet.cit.tum.de/";

	// ----------------- Colors -----------------
	private static final DeviceRgb BADGE_BACKGROUND = new DeviceRgb(0x1c, 0x7e, 0xd6);
	private static final DeviceRgb BADGE_TEXT_COLOR = new DeviceRgb(0xff, 0xff, 0xff);
	private static final DeviceRgb METADATA_COLOR = new DeviceRgb(0x8d, 0x8d, 0x8f);
	private static final DeviceRgb TABLE_HEADER_BACKGROUND = new DeviceRgb(0xf2, 0xf2, 0xf2);
	private static final DeviceRgb TABLE_BORDER_COLOR = new DeviceRgb(0xd0, 0xd0, 0xd5);
	private static final DeviceRgb TABLE_ROW_ALT_BACKGROUND = new DeviceRgb(0xfa, 0xfa, 0xfa);

	// ----------------- Font Sizes -----------------
	private static final float FONT_SIZE_BADGE = 7f;
	private static final float FONT_SIZE_MAIN_HEADING = 18f;
	private static final float FONT_SIZE_GROUP_TITLE = 14f;
	private static final float FONT_SIZE_TEXT = 10f;
	private static final float FONT_SIZE_METADATA = 7f;

	// ----------------- Spacing -----------------
	private static final float BADGE_CHAR_WIDTH_FACTOR = 0.6f;
	private static final float BADGE_PADDING_HORIZONTAL = 6f;
	private static final float BADGE_PADDING_VERTICAL = 2f;
	private static final float BADGE_BORDER_RADIUS = 8f;
	private static final float CONTENT_INDENT = 15f;
	private static final float MARGIN_PDF_TOP_AND_BOTTOM = 8f;
	private static final float MARGIN_HEADER_ITEMS_BOTTOM = 8f;
	private static final float MARGIN_TITLE_BOTTOM = 8f;
	private static final float MARGIN_OVERVIEW_SECTION_BOTTOM = 0f;
	private static final float MARGIN_DATA_ROW_BOTTOM = 6f;
	private static final float HEADER_MARGIN_BOTTOM = 16f;
	private static final float LINE_LEADING = 1.0f;
	private static final float METADATA_MARGIN_LEFT_RIGHT = 15f;
	private static final float TABLE_CELL_PADDING = 6f;
	private static final float TABLE_MARGIN_BOTTOM = 10f;
	private static final float TABLE_FIRST_COLUMN_WIDTH = 50f;

	// ----------------- Layouts -----------------
	private static final String BULLET_POINT_SYMBOL = "\u2022";
	private static final float LIST_SYMBOL_INDENT = 12f;
	private static final float TABLE_BORDER_WIDTH = 0.5f;

	// ----------------- HTML Cleanup Regexes -----------------
	private static final String HTML_TAG_REGEX = "<[^>]*>";
	private static final String NBSP_REGEX = "&nbsp;";
	private static final String NBSP_REPLACEMENT = " ";


	/** Sealed marker interface for ordered content blocks. */
	private sealed interface ContentBlock permits HtmlSection, TableBlock {
	}

	/** A titled section whose body is rendered from HTML. */
	private record HtmlSection(String heading, String htmlContent) implements ContentBlock {
	}

	/**
	 * A titled section rendered as a structured table.
	 *
	 * @param heading the section heading shown above the table
	 * @param headers column header labels (e.g.
	 *                {@code ["Name", "Weight", "Grade"]})
	 * @param rows    each inner list represents one row; its size must match
	 *                {@code headers}
	 */
	private record TableBlock(String heading, List<String> headers, List<List<TableCell>> rows, String footer)
			implements ContentBlock {
	}

	/** Represents a single cell in a table row. */
	public sealed interface TableCell permits TextCell, BadgeCell {
	}

	/** A plain text cell. */
	public record TextCell(String value) implements TableCell {
	}

	/** A cell rendered as a coloured badge, e.g. to highlight a bonus component. */
	public record BadgeCell(String label) implements TableCell {
	}

	private record OverviewItem(String title, String value) {
	}

	/**
	 * Initializes a new PDF builder with the given main heading.
	 *
	 * @param heading         the main heading for the PDF document
	 * @param currentUserName the name of the user generating the PDF, used in
	 *                        metadata
	 */
	public PDFBuilder(String heading, String currentUserName) {
		this.heading = heading;
		this.currentUserName = currentUserName;
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

	// ----------------- Content Blocks -----------------

	/**
	 * Adds a titled section with HTML content to be rendered in the PDF document.
	 *
	 * @param heading     the section heading
	 * @param htmlContent the HTML content of the section
	 * @return this PDFBuilder instance for method chaining
	 */
	public PDFBuilder addSection(String heading, String htmlContent) {
		contentBlocks.add(new HtmlSection(heading, htmlContent));
		return this;
	}

	/**
	 * Appends a titled table section.
	 *
	 * <p>
	 * Use {@link BadgeCell} for cells that should be rendered as a highlighted
	 * badge
	 * instead of plain text.
	 *
	 * @param heading the section heading shown above the table
	 * @param headers column header labels
	 * @param rows    row data; each inner list must have the same number of
	 *                elements as {@code headers}
	 * @param footer  optional text displayed below the table, or {@code null} for
	 *                none
	 * @return this builder for method chaining
	 */
	public PDFBuilder addTable(String heading, List<String> headers, List<List<TableCell>> rows, String footer) {
		contentBlocks.add(new TableBlock(heading, headers, rows, footer));
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
				.setFont(getBoldFont())
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

		for (ContentBlock block : contentBlocks) {
			switch (block) {
				case HtmlSection section -> renderHtmlSection(document, section, converterProperties);
				case TableBlock table -> renderTableBlock(document, table);
			}
		}

		addMetadata(pdf, currentUserName);

		document.close();

		return new ByteArrayResource(outputStream.toByteArray());
	}

	// ----------------- Rendering -----------------

	/**
	 * Renders a titled HTML section into the document.
	 */
	private void renderHtmlSection(Document document, HtmlSection section, ConverterProperties converterProperties) {
		Paragraph sectionHeading = new Paragraph(section.heading())
				.setFont(getBoldFont())
				.setFontSize(FONT_SIZE_GROUP_TITLE)
				.setMarginBottom(MARGIN_TITLE_BOTTOM);
		document.add(sectionHeading);

		for (IBlockElement element : parseHtmlContent(section.htmlContent(), converterProperties)) {
			if (element instanceof Paragraph para) {
				para.setMarginTop(0).setMarginLeft(CONTENT_INDENT);
				if (para.<TextAlignment>getProperty(Property.TEXT_ALIGNMENT) == null) {
					para.setTextAlignment(TextAlignment.JUSTIFIED);
				}
			} else if (element instanceof com.itextpdf.layout.element.List list) {
				list.setMarginTop(0).setMarginLeft(CONTENT_INDENT);
			}
			document.add(element);
		}
	}

	/**
	 * Renders a titled table block into the document.
	 *
	 * <p>
	 * The first row is a styled header row. Data rows alternate background
	 * colors for readability. {@link BadgeCell} entries are rendered as coloured
	 * badges.
	 */
	private void renderTableBlock(Document document, TableBlock tableBlock) {
		Paragraph sectionHeading = new Paragraph(tableBlock.heading())
				.setFont(getBoldFont())
				.setFontSize(FONT_SIZE_GROUP_TITLE)
				.setMarginBottom(MARGIN_TITLE_BOTTOM);
		document.add(sectionHeading);

		int columnCount = tableBlock.headers().size();
		float[] columnWidths = new float[columnCount];
		// First column gets more space, remaining columns share the rest equally.
		columnWidths[0] = TABLE_FIRST_COLUMN_WIDTH;
		float remaining = (columnCount > 1) ? (TABLE_FIRST_COLUMN_WIDTH / (columnCount - 1)) : TABLE_FIRST_COLUMN_WIDTH;
		for (int i = 1; i < columnCount; i++) {
			columnWidths[i] = remaining;
		}

		Table table = new Table(UnitValue.createPercentArray(columnWidths));
		table.setWidth(UnitValue.createPercentValue(100));
		table.setMarginLeft(CONTENT_INDENT);
		table.setMarginBottom(TABLE_MARGIN_BOTTOM);
		table.setBorder(Border.NO_BORDER);

		// Header row
		for (String header : tableBlock.headers()) {
			Cell headerCell = new Cell()
					.add(new Paragraph(header)
							.setFont(getBoldFont())
							.setFontSize(FONT_SIZE_TEXT)
							.setMargin(0))
					.setBackgroundColor(TABLE_HEADER_BACKGROUND)
					.setBorderTop(new SolidBorder(TABLE_BORDER_COLOR, TABLE_BORDER_WIDTH))
					.setBorderBottom(new SolidBorder(TABLE_BORDER_COLOR, TABLE_BORDER_WIDTH))
					.setBorderLeft(Border.NO_BORDER)
					.setBorderRight(Border.NO_BORDER)
					.setPadding(TABLE_CELL_PADDING)
					.setVerticalAlignment(VerticalAlignment.MIDDLE);
			table.addHeaderCell(headerCell);
		}

		// Data rows
		List<List<TableCell>> rows = tableBlock.rows();
		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
			List<TableCell> row = rows.get(rowIndex);
			boolean isAltRow = rowIndex % 2 != 0;

			for (TableCell cell : row) {
				Cell dataCell = new Cell()
						.setBorderTop(new SolidBorder(TABLE_BORDER_COLOR, TABLE_BORDER_WIDTH))
						.setBorderBottom(Border.NO_BORDER)
						.setBorderLeft(Border.NO_BORDER)
						.setBorderRight(Border.NO_BORDER)
						.setPadding(TABLE_CELL_PADDING)
						.setVerticalAlignment(VerticalAlignment.MIDDLE);

				if (isAltRow) {
					dataCell.setBackgroundColor(TABLE_ROW_ALT_BACKGROUND);
				}

				switch (cell) {
					case TextCell t -> dataCell.add(new Paragraph(t.value())
							.setFont(getNormalFont())
							.setFontSize(FONT_SIZE_TEXT)
							.setMargin(0));
					case BadgeCell b -> dataCell.add(buildBadge(b.label()));
				}

				table.addCell(dataCell);
			}
		}

		document.add(table);

		if (tableBlock.footer() != null && !tableBlock.footer().isBlank()) {
			document.add(new Paragraph(tableBlock.footer())
					.setFont(getNormalFont())
					.setFontSize(FONT_SIZE_TEXT)
					.setMarginLeft(CONTENT_INDENT)
					.setMarginTop(0f));
		}
	}

	// ----------------- Helper Methods -----------------

	/**
	 * Adds header items in a single line separated by |
	 */
	private void addHeaderItems(Document document) {
		Paragraph headerLine = new Paragraph()
				.setFont(getNormalFont())
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
				.setFont(getBoldFont())
				.setFontSize(FONT_SIZE_GROUP_TITLE)
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
					.add(new Text(item.title() + ": ").setFont(getBoldFont()).setFontSize(FONT_SIZE_TEXT))
					.add(new Text(item.value()).setFont(getNormalFont()).setFontSize(FONT_SIZE_TEXT))
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
					.setFont(getNormalFont())
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
			thesisManagementLink.setUnderline().setFont(getNormalFont())
					.setFontSize(FONT_SIZE_METADATA);

			metadataParagraph.add(thesisManagementLink);

			metadataParagraph.add(new Text("."));

			canvas.showTextAligned(metadataParagraph, pageSize.getWidth() / 2, MARGIN_PDF_TOP_AND_BOTTOM,
					TextAlignment.CENTER);

			// --- Page Number ---
			Paragraph pageNumber = new Paragraph(
					String.format("%s %d %s %d", "Page", i, "of", totalPages))
					.setFont(getNormalFont())
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
	private List<IBlockElement> parseHtmlContent(String html, ConverterProperties props) {
		List<IBlockElement> elements = new ArrayList<>();
		try {

			List<IElement> pdfElements = HtmlConverter.convertToElements(html, props);

			for (IElement element : pdfElements) {
				if (element instanceof IBlockElement block) {
					if (block instanceof Paragraph para) {
						para.setFont(getNormalFont())
								.setFontSize(FONT_SIZE_TEXT)
								.setMarginBottom(MARGIN_DATA_ROW_BOTTOM);
						fixFontProperties(para);
						// A direct import of iText's List class is required to distinguish it from
						// Java's List.
					} else if (block instanceof com.itextpdf.layout.element.List list) {
						list.setListSymbol(BULLET_POINT_SYMBOL)
								.setFont(getNormalFont())
								.setFontSize(FONT_SIZE_TEXT)
								.setMarginTop(0f)
								.setMarginBottom(MARGIN_DATA_ROW_BOTTOM)
								.setPaddingLeft(CONTENT_INDENT)
								.setSymbolIndent(LIST_SYMBOL_INDENT);

						for (IElement item : list.getChildren()) {
							if (item instanceof ListItem listItem) {
								listItem.setMarginTop(0f).setMarginBottom(0f);

								// adjust the Paragraph inside the ListItem
								for (IElement liChild : listItem.getChildren()) {
									if (liChild instanceof Paragraph para) {
										para.setFont(getNormalFont())
												.setFontSize(FONT_SIZE_TEXT)
												.setMultipliedLeading(1f)
												.setMarginTop(0f)
												.setMarginBottom(0f);
									}
								}
							}
						}
						fixFontProperties(list);
					}

					elements.add(block);
				}
			}
		} catch (Exception e) {
			String plain = html.replaceAll(HTML_TAG_REGEX, "").replaceAll(NBSP_REGEX, NBSP_REPLACEMENT).trim();
			elements.add(new Paragraph(plain).setFont(getNormalFont()).setFontSize(FONT_SIZE_TEXT)
					.setMarginBottom(MARGIN_DATA_ROW_BOTTOM).setMultipliedLeading(LINE_LEADING));
		}
		return elements;
	}

	/**
	 * Builds a small inline badge with a blue background and white bold text.
	 *
	 * @param label the text to display inside the badge
	 * @return a table element sized to fit the label
	 */
	private Table buildBadge(String label) {
		// Calculate badge width based on label length, font size, and padding
		float width = (label.length() * FONT_SIZE_BADGE * BADGE_CHAR_WIDTH_FACTOR) + (BADGE_PADDING_HORIZONTAL * 2);

		Table badgeTable = new Table(1);
		badgeTable.setWidth(UnitValue.createPointValue(width));
		badgeTable.setBorder(Border.NO_BORDER);

		Cell badgeCell = new Cell()
				.add(new Paragraph(label)
						.setFont(getBoldFont())
						.setFontSize(FONT_SIZE_BADGE)
						.setFontColor(BADGE_TEXT_COLOR)
						.setTextAlignment(TextAlignment.CENTER)
						.setMargin(0))
				.setBackgroundColor(BADGE_BACKGROUND)
				.setBorderRadius(new BorderRadius(BADGE_BORDER_RADIUS))
				.setBorder(Border.NO_BORDER)
				.setPaddingLeft(BADGE_PADDING_HORIZONTAL)
				.setPaddingRight(BADGE_PADDING_HORIZONTAL)
				.setPaddingTop(BADGE_PADDING_VERTICAL)
				.setPaddingBottom(BADGE_PADDING_VERTICAL);

		badgeTable.addCell(badgeCell);
		return badgeTable;
	}

	/**
	 * Recursively enforces font size on all child elements,
	 * while preserving bold/italic font variants set by HtmlConverter.
	 */
	private void fixFontProperties(IElement element) {
		if (element instanceof Text text) {
			text.setFontSize(FONT_SIZE_TEXT);
			// Preserve bold/italic by not overriding the font itself
		} else if (element instanceof AbstractElement<?> container) {
			container.setFontSize(FONT_SIZE_TEXT);
			for (IElement child : container.getChildren()) {
				fixFontProperties(child);
			}
		}
	}

	/**
	 * Returns the shared normal (Helvetica) font, initializing it on first use.
	 * Falls back to Helvetica-Bold if the standard font cannot be loaded.
	 */
	private PdfFont getNormalFont() {
		if (normalFont == null) {
			synchronized (PDFBuilder.class) {
				if (normalFont == null) {
					normalFont = loadFont(StandardFonts.HELVETICA, StandardFonts.HELVETICA_BOLD);
				}
			}
		}
		return normalFont;
	}

	/**
	 * Returns the shared bold (Helvetica-Bold) font, initializing it on first use.
	 * Falls back to the normal font if the bold variant cannot be loaded.
	 */
	private PdfFont getBoldFont() {
		if (boldFont == null) {
			synchronized (PDFBuilder.class) {
				if (boldFont == null) {
					boldFont = loadFont(StandardFonts.HELVETICA_BOLD, StandardFonts.HELVETICA);
				}
			}
		}
		return boldFont;
	}

	/**
	 * Attempts to create a PdfFont for {@code preferredStandardFont}; on failure
	 * falls back to {@code fallbackStandardFont}; if that also fails a
	 * RuntimeException is thrown so the caller receives a clear error rather than a
	 * silent {@code null} reference.
	 */
	private static PdfFont loadFont(String preferredStandardFont, String fallbackStandardFont) {
		try {
			return PdfFontFactory.createFont(preferredStandardFont);
		} catch (IOException primary) {
			try {
				return PdfFontFactory.createFont(fallbackStandardFont);
			} catch (IOException fallback) {
				throw new RuntimeException(
						"Failed to load both preferred font '" + preferredStandardFont
								+ "' and fallback font '" + fallbackStandardFont + "'",
						fallback);
			}
		}
	}
}
