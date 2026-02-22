package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.mail.internet.InternetAddress;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

class CalendarServiceDisabledTest {

	private CalendarService calendarService;

	@BeforeEach
	void setUp() {
		calendarService = new CalendarService(false, "http://localhost:9999", "user", "pass");
	}

	@Test
	void createEvent_WhenDisabled_ReturnsNull() throws Exception {
		CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
				"Test", "Room", "Desc",
				Instant.now().plus(1, ChronoUnit.DAYS),
				Instant.now().plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
				new InternetAddress("org@test.com"),
				List.of(new InternetAddress("req@test.com")),
				List.of(new InternetAddress("opt@test.com"))
		);

		String result = calendarService.createEvent(data);
		assertThat(result).isNull();
	}

	@Test
	void updateEvent_WhenDisabled_ReturnsEarlyWithoutError() {
		CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
				"Test", null, null,
				Instant.now().plus(1, ChronoUnit.DAYS),
				Instant.now().plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
				null, null, null
		);

		assertDoesNotThrow(() -> calendarService.updateEvent("some-id", data));
	}

	@Test
	void deleteEvent_WhenDisabled_ReturnsEarlyWithoutError() {
		assertDoesNotThrow(() -> calendarService.deleteEvent("some-id"));
	}
}
