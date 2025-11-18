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
                interviewSlot.getStreamLink()
        );
    }

    public UUID getSlotId() {
        return slotId;
    }
}
