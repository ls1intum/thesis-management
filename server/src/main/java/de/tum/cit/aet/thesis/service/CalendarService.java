package de.tum.cit.aet.thesis.service;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.mail.internet.InternetAddress;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Manages calendar events via a CalDAV server, supporting creation, update, and deletion of iCal events. */
@Service
public class CalendarService {
	private static final Logger log = LoggerFactory.getLogger(CalendarService.class);

	private final WebClient webClient;
	private final boolean enabled;

	/**
	 * Initializes the CalDAV WebClient with the configured URL and authentication credentials.
	 *
	 * @param enabled whether the calendar integration is enabled
	 * @param caldavUrl the CalDAV server URL
	 * @param caldavUsername the CalDAV authentication username
	 * @param caldavPassword the CalDAV authentication password
	 */
	public CalendarService(
			@Value("${thesis-management.calendar.enabled}") Boolean enabled,
			@Value("${thesis-management.calendar.url}") String caldavUrl,
			@Value("${thesis-management.calendar.username}") String caldavUsername,
			@Value("${thesis-management.calendar.password}") String caldavPassword
	) {
		this.enabled = enabled;

		this.webClient = WebClient.builder()
				.baseUrl(caldavUrl)
				.defaultHeaders(headers -> headers.setBasicAuth(caldavUsername, caldavPassword))
				.build();
	}

	/**
	 * Represents a calendar event with scheduling details, organizer, and attendee information.
	 *
	 * @param title the event title
	 * @param location the event location
	 * @param description the event description
	 * @param start the event start time
	 * @param end the event end time
	 * @param organizer the event organizer email
	 * @param requiredAttendees the list of required attendee emails
	 * @param optionalAttendees the list of optional attendee emails
	 */
	public record CalendarEvent(
			String title,
			String location,
			String description,
			Instant start,
			Instant end,
			InternetAddress organizer,
			List<InternetAddress> requiredAttendees,
			List<InternetAddress> optionalAttendees
	) {}

	/**
	 * Creates a new calendar event on the CalDAV server and returns its generated event ID.
	 *
	 * @param data the calendar event data
	 * @return the generated event ID, or null if calendar is disabled or creation fails
	 */
	public String createEvent(CalendarEvent data) {
		if (!enabled) {
			return null;
		}

		try {
			Calendar calendar = getCalendar();
			String eventId = UUID.randomUUID().toString();

			calendar.add(createVEvent(eventId, data));
			updateCalendar(calendar);

			return eventId;
		} catch (Exception exception) {
			log.warn("Failed to create calendar event", exception);
		}

		return null;
	}

	/**
	 * Updates an existing calendar event identified by its event ID with the provided data.
	 *
	 * @param eventId the event ID to update
	 * @param data the new calendar event data
	 */
	public void updateEvent(String eventId, CalendarEvent data) {
		if (!enabled) {
			return;
		}

		if (eventId == null) {
			return;
		}

		try {
			Calendar calendar = getCalendar();
			Optional<VEvent> event = findVEvent(calendar, eventId);

			event.ifPresent(calendar::remove);
			calendar.add(createVEvent(eventId, data));

			updateCalendar(calendar);
		} catch (Exception exception) {
			log.warn("Failed to create calendar event", exception);
		}
	}

	/**
	 * Deletes a calendar event identified by its event ID from the CalDAV server.
	 *
	 * @param eventId the event ID to delete
	 */
	public void deleteEvent(String eventId) {
		if (!enabled) {
			return;
		}

		if (eventId == null || eventId.isBlank()) {
			return;
		}

		try {
			Calendar calendar = getCalendar();

			VEvent event = findVEvent(calendar, eventId).orElseThrow();
			calendar.remove(event);

			updateCalendar(calendar);
		} catch (Exception exception) {
			log.warn("Failed to delete calendar event", exception);
		}
	}

	/**
	 * Finds a VEvent in the given calendar by its unique event ID.
	 *
	 * @param calendar the iCal calendar to search
	 * @param eventId the event ID to find
	 * @return an optional containing the VEvent if found
	 */
	public Optional<VEvent> findVEvent(Calendar calendar, String eventId) {
		return calendar.getComponents(Component.VEVENT).stream()
				.map(component -> (VEvent) component)
				.filter(event -> event.getUid()
						.map(Uid::getValue)
						.filter(value -> value.equals(eventId))
						.isPresent())
				.findFirst();
	}

	/**
	 * Builds an iCal VEvent from the given event ID and calendar event data.
	 *
	 * @param eventId the unique event ID
	 * @param data the calendar event data
	 * @return the constructed VEvent
	 */
	public VEvent createVEvent(String eventId, CalendarEvent data) {
		VEvent event = new VEvent(data.start, data.end, data.title);

		event.add(new Uid(eventId));

		if (data.location != null) {
			event.add(new Location(data.location));
		}

		if (data.description != null) {
			event.add(new Description(data.description));
		}

		if (data.organizer != null) {
			Organizer organizer = new Organizer(URI.create("mailto:" + data.organizer.getAddress()));

			organizer.add(Rsvp.TRUE);

			event.add(organizer);
		}

		if (data.requiredAttendees != null) {
			for (InternetAddress address : data.requiredAttendees) {
				Attendee attendee = new Attendee(URI.create("mailto:" + address.getAddress()));

				attendee.add(Role.REQ_PARTICIPANT);
				attendee.add(PartStat.ACCEPTED);

				event.add(attendee);
			}
		}

		if (data.optionalAttendees != null) {
			for (InternetAddress address : data.optionalAttendees) {
				if (data.requiredAttendees != null && data.requiredAttendees.contains(address)) {
					continue;
				}

				Attendee attendee = new Attendee(URI.create("mailto:" + address.getAddress()));

				attendee.add(Role.OPT_PARTICIPANT);
				attendee.add(Rsvp.TRUE);

				event.add(attendee);
			}
		}

		return event;
	}

	private Calendar getCalendar() {
		String response = webClient.method(HttpMethod.GET)
				.retrieve()
				.bodyToMono(String.class)
				.block();

		if (response == null) {
			throw new RuntimeException("Calendar response was empty");
		}

		try {
			CalendarBuilder builder = new CalendarBuilder();
			Reader reader = Reader.of(response);

			return builder.build(reader);
		} catch (IOException | ParserException e) {
			throw new RuntimeException("Failed to parse calendar", e);
		}
	}

	private void updateCalendar(Calendar calendar) {
		webClient.method(HttpMethod.PUT)
				.contentType(MediaType.parseMediaType("text/calendar"))
				.bodyValue(calendar.toString())
				.retrieve()
				.bodyToMono(Void.class)
				.block();
	}

	/**
	 * Creates an empty iCal calendar with the specified product ID and Gregorian calendar scale.
	 *
	 * @param prodId the product identifier for the calendar
	 * @return the empty iCal calendar
	 */
	public Calendar createEmptyCalendar(String prodId) {
		Calendar calendar = new Calendar();

		calendar.add(new ProdId(prodId));
		calendar.add(ImmutableVersion.VERSION_2_0);
		calendar.add(ImmutableCalScale.GREGORIAN);

		return calendar;
	}
}
