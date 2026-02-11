package de.tum.cit.aet.thesis.utility;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.styledxmlparser.resolver.font.BasicFontProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Fluent builder for generating PDF documents with a heading, key-value data pairs, and HTML content sections. */
public class PDFBuilder {
	private final String heading;
	private final List<Section> sections;
	private final List<Data> data;

	private record Data(String title, String value) { }
	private record Section(String heading, String htmlContent) { }

	/**
	 * Initializes a new PDF builder with the given main heading.
	 *
	 * @param heading the main heading for the PDF document
	 */
	public PDFBuilder(String heading) {
		this.heading = heading;
		this.sections = new ArrayList<>();
		this.data = new ArrayList<>();
	}

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

	/**
	 * Adds a titled section with HTML content to be rendered in the PDF document.
	 *
	 * @param heading the section heading
	 * @param htmlContent the HTML content of the section
	 * @return this PDFBuilder instance for method chaining
	 */
	public PDFBuilder addSection(String heading, String htmlContent) {
		sections.add(new Section(heading, htmlContent));

		return this;
	}

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

		document.close();

		return new ByteArrayResource(outputStream.toByteArray());
	}
}
