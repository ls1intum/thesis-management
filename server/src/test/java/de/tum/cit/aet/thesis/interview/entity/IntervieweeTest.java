package de.tum.cit.aet.thesis.interview.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class IntervieweeTest {

	private InterviewSlot slot(Instant start) {
		InterviewSlot s = new InterviewSlot();
		s.setId(UUID.randomUUID());
		s.setStartDate(start);
		s.setEndDate(start.plusSeconds(3600));
		return s;
	}

	@Test
	void getNextSlot_emptyList_returnsNull() {
		Interviewee interviewee = new Interviewee();
		interviewee.setSlots(new ArrayList<>());
		assertNull(interviewee.getNextSlot());
	}

	@Test
	void getNextSlot_returnsEarliestSlot() {
		Interviewee interviewee = new Interviewee();
		InterviewSlot later = slot(Instant.parse("2026-03-15T10:00:00Z"));
		InterviewSlot earlier = slot(Instant.parse("2026-03-14T10:00:00Z"));
		InterviewSlot earliest = slot(Instant.parse("2026-03-13T10:00:00Z"));
		interviewee.setSlots(List.of(later, earlier, earliest));

		assertEquals(earliest.getId(), interviewee.getNextSlot().getId());
	}

	@Test
	void getNextSlot_singleSlot_returnsThatSlot() {
		Interviewee interviewee = new Interviewee();
		InterviewSlot only = slot(Instant.parse("2026-03-15T10:00:00Z"));
		interviewee.setSlots(List.of(only));
		assertEquals(only.getId(), interviewee.getNextSlot().getId());
	}
}
