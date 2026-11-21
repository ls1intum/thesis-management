package de.tum.cit.aet.thesis.core.application.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import org.junit.jupiter.api.Test;

import java.util.List;

class MailApplicationReminderTest {

	@Test
	void record_preservesValues() {
		MailApplicationReminder reminder = new MailApplicationReminder("3", "https://link");
		assertEquals("3", reminder.unreviewedApplications());
		assertEquals("https://link", reminder.reviewApplicationsLink());
	}

	@Test
	void templateVariables_returnsBothEntries() {
		List<MailVariableDto> vars = MailApplicationReminder.templateVariables();
		assertEquals(2, vars.size());
		assertEquals("Unreviewed Applications", vars.get(0).label());
		assertEquals("Review Applications URL", vars.get(1).label());
		assertEquals("Application Reminder", vars.get(0).group());
		assertEquals("Application Reminder", vars.get(1).group());
	}
}
