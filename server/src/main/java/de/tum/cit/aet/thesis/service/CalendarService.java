package de.tum.cit.aet.thesis.service;

import net.fortuna.ical4j.model.Calendar;
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
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/** Provides helper methods for building iCalendar feeds (ICS subscription format). */
@Service
public class CalendarService {

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
