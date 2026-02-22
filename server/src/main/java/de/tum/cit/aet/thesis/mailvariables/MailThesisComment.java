package de.tum.cit.aet.thesis.mailvariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.ThesisComment;
import de.tum.cit.aet.thesis.entity.User;

import java.util.List;

/** Mail placeholder model for thesis comment variables. */
public record MailThesisComment(
		String creatorFirstName,
		String creatorLastName,
		String message
) {
	/**
	 * Builds a mail-safe thesis comment model.
	 *
	 * @param comment the thesis comment entity
	 * @return mapped thesis comment mail model
	 */
	public static MailThesisComment fromComment(ThesisComment comment) {
		if (comment == null) {
			return new MailThesisComment("", "", "");
		}

		User creator = comment.getCreatedBy();

		return new MailThesisComment(
				valueOrEmpty(creator != null ? creator.getFirstName() : null),
				valueOrEmpty(creator != null ? creator.getLastName() : null),
				valueOrEmpty(comment.getMessage())
		);
	}

	/**
	 * Returns all selectable template variables for thesis comments.
	 *
	 * @return thesis comment variable descriptors
	 */
	public static List<MailVariableDto> templateVariables() {
		return List.of(
				new MailVariableDto("Comment Creator First Name", "[[${comment.creatorFirstName}]]", "Max", "Thesis Comment"),
				new MailVariableDto("Comment Creator Last Name", "[[${comment.creatorLastName}]]", "Mustermann", "Thesis Comment"),
				new MailVariableDto("Comment Message", "[[${comment.message}]]", "Comment text", "Thesis Comment")
		);
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
