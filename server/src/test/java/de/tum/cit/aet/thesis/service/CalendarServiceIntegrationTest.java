package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.mail.internet.InternetAddress;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Testcontainers
class CalendarServiceIntegrationTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private CalendarService calendarService;

	@Nested
	class CreateEvent {
		@Test
		void createEvent_WithFullData_ReturnsEventId() throws Exception {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Test Presentation",
					"Room 101",
					"A test description",
					Instant.now().plus(1, ChronoUnit.DAYS),
					Instant.now().plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					new InternetAddress("organizer@test.com"),
					List.of(new InternetAddress("required@test.com")),
					List.of(new InternetAddress("optional@test.com"))
			);

			String eventId = calendarService.createEvent(data);
			assertThat(eventId).isNotNull().isNotBlank();
		}

		@Test
		void createEvent_WithMinimalData_ReturnsEventId() throws Exception {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Minimal Event",
					null,
					null,
					Instant.now().plus(2, ChronoUnit.DAYS),
					Instant.now().plus(2, ChronoUnit.DAYS).plus(60, ChronoUnit.MINUTES),
					null,
					null,
					null
			);

			String eventId = calendarService.createEvent(data);
			assertThat(eventId).isNotNull().isNotBlank();
		}
	}

	@Nested
	class UpdateEvent {
		@Test
		void updateEvent_ExistingEvent_Succeeds() throws Exception {
			CalendarService.CalendarEvent createData = new CalendarService.CalendarEvent(
					"Original Event",
					"Room A",
					"Original description",
					Instant.now().plus(3, ChronoUnit.DAYS),
					Instant.now().plus(3, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					null, null, null
			);

			String eventId = calendarService.createEvent(createData);
			assertThat(eventId).isNotNull();

			CalendarService.CalendarEvent updateData = new CalendarService.CalendarEvent(
					"Updated Event",
					"Room B",
					"Updated description",
					Instant.now().plus(4, ChronoUnit.DAYS),
					Instant.now().plus(4, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					null, null, null
			);

			assertDoesNotThrow(() -> calendarService.updateEvent(eventId, updateData));
		}

		@Test
		void updateEvent_NullEventId_ReturnsEarly() {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Event", null, null,
					Instant.now().plus(1, ChronoUnit.DAYS),
					Instant.now().plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					null, null, null
			);

			assertDoesNotThrow(() -> calendarService.updateEvent(null, data));
		}
	}

	@Nested
	class DeleteEvent {
		@Test
		void deleteEvent_ExistingEvent_Succeeds() throws Exception {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"To Delete",
					null, null,
					Instant.now().plus(5, ChronoUnit.DAYS),
					Instant.now().plus(5, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					null, null, null
			);

			String eventId = calendarService.createEvent(data);
			assertThat(eventId).isNotNull();

			assertDoesNotThrow(() -> calendarService.deleteEvent(eventId));
		}

		@Test
		void deleteEvent_NullEventId_ReturnsEarly() {
			assertDoesNotThrow(() -> calendarService.deleteEvent(null));
		}

		@Test
		void deleteEvent_BlankEventId_ReturnsEarly() {
			assertDoesNotThrow(() -> calendarService.deleteEvent(""));
		}

		@Test
		void deleteEvent_NonExistentEvent_GracefulFailure() {
			assertDoesNotThrow(() -> calendarService.deleteEvent("non-existent-event-id"));
		}
	}

	@Nested
	class CreateVEvent {
		@Test
		void createVEvent_SetsUidAndTitle() throws Exception {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Test Title",
					"Test Location",
					"Test Description",
					Instant.now(),
					Instant.now().plus(1, ChronoUnit.HOURS),
					null, null, null
			);

			VEvent event = calendarService.createVEvent("test-uid-123", data);

			assertThat(event.getUid().get().getValue()).isEqualTo("test-uid-123");
			assertThat(event.getSummary().getValue()).isEqualTo("Test Title");
			assertThat(event.getLocation().getValue()).isEqualTo("Test Location");
			assertThat(event.getDescription().getValue()).isEqualTo("Test Description");
		}

		@Test
		void createVEvent_WithOrganizer_SetsOrganizerProperty() throws Exception {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Organizer Event",
					null, null,
					Instant.now(),
					Instant.now().plus(1, ChronoUnit.HOURS),
					new InternetAddress("organizer@test.com"),
					null, null
			);

			VEvent event = calendarService.createVEvent("org-uid", data);

			assertThat(event.getOrganizer().getValue()).contains("organizer@test.com");
		}

		@Test
		void createVEvent_WithRequiredAttendees_SetsAttendeeProperties() throws Exception {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Attendee Event",
					null, null,
					Instant.now(),
					Instant.now().plus(1, ChronoUnit.HOURS),
					null,
					List.of(new InternetAddress("req@test.com")),
					null
			);

			VEvent event = calendarService.createVEvent("att-uid", data);

			assertThat(event.getProperties("ATTENDEE")).isNotEmpty();
			assertThat(event.getProperties("ATTENDEE").getFirst().getValue()).contains("req@test.com");
		}

		@Test
		void createVEvent_WithOptionalAttendees_SetsOptionalAttendeeProperties() throws Exception {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Optional Attendee Event",
					null, null,
					Instant.now(),
					Instant.now().plus(1, ChronoUnit.HOURS),
					null,
					null,
					List.of(new InternetAddress("opt@test.com"))
			);

			VEvent event = calendarService.createVEvent("opt-uid", data);

			assertThat(event.getProperties("ATTENDEE")).isNotEmpty();
		}

		@Test
		void createVEvent_WithOverlappingAttendees_DeduplicatesOptional() throws Exception {
			InternetAddress shared = new InternetAddress("shared@test.com");

			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Dedup Event",
					null, null,
					Instant.now(),
					Instant.now().plus(1, ChronoUnit.HOURS),
					null,
					List.of(shared),
					List.of(shared, new InternetAddress("unique@test.com"))
			);

			VEvent event = calendarService.createVEvent("dedup-uid", data);

			long sharedCount = event.getProperties("ATTENDEE").stream()
					.filter(p -> p.getValue().contains("shared@test.com"))
					.count();
			assertThat(sharedCount).isEqualTo(1);
		}

		@Test
		void createVEvent_WithNullLocationAndDescription_OmitsThoseFields() throws Exception {
			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Sparse Event",
					null,
					null,
					Instant.now(),
					Instant.now().plus(1, ChronoUnit.HOURS),
					null, null, null
			);

			VEvent event = calendarService.createVEvent("sparse-uid", data);

			assertThat(event.getLocation()).isNull();
			assertThat(event.getDescription()).isNull();
		}
	}

	@Nested
	class FindVEvent {
		@Test
		void findVEvent_MatchingUid_ReturnsEvent() {
			Calendar calendar = calendarService.createEmptyCalendar("-//Test//Test//EN");

			CalendarService.CalendarEvent data = new CalendarService.CalendarEvent(
					"Find Me",
					null, null,
					Instant.now(),
					Instant.now().plus(1, ChronoUnit.HOURS),
					null, null, null
			);

			VEvent event = calendarService.createVEvent("find-uid", data);
			calendar.add(event);

			Optional<VEvent> found = calendarService.findVEvent(calendar, "find-uid");
			assertThat(found).isPresent();
			assertThat(found.get().getUid().get().getValue()).isEqualTo("find-uid");
		}

		@Test
		void findVEvent_NonMatchingUid_ReturnsEmpty() {
			Calendar calendar = calendarService.createEmptyCalendar("-//Test//Test//EN");

			Optional<VEvent> found = calendarService.findVEvent(calendar, "nonexistent-uid");
			assertThat(found).isEmpty();
		}
	}

	@Nested
	class CreateEmptyCalendar {
		@Test
		void createEmptyCalendar_SetsPropertiesCorrectly() {
			Calendar calendar = calendarService.createEmptyCalendar("-//Test//Calendar//EN");

			assertThat(calendar.toString()).contains("-//Test//Calendar//EN");
			assertThat(calendar.toString()).contains("VERSION:2.0");
			assertThat(calendar.toString()).contains("CALSCALE:GREGORIAN");
		}
	}
}
