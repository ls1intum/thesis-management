package de.tum.cit.aet.thesis.interview.dto;

import de.tum.cit.aet.thesis.core.application.constants.ApplicationState;
import de.tum.cit.aet.thesis.core.user.dto.LightUserDto;
import de.tum.cit.aet.thesis.interview.entity.InterviewSlot;
import de.tum.cit.aet.thesis.interview.entity.Interviewee;

import java.time.Instant;
import java.util.UUID;

public record IntervieweeLightWithNextSlotDto(
	UUID intervieweeId,
	LightUserDto user,
	int score,
	Instant lastInvited,
	InterviewSlotDto nextSlot,
	UUID applicationId,
	ApplicationState applicationState
) {
	public static IntervieweeLightWithNextSlotDto fromIntervieweeEntity(Interviewee interviewee) {
		int score = interviewee.getScore() != null ? interviewee.getScore() : -1;
		InterviewSlot nextSlot = interviewee.getNextSlot();

		return new IntervieweeLightWithNextSlotDto(
				interviewee.getIntervieweeId(),
				LightUserDto.fromUserEntity(interviewee.getApplication().getUser()),
				score,
				interviewee.getLastInvited(),
				nextSlot != null ? InterviewSlotDto.fromInterviewSlot(nextSlot) : null,
				interviewee.getApplication().getId(),
				interviewee.getApplication().getState()
		);

	}
}
