package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.utility.DataFormatter;

import java.util.List;

public record MailInterviewSlot(
        String startDate,
        String location,
        String streamUrl
) {
    public static MailInterviewSlot fromInterviewSlot(InterviewSlot interviewSlot) {
        if (interviewSlot == null) {
            return new MailInterviewSlot("", "", "");
        }

        return new MailInterviewSlot(
                valueOrEmpty(DataFormatter.formatDateTime(interviewSlot.getStartDate())),
                valueOrEmpty(interviewSlot.getLocation()),
                valueOrEmpty(normalizeUrl(interviewSlot.getStreamLink()))
        );
    }

    public static List<MailVariableDto> templateVariables() {
        return List.of(
                new MailVariableDto("Interview Slot Start Date", "[[${slot.startDate}]]", "12.02.2026 14:00:00 CET", "Interview Slot"),
                new MailVariableDto("Interview Slot Location", "[[${slot.location}]]", "Room 101", "Interview Slot"),
                new MailVariableDto("Interview Slot Stream URL", "[[${slot.streamUrl}]]", "https://meeting.example.org/room/123", "Interview Slot")
        );
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }

        String s = url.trim();
        if (s.isEmpty()) {
            return null;
        }

        if (!s.matches("(?i)^https?://.*")) {
            return "https://" + s;
        }

        return s;
    }
}
