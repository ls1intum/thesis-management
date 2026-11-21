package de.tum.cit.aet.thesis.interview.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import de.tum.cit.aet.thesis.interview.entity.InterviewSlot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class MailInterviewSlotTest {

	@Test
	void fromInterviewSlot_null_returnsEmptyRecord() {
		MailInterviewSlot result = MailInterviewSlot.fromInterviewSlot(null);
		assertEquals(new MailInterviewSlot("", "", ""), result);
	}

	@Test
	void fromInterviewSlot_fullData_mapsAndNormalizes() {
		InterviewSlot slot = new InterviewSlot();
		slot.setStartDate(Instant.parse("2026-02-12T13:00:00Z"));
		slot.setLocation("Room 101");
		slot.setStreamLink("meeting.example.org/room/123");

		MailInterviewSlot result = MailInterviewSlot.fromInterviewSlot(slot);

		assertEquals("Room 101", result.location());
		assertTrue(result.streamUrl().startsWith("https://"), "Expected https prefix: " + result.streamUrl());
		assertEquals("https://meeting.example.org/room/123", result.streamUrl());
		assertNotNull(result.startDate());
	}

	@Test
	void fromInterviewSlot_existingHttpsLink_unchanged() {
		InterviewSlot slot = new InterviewSlot();
		slot.setStartDate(Instant.parse("2026-02-12T13:00:00Z"));
		slot.setStreamLink("https://meeting.example.org/room/123");

		MailInterviewSlot result = MailInterviewSlot.fromInterviewSlot(slot);
		assertEquals("https://meeting.example.org/room/123", result.streamUrl());
	}

	@Test
	void fromInterviewSlot_existingHttpLink_unchanged() {
		InterviewSlot slot = new InterviewSlot();
		slot.setStartDate(Instant.parse("2026-02-12T13:00:00Z"));
		slot.setStreamLink("http://internal/room/1");

		MailInterviewSlot result = MailInterviewSlot.fromInterviewSlot(slot);
		assertEquals("http://internal/room/1", result.streamUrl());
	}

	@Test
	void fromInterviewSlot_emptyOrNullLink_returnsEmptyUrl() {
		InterviewSlot slot = new InterviewSlot();
		slot.setStartDate(Instant.parse("2026-02-12T13:00:00Z"));
		slot.setStreamLink("   ");

		MailInterviewSlot result = MailInterviewSlot.fromInterviewSlot(slot);
		assertEquals("", result.streamUrl());
	}

	@Test
	void fromInterviewSlot_nullLink_returnsEmptyUrl() {
		InterviewSlot slot = new InterviewSlot();
		slot.setStartDate(Instant.parse("2026-02-12T13:00:00Z"));
		slot.setStreamLink(null);

		MailInterviewSlot result = MailInterviewSlot.fromInterviewSlot(slot);
		assertEquals("", result.streamUrl());
	}

	@Test
	void templateVariables_containsExpectedKeys() {
		List<MailVariableDto> vars = MailInterviewSlot.templateVariables();
		assertEquals(3, vars.size());
		assertEquals("Interview Slot Start Date", vars.get(0).label());
		assertEquals("Interview Slot Location", vars.get(1).label());
		assertEquals("Interview Slot Stream URL", vars.get(2).label());
		for (MailVariableDto v : vars) {
			assertEquals("Interview Slot", v.group());
		}
	}
}
