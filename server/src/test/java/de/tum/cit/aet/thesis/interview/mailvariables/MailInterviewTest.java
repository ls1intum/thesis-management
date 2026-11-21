package de.tum.cit.aet.thesis.interview.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import org.junit.jupiter.api.Test;

import java.util.List;

class MailInterviewTest {

	@Test
	void record_preservesValues() {
		MailInterview m = new MailInterview("https://invite");
		assertEquals("https://invite", m.inviteUrl());
	}

	@Test
	void templateVariables_returnsExpectedSet() {
		List<MailVariableDto> vars = MailInterview.templateVariables();
		assertNotNull(vars);
		assertEquals(1, vars.size());
		assertEquals("Interview Invite URL", vars.get(0).label());
		assertEquals("Interview", vars.get(0).group());
		assertFalse(vars.get(0).templateVariable().isBlank());
	}
}
