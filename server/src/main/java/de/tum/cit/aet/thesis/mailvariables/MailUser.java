package de.tum.cit.aet.thesis.mailvariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.User;

import java.util.List;

/** Mail placeholder model for user variables. */
public record MailUser(
		String firstName,
		String lastName
) {
	/**
	 * Builds a mail-safe user model.
	 *
	 * @param user the user entity
	 * @return mapped user mail model
	 */
	public static MailUser fromUser(User user) {
		if (user == null) {
			return new MailUser("", "");
		}

		return new MailUser(valueOrEmpty(user.getFirstName()), valueOrEmpty(user.getLastName()));
	}

	/**
	 * Returns selectable template variables for a configurable user placeholder prefix.
	 *
	 * @param placeholder the placeholder root key
	 * @param labelPrefix the label prefix shown in the editor
	 * @param group the variable group label
	 * @return user variable descriptors
	 */
	public static List<MailVariableDto> templateVariables(String placeholder, String labelPrefix, String group) {
		return List.of(
				new MailVariableDto(labelPrefix + " First Name", "[[${" + placeholder + ".firstName}]]", "Max", group),
				new MailVariableDto(labelPrefix + " Last Name", "[[${" + placeholder + ".lastName}]]", "Mustermann", group)
		);
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
