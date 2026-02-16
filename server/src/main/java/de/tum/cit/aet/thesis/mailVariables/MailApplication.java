package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.utility.DataFormatter;

import java.util.List;

public record MailApplication(
		String thesisTitle,
		String applicantFirstName,
		String applicantLastName,
		String applicantEmail,
		String applicantUniversityId,
		String applicantMatriculationNumber,
		String studyProgram,
		String studyDegree,
		String semester,
		String desiredStartDate,
		String motivation,
		String specialSkills,
		String interests,
		String projects
) {
	public static MailApplication fromApplication(Application application) {
		if (application == null) {
			return new MailApplication("", "", "", "", "", "", "", "", "", "", "", "", "", "");
		}

		User applicant = application.getUser();

		return new MailApplication(
				valueOrEmpty(resolveThesisTitle(application)),
				valueOrEmpty(applicant != null ? applicant.getFirstName() : null),
				valueOrEmpty(applicant != null ? applicant.getLastName() : null),
				valueOrEmpty(applicant != null && applicant.getEmail() != null ? applicant.getEmail().getAddress() : null),
				valueOrEmpty(applicant != null ? applicant.getUniversityId() : null),
				valueOrEmpty(applicant != null ? applicant.getMatriculationNumber() : null),
				valueOrEmpty(DataFormatter.formatConstantName(applicant != null ? applicant.getStudyProgram() : null)),
				valueOrEmpty(DataFormatter.formatConstantName(applicant != null ? applicant.getStudyDegree() : null)),
				valueOrEmpty(DataFormatter.formatSemester(applicant != null ? applicant.getEnrolledAt() : null)),
				valueOrEmpty(DataFormatter.formatDate(application.getDesiredStartDate())),
				valueOrEmpty(application.getMotivation()),
				valueOrEmpty(applicant != null ? applicant.getSpecialSkills() : null),
				valueOrEmpty(applicant != null ? applicant.getInterests() : null),
				valueOrEmpty(applicant != null ? applicant.getProjects() : null)
		);
	}

	public static List<MailVariableDto> templateVariables() {
		return List.of(
				new MailVariableDto("Thesis Title", "[[${application.thesisTitle}]]", "Deep Learning for NLP", "Application"),
				new MailVariableDto("Applicant First Name", "[[${application.applicantFirstName}]]", "Max", "Application"),
				new MailVariableDto("Applicant Last Name", "[[${application.applicantLastName}]]", "Mustermann", "Application"),
				new MailVariableDto("Applicant Email", "[[${application.applicantEmail}]]", "max.mustermann@test.de", "Application"),
				new MailVariableDto("Applicant University ID", "[[${application.applicantUniversityId}]]", "ge47zig", "Application"),
				new MailVariableDto("Applicant Matriculation Number", "[[${application.applicantMatriculationNumber}]]", "12345678", "Application"),
				new MailVariableDto("Applicant Study Program", "[[${application.studyProgram}]]", "Informatics", "Application"),
				new MailVariableDto("Applicant Study Degree", "[[${application.studyDegree}]]", "Bachelor", "Application"),
				new MailVariableDto("Applicant Semester", "[[${application.semester}]]", "3", "Application"),
				new MailVariableDto("Desired Start Date", "[[${application.desiredStartDate}]]", "01.10.2024", "Application"),
				new MailVariableDto("Application Motivation", "[[${application.motivation}]]", "Strong motivation", "Application"),
				new MailVariableDto("Applicant Special Skills", "[[${application.specialSkills}]]", "Python, Java", "Application"),
				new MailVariableDto("Applicant Interests", "[[${application.interests}]]", "AI", "Application"),
				new MailVariableDto("Applicant Projects", "[[${application.projects}]]", "Chatbot", "Application"),
				new MailVariableDto("Application URL", "[[${applicationUrl}]]", "https://thesis-management.com/applications/123", "Application")
		);
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}

	private static String resolveThesisTitle(Application application) {
		if (application.getThesisTitle() != null && !application.getThesisTitle().isBlank()) {
			return application.getThesisTitle();
		}

		if (application.getTopic() != null && application.getTopic().getTitle() != null
				&& !application.getTopic().getTitle().isBlank()) {
			return application.getTopic().getTitle();
		}

		return "";
	}
}
