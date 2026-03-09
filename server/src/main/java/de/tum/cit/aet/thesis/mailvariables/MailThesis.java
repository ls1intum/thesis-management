package de.tum.cit.aet.thesis.mailvariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.utility.DataFormatter;

import java.util.List;

/** Mail placeholder model for thesis-related variables. */
public record MailThesis(
		String title,
		String type,
		String students,
		String examiners,
		String supervisors,
		String abstractText,
		String finalGrade,
		String finalFeedback
) {
	/**
	 * Builds a thesis mail model without final grade values.
	 *
	 * @param thesis the thesis entity
	 * @return mapped thesis mail model
	 */
	public static MailThesis fromThesis(Thesis thesis) {
		return fromThesis(thesis, false);
	}

	/**
	 * Builds a thesis mail model including final grade values.
	 *
	 * @param thesis the thesis entity
	 * @return mapped thesis mail model including grade fields
	 */
	public static MailThesis fromThesisWithGrade(Thesis thesis) {
		return fromThesis(thesis, true);
	}

	private static MailThesis fromThesis(Thesis thesis, boolean includeFinalGrade) {
		if (thesis == null) {
			return new MailThesis("", "", "", "", "", "", "", "");
		}

		return new MailThesis(
				valueOrEmpty(thesis.getTitle()),
				valueOrEmpty(DataFormatter.formatConstantName(thesis.getType())),
				valueOrEmpty(formatUsers(thesis.getStudents())),
				valueOrEmpty(formatUsers(thesis.getExaminers())),
				valueOrEmpty(formatUsers(thesis.getSupervisors())),
				valueOrEmpty(thesis.getAbstractField()),
				includeFinalGrade ? valueOrEmpty(thesis.getFinalGrade()) : "",
				includeFinalGrade ? valueOrEmpty(thesis.getFinalFeedback()) : ""
		);
	}

	/**
	 * Returns all selectable template variables for thesis lifecycle templates.
	 *
	 * @return thesis variable descriptors
	 */
	public static List<MailVariableDto> templateVariables() {
		return List.of(
				new MailVariableDto("Thesis Title", "[[${thesis.title}]]", "Deep Learning for NLP", "Thesis"),
				new MailVariableDto("Thesis Type", "[[${thesis.type}]]", "Bachelor Thesis", "Thesis"),
				new MailVariableDto("Thesis Student(s)", "[[${thesis.students}]]", "Max Mustermann", "Thesis"),
				new MailVariableDto("Thesis Examiner(s)", "[[${thesis.examiners}]]", "Maria Musterfrau", "Thesis"),
				new MailVariableDto("Thesis Supervisor(s)", "[[${thesis.supervisors}]]", "Alex Example", "Thesis"),
				new MailVariableDto("Thesis Abstract", "<span th:utext=\"${thesis.abstractText}\"></span>", "Abstract text", "Thesis"),
				new MailVariableDto("Thesis URL", "[[${thesisUrl}]]", "https://thesis-management.com/theses/123", "Thesis")
		);
	}

	/**
	 * Returns all selectable template variables for thesis grade templates.
	 *
	 * @return thesis grade variable descriptors
	 */
	public static List<MailVariableDto> gradeTemplateVariables() {
		return List.of(
				new MailVariableDto("Thesis Final Grade", "[[${thesis.finalGrade}]]", "1.3", "Thesis"),
				new MailVariableDto("Thesis Final Feedback", "[[${thesis.finalFeedback}]]", "Great work", "Thesis")
		);
	}

	private static String formatUsers(List<User> users) {
		if (users == null || users.isEmpty()) {
			return "";
		}

		return users.stream()
				.map(user -> (valueOrEmpty(user.getFirstName()) + " " + valueOrEmpty(user.getLastName())).trim())
				.filter(name -> !name.isBlank())
				.reduce((left, right) -> left + " and " + right)
				.orElse("");
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
