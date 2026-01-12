package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;

import java.time.Instant;
import java.util.UUID;

public record UpcomingInterviewDto(
        UUID interviewProcessId,
        String topicTitle,
        InterviewSlotDto slot
) {
    public static UpcomingInterviewDto fromInterviewSlot(InterviewProcess interviewProcess, InterviewSlot interviewSlot) {
        return new UpcomingInterviewDto(
                interviewProcess.getId(),
                interviewProcess.getTopic().getTitle(),
                InterviewSlotDto.fromInterviewSlot(interviewSlot)
        );
    }
}
