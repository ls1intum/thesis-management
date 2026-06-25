package de.tum.cit.aet.thesis.core.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.core.admin.service.CalendarService.CalendarEvent;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import org.junit.jupiter.api.Test;

import jakarta.mail.internet.InternetAddress;

import java.time.Instant;
import java.util.List;

class CalendarServiceTest {

	private final CalendarService service = new CalendarService();

	private static InternetAddress addr(String email) throws Exception {
		return new InternetAddress(email);
	}

	@Test
	void createVEvent_fullData_addsAllProperties() throws Exception {
		CalendarEvent event = new CalendarEvent(
				"Title",
				"Room 101",
				"Description text",
				Instant.parse("2026-02-15T10:00:00Z"),
				Instant.parse("2026-02-15T11:00:00Z"),
				addr("org@example.com"),
				List.of(addr("req@example.com")),
				List.of(addr("opt@example.com"))
		);

		VEvent v = service.createVEvent("event-123", event);

		String text = v.toString();
		assertThat(text).contains("Title");
		assertThat(text).contains("Room 101");
		assertThat(text).contains("Description text");
		assertThat(text).contains("UID:event-123");
		assertThat(text).contains("ORGANIZER");
		assertThat(text).contains("ATTENDEE");
		assertThat(text).contains("req@example.com");
		assertThat(text).contains("opt@example.com");
	}

	@Test
	void createVEvent_optionalAttendeeDuplicateOfRequired_skipped() throws Exception {
		InternetAddress shared = addr("dup@example.com");
		CalendarEvent event = new CalendarEvent(
				"Title",
				null,
				null,
				Instant.parse("2026-02-15T10:00:00Z"),
				Instant.parse("2026-02-15T11:00:00Z"),
				null,
				List.of(shared),
				List.of(shared)
		);

		VEvent v = service.createVEvent("event-id", event);
		// Only one attendee entry with this email
		String text = v.toString();
		int firstIdx = text.indexOf("dup@example.com");
		int secondIdx = text.indexOf("dup@example.com", firstIdx + 1);
		assertThat(firstIdx).isPositive();
		assertThat(secondIdx).isEqualTo(-1);
	}

	@Test
	void createVEvent_nullOptionalFields_doesNotThrow() {
		CalendarEvent event = new CalendarEvent(
				"Bare title",
				null,
				null,
				Instant.parse("2026-02-15T10:00:00Z"),
				Instant.parse("2026-02-15T11:00:00Z"),
				null,
				null,
				null
		);

		VEvent v = service.createVEvent("bare", event);
		assertThat(v.toString()).contains("UID:bare");
	}

	@Test
	void createEmptyCalendar_setsProdIdAndDefaults() {
		Calendar calendar = service.createEmptyCalendar("-//Thesis Management//Test 1.0//EN");
		String text = calendar.toString();
		assertThat(text).contains("PRODID:-//Thesis Management//Test 1.0//EN");
		assertThat(text).contains("VERSION:2.0");
		assertThat(text).contains("CALSCALE:GREGORIAN");
	}
}
