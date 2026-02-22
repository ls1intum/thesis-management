package de.tum.cit.aet.thesis.utility;

import static org.junit.jupiter.api.Assertions.*;

import de.tum.cit.aet.thesis.dto.LightUserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

class DataFormatterTest {
	private enum TestEnum {
		FIRST_VALUE,
		SECOND_VALUE,
		SIMPLE
	}

	private LightUserDto createTestUser(String firstName, String lastName) {
		return new LightUserDto(
				UUID.randomUUID(),
				"avatar.jpg",
				"uni123",
				"m12345",
				firstName,
				lastName,
				firstName.toLowerCase() + "@example.com",
				"Bachelor",
				"Computer Science",
				new HashMap<>(),
				Instant.now(),
				Arrays.asList("admin", "supervisor")
		);
	}

	@Test
	void formatDate_WithValidInstant_ReturnsFormattedDate() {
		Instant instant = Instant.parse("2024-01-15T10:00:00Z");
		String expected = DateTimeFormatter.ofPattern("dd.MM.yyyy")
				.withZone(ZoneId.systemDefault())
				.format(instant);

		String result = DataFormatter.formatDate(instant);

		assertEquals(expected, result);
	}

	@Test
	void formatDate_WithNullValue_ReturnsEmptyString() {
		String result = DataFormatter.formatDate(null);

		assertEquals("", result);
	}

	@Test
	void formatDateTime_WithValidInstant_ReturnsFormattedDateTime() {
		Instant instant = Instant.parse("2024-01-15T10:00:00Z");
		String expected = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z")
				.withZone(ZoneId.systemDefault())
				.format(instant);

		String result = DataFormatter.formatDateTime(instant);

		assertEquals(expected, result);
	}

	@Test
	void formatDateTime_WithNullValue_ReturnsEmptyString() {
		String result = DataFormatter.formatDateTime(null);

		assertEquals("", result);
	}

	@Test
	void formatEnum_WithValidEnum_ReturnsFormattedString() {
		TestEnum enumValue = TestEnum.FIRST_VALUE;

		String result = DataFormatter.formatEnum(enumValue);

		assertEquals("First Value", result);
	}

	@Test
	void formatEnum_WithNullValue_ReturnsEmptyString() {
		String result = DataFormatter.formatEnum(null);

		assertEquals("", result);
	}

	@Test
	void formatUsers_WithValidUserList_ReturnsFormattedString() {
		List<LightUserDto> users = Arrays.asList(
				createTestUser("John", "Doe"),
				createTestUser("Jane", "Smith")
		);

		String result = DataFormatter.formatUsers(users);

		assertEquals("John Doe and Jane Smith", result);
	}

	@Test
	void formatUsers_WithSingleUser_ReturnsFormattedString() {
		List<LightUserDto> users = Collections.singletonList(
				createTestUser("John", "Doe")
		);

		String result = DataFormatter.formatUsers(users);

		assertEquals("John Doe", result);
	}

	@Test
	void formatUsers_WithEmptyList_ReturnsEmptyString() {
		List<LightUserDto> emptyList = Collections.emptyList();

		String result = DataFormatter.formatUsers(emptyList);

		assertEquals("", result);
	}

	@Test
	void formatUsers_WithNullInput_ReturnsEmptyString() {
		String result = DataFormatter.formatUsers(null);

		assertEquals("", result);
	}

	@ParameterizedTest
	@MethodSource("provideConstantNameTestCases")
	void formatConstantName_WithVariousInputs_ReturnsExpectedResults(String input, String expected) {
		String result = DataFormatter.formatConstantName(input);

		assertEquals(expected, result);
	}

	private static Stream<Arguments> provideConstantNameTestCases() {
		return Stream.of(
				Arguments.of("HELLO_WORLD", "Hello World"),
				Arguments.of("SIMPLE_TEXT", "Simple Text"),
				Arguments.of("SINGLE", "Single"),
				Arguments.of("", ""),
				Arguments.of("___", ""),
				Arguments.of("MULTIPLE_WORD_TEST", "Multiple Word Test"),
				Arguments.of("CAMEL_CASE_TEST", "Camel Case Test")
		);
	}

	@Test
	void formatConstantName_WithNullValue_ReturnsEmptyString() {
		String result = DataFormatter.formatConstantName(null);

		assertEquals("", result);
	}

	@ParameterizedTest
	@MethodSource("provideOptionalStringTestCases")
	void formatOptionalString_WithVariousInputs_ReturnsExpectedResults(String input, String expected) {
		String result = DataFormatter.formatOptionalString(input);

		assertEquals(expected, result);
	}

	private static Stream<Arguments> provideOptionalStringTestCases() {
		return Stream.of(
				Arguments.of("Valid Text", "Valid Text"),
				Arguments.of("", "Not available"),
				Arguments.of("   ", "Not available"),
				Arguments.of(null, "Not available")
		);
	}

	@Test
	void formatSemester_WithValidInstant_ReturnsExpectedSemester() {
		Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
		String result = DataFormatter.formatSemester(oneYearAgo);
		assertEquals("2", result, "365 days / 182 days per semester = 2");
	}

	@Test
	void formatSemester_WithNullValue_ReturnsEmptyString() {
		String result = DataFormatter.formatSemester(null);

		assertEquals("", result);
	}
}
