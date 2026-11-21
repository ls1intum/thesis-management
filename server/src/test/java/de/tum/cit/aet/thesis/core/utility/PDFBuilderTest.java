package de.tum.cit.aet.thesis.core.utility;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.core.utility.PDFBuilder.BadgeCell;
import de.tum.cit.aet.thesis.core.utility.PDFBuilder.TableCell;
import de.tum.cit.aet.thesis.core.utility.PDFBuilder.TextCell;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.List;

class PDFBuilderTest {

	@Test
	void badgeCell_recordExposesLabel() {
		BadgeCell cell = new BadgeCell("Bonus");
		assertThat(cell.label()).isEqualTo("Bonus");
	}

	@Test
	void textCell_recordExposesValue() {
		TextCell cell = new TextCell("plain");
		assertThat(cell.value()).isEqualTo("plain");
	}

	@Test
	void build_minimalDocument_producesValidPdfBytes() throws Exception {
		PDFBuilder builder = new PDFBuilder("Heading", "Anna Tester", "https://example.com");

		Resource resource = builder
				.addHeaderItem("Header A")
				.addOverviewItem("Field", "Value")
				.addSection("Section 1", "<p>Hello <strong>World</strong></p>")
				.build();

		assertThat(resource).isNotNull();
		try (InputStream is = resource.getInputStream()) {
			byte[] bytes = is.readAllBytes();
			assertThat(bytes.length).isGreaterThan(0);
			assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
		}
	}

	@Test
	void build_documentWithTable_includesAllCellTypes() throws Exception {
		PDFBuilder builder = new PDFBuilder("Tabular", "Anna Tester", "https://example.com");

		List<TableCell> row1 = List.of(new TextCell("Item 1"), new BadgeCell("Bonus"));
		List<TableCell> row2 = List.of(new TextCell("Item 2"), new TextCell("Plain"));

		Resource resource = builder
				.addTable("Grades", List.of("Component", "Type"), List.of(row1, row2), "Total: 1.0")
				.build();

		assertThat(resource).isNotNull();
		try (InputStream is = resource.getInputStream()) {
			byte[] bytes = is.readAllBytes();
			assertThat(bytes.length).isGreaterThan(0);
			assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
		}
	}

	@Test
	void build_documentWithoutHeaderOrOverviewOrTableFooter_doesNotThrow() throws Exception {
		PDFBuilder builder = new PDFBuilder("Empty", "Anna Tester", "https://example.com");

		Resource resource = builder
				.addTable("Only Table", List.of("Header"), List.of(List.of(new TextCell("a"))), null)
				.build();

		assertThat(resource).isNotNull();
	}
}
