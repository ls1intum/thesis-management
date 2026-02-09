package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;

import java.time.Instant;
import java.util.UUID;

public record InterviewSlotDto(
        UUID slotId,
        Instant startDate,
        Instant endDate,
        IntervieweeLightDto bookedBy,
        String location,
        String streamUrl
) {
    public static InterviewSlotDto fromInterviewSlot(InterviewSlot interviewSlot) {
        Interviewee interviewee = interviewSlot.getInterviewee();
        IntervieweeLightDto intervieweeLightDto = interviewee != null ? IntervieweeLightDto.fromIntervieweeEntity(interviewee) : null;

        return new InterviewSlotDto(
                interviewSlot.getId(),
                interviewSlot.getStartDate(),
                interviewSlot.getEndDate(),
                intervieweeLightDto,
                interviewSlot.getLocation(),
                normalizeUrl(interviewSlot.getStreamLink())
        );
    }

    public UUID getSlotId() {
        return slotId;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }

        String s = url.trim();
        if (s.isEmpty()) {
            return null;
        }

        // equivalent to /^https?:\/\//i
        if (!s.matches("(?i)^https?://.*")) {
            return "https://" + s;
        }

        return s;
    }
}
