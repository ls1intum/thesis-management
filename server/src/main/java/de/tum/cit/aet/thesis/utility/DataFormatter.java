package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.dto.LightUserDto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides static helper methods for formatting dates, enums, user names, and other data types into display strings.
 */
public class DataFormatter {
	/**
	 * Formats an Instant value into a date string with the pattern dd.MM.yyyy.
	 *
	 * @param time the Instant value to format
	 * @return the formatted date string
	 */
	public static String formatDate(Object time) {
		if (!(time instanceof Instant)) {
			return "";
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
				.withZone(ZoneId.systemDefault());

		return formatter.format((Instant) time);
	}

	/**
	 * Formats an Instant value into a date-time string with the pattern dd.MM.yyyy HH:mm:ss z.
	 *
	 * @param time the Instant value to format
	 * @return the formatted date-time string
	 */
	public static String formatDateTime(Object time) {
		if (!(time instanceof Instant)) {
			return "";
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z")
				.withZone(ZoneId.systemDefault());

		return formatter.format((Instant) time);
	}

	/**
	 * Formats an enum constant into a human-readable capitalized string.
	 *
	 * @param value the enum constant to format
	 * @return the formatted string representation
	 */
	public static String formatEnum(Object value) {
		if (value == null || !value.getClass().isEnum()) {
			return "";
		}

		return formatConstantName(((Enum<?>) value).name());
	}

	/**
	 * Formats a list of LightUserDto objects into a string of full names joined by "and".
	 *
	 * @param value the list of LightUserDto objects
	 * @return the formatted user names string
	 */
	public static String formatUsers(Object value) {
		List<LightUserDto> users = new ArrayList<>();

		if (value instanceof List) {
			for (Object element : (List<?>) value) {
				if (element instanceof LightUserDto) {
					users.add((LightUserDto) element);
				}
			}
		}

		return String.join(" and ", users.stream().map(user -> user.firstName() + " " + user.lastName()).toList());
	}

	/**
	 * Converts an underscore-separated constant name into a capitalized, space-separated string.
	 *
	 * @param value the constant name to format
	 * @return the formatted string
	 */
	public static String formatConstantName(Object value) {
		if (!(value instanceof String text)) {
			return "";
		}

		String[] words = text.split("_");
		StringBuilder capitalizedSentence = new StringBuilder();

		for (String word : words) {
			if (!word.isEmpty()) {
				capitalizedSentence.append(Character.toUpperCase(word.charAt(0)))
						.append(word.substring(1).toLowerCase())
						.append(" ");
			}
		}

		return capitalizedSentence.toString().trim();
	}

	/**
	 * Returns the string value if non-blank, or "Not available" as a fallback.
	 *
	 * @param value the value to format
	 * @return the string value or "Not available"
	 */
	public static String formatOptionalString(Object value) {
		if (!(value instanceof String text)) {
			return "Not available";
		}

		if (text.isBlank()) {
			return "Not available";
		}

		return text;
	}

	/**
	 * Calculates the approximate number of semesters elapsed since the given Instant.
	 *
	 * @param value the Instant representing the start date
	 * @return the approximate number of semesters as a string
	 */
	public static String formatSemester(Object value) {
		if (!(value instanceof Instant)) {
			return "";
		}

		long monthsBetween = ChronoUnit.DAYS.between((Instant) value, Instant.now());

		return String.valueOf(monthsBetween / (365 / 2));
	}
}
