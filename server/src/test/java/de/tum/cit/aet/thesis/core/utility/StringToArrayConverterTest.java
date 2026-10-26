package de.tum.cit.aet.thesis.core.utility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringToArrayConverterTest {
	private final StringToArrayConverter converter = new StringToArrayConverter();

	@Test
	void convert_emptyString_returnsEmptyArray() {
		assertEquals(0, converter.convert("").length);
	}

	@Test
	void convert_singleValue_returnsSingleElement() {
		assertArrayEquals(new String[] { "foo" }, converter.convert("foo"));
	}

	@Test
	void convert_commaSeparated_returnsAllElements() {
		assertArrayEquals(new String[] { "a", "b", "c" }, converter.convert("a,b,c"));
	}

	@Test
	void convert_emptyEntriesFilteredOut() {
		assertArrayEquals(new String[] { "a", "b" }, converter.convert("a,,b,"));
	}

	@Test
	void convert_onlyDelimiters_returnsEmptyArray() {
		assertEquals(0, converter.convert(",,,").length);
	}
}
