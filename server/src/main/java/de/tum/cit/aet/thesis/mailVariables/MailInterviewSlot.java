package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;

import java.util.List;

public record MailInterviewSlot(
        String startDate,
        String location,
        String streamUrl
) {
    public static List<MailVariableDto> templateVariables() {
        return List.of(
                new MailVariableDto("Interview Slot Start Date", "[[${slot.startDate}]]", "12.02.2026 14:00:00 CET", "Interview Slot"),
                new MailVariableDto("Interview Slot Location", "[[${slot.location}]]", "Room 101", "Interview Slot"),
                new MailVariableDto("Interview Slot Stream URL", "[[${slot.streamUrl}]]", "https://meeting.example.org/room/123", "Interview Slot")
        );
    }
}
