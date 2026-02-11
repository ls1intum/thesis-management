package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.util.Set;

/** Utility class providing static validation methods for request parameters such as strings, sets, and emails. */
public class RequestValidator {
	/**
	 * Validates that the string is non-null and does not exceed the specified maximum length.
	 *
	 * @param value the string to validate
	 * @param maxLength the maximum allowed length
	 * @return the validated string
	 */
	public static String validateStringMaxLength(String value, int maxLength) {
		if (value == null) {
			throw new ResourceInvalidParametersException("Required string is null");
		}

		if (value.length() > maxLength) {
			throw new ResourceInvalidParametersException("String exceeds maximum length of " + maxLength + " characters");
		}

		return value;
	}

	/**
	 * Validates the string's maximum length, returning null if the value is null or empty.
	 *
	 * @param value the string to validate
	 * @param maxLength the maximum allowed length
	 * @return the validated string or null
	 */
	public static String validateStringMaxLengthAllowNull(String value, int maxLength) {
		if (value == null || value.isEmpty()) {
			return null;
		}

		return validateStringMaxLength(value, maxLength);
	}

	/**
	 * Validates that the set is non-null and that each string item does not exceed the specified maximum length.
	 *
	 * @param value the set of strings to validate
	 * @param maxLength the maximum allowed length per item
	 * @return the validated set
	 */
	public static Set<String> validateStringSetItemMaxLength(Set<String> value, int maxLength) {
		if (value == null) {
			throw new ResourceInvalidParametersException("Required set is null");
		}

		for (String s : value) {
			if (s.length() > maxLength) {
				throw new ResourceInvalidParametersException("String exceeds maximum length of " + maxLength + " characters");
			}
		}

		return value;
	}

	/**
	 * Validates string item max length for each set element, returning null if the set is null.
	 *
	 * @param value the set of strings to validate
	 * @param maxLength the maximum allowed length per item
	 * @return the validated set or null
	 */
	public static Set<String> validateStringSetItemMaxLengthAllowNull(Set<String> value, int maxLength) {
		if (value == null) {
			return null;
		}

		return validateStringSetItemMaxLength(value, maxLength);
	}

	/**
	 * Validates that the value is a non-null, well-formed email address within 200 characters.
	 *
	 * @param value the email address to validate
	 * @return the validated email address
	 */
	public static String validateEmail(String value) {
		if (value == null || value.length() > 200) {
			throw new ResourceInvalidParametersException("Required email is missing");
		}

		try {
			InternetAddress emailAddr = new InternetAddress(value);

			emailAddr.validate();

			return value;
		} catch (AddressException ex) {
			throw new ResourceInvalidParametersException("Invalid email address");
		}
	}

	/**
	 * Validates the email format, returning null if the value is null or empty.
	 *
	 * @param value the email address to validate
	 * @return the validated email address or null
	 */
	public static String validateEmailAllowNull(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}

		return validateEmail(value);
	}

	/**
	 * Validates that the given value is non-null, throwing an exception otherwise.
	 *
	 * @param value the value to validate
	 * @param <T> the type of the value
	 * @return the validated non-null value
	 */
	public static <T> T validateNotNull(T value) {
		if (value == null) {
			throw new ResourceInvalidParametersException("Required value is null");
		}

		return value;
	}
}
