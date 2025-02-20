package de.tum.cit.aet.thesis.service;

import jakarta.mail.internet.InternetAddress;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CalendarService {
    private static final Logger log = LoggerFactory.getLogger(CalendarService.class);

    private final WebClient webClient;
    private final boolean enabled;

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

    public Optional<VEvent> findVEvent(Calendar calendar, String eventId) {
        return calendar.getComponents(Component.VEVENT).stream()
                .map(component -> (VEvent) component)
                .filter(event -> event.getUid()
                        .map(Uid::getValue)
                        .filter(value -> value.equals(eventId))
                        .isPresent())
                .findFirst();
    }

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
            StringReader reader = new StringReader(response);

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
}
