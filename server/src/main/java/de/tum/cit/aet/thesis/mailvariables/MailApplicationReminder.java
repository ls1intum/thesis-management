package de.tum.cit.aet.thesis.mailvariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;

import java.util.List;

/** Mail placeholder model for application reminder variables. */
public record MailApplicationReminder(
		String unreviewedApplications,
		String reviewApplicationsLink
) {
	/**
	 * Returns all selectable template variables for application reminders.
	 *
	 * @return application reminder variable descriptors
	 */
	public static List<MailVariableDto> templateVariables() {
		return List.of(
				new MailVariableDto("Unreviewed Applications", "[[${unreviewedApplications}]]", "10", "Application Reminder"),
				new MailVariableDto("Review Applications URL", "[[${reviewApplicationsLink}]]", "https://thesis-management.com/applications", "Application Reminder")
		);
	}
}
