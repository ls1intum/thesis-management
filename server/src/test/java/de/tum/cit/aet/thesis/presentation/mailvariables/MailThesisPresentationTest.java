package de.tum.cit.aet.thesis.presentation.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.presentation.constants.ThesisPresentationType;
import de.tum.cit.aet.thesis.presentation.entity.ThesisPresentation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class MailThesisPresentationTest {

	@Test
	void fromPresentation_null_returnsEmpty() {
		MailThesisPresentation result = MailThesisPresentation.fromPresentation(null);
		assertEquals(new MailThesisPresentation("", "", "", "", "", "", ""), result);
	}

	@Test
	void fromPresentation_withData_mapsAllFields() {
		User creator = new User();
		creator.setFirstName("Max");
		creator.setLastName("Mustermann");
		ThesisPresentation presentation = new ThesisPresentation();
		presentation.setCreatedBy(creator);
		presentation.setScheduledAt(Instant.parse("2026-01-15T10:00:00Z"));
		presentation.setLocation("Room 101");
		presentation.setStreamUrl("https://video.example.com");
		presentation.setLanguage("ENGLISH");
		presentation.setType(ThesisPresentationType.FINAL);

		MailThesisPresentation result = MailThesisPresentation.fromPresentation(presentation);

		assertEquals("Max", result.creatorFirstName());
		assertEquals("Mustermann", result.creatorLastName());
		assertEquals("Final", result.type());
		assertEquals("Room 101", result.location());
		assertEquals("https://video.example.com", result.streamUrl());
		assertEquals("English", result.language());
		assertFalse(result.scheduledAt().isBlank());
	}

	@Test
	void fromPresentation_withoutCreator_returnsEmptyNames() {
		ThesisPresentation presentation = new ThesisPresentation();
		presentation.setScheduledAt(Instant.parse("2026-01-15T10:00:00Z"));
		presentation.setLanguage("ENGLISH");
		presentation.setType(ThesisPresentationType.INTERMEDIATE);

		MailThesisPresentation result = MailThesisPresentation.fromPresentation(presentation);
		assertEquals("", result.creatorFirstName());
		assertEquals("", result.creatorLastName());
	}

	@Test
	void templateVariables_eightEntriesInPresentationGroup() {
		List<MailVariableDto> vars = MailThesisPresentation.templateVariables();
		assertEquals(8, vars.size());
		for (MailVariableDto v : vars) {
			assertEquals("Presentation", v.group());
		}
	}
}
