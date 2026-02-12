package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;

import java.util.List;

public record MailApplicationReminder(
        String unreviewedApplications,
        String reviewApplicationsLink
) {
    public static List<MailVariableDto> templateVariables() {
        return List.of(
                new MailVariableDto("Unreviewed Applications", "[[${unreviewedApplications}]]", "10", "Application Reminder"),
                new MailVariableDto("Review Applications URL", "[[${reviewApplicationsLink}]]", "https://thesis-management.com/applications", "Application Reminder")
        );
    }
}
