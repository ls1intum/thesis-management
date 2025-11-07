package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;

import java.time.Instant;
import java.util.UUID;

public record IntervieweeLightWithNextSlotDto(
    UUID intervieweeId,
    LightUserDto user,
    int score,
    Instant lastInvited,
    InterviewSlot nextSlot
) {
    public static IntervieweeLightWithNextSlotDto fromIntervieweeEntity(Interviewee interviewee) {
        int score = interviewee.getScore() != null ? interviewee.getScore() : -1;
        return new IntervieweeLightWithNextSlotDto(
                interviewee.getIntervieweeId(),
                LightUserDto.fromUserEntity(interviewee.getApplication().getUser()),
                score,
                interviewee.getLastInvited(),
                interviewee.getNextSlot()
        );

    }
}
