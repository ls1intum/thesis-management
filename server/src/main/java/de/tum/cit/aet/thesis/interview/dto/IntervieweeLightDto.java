package de.tum.cit.aet.thesis.interview.dto;

import de.tum.cit.aet.thesis.core.user.dto.LightUserDto;
import de.tum.cit.aet.thesis.interview.entity.Interviewee;

import java.time.Instant;
import java.util.UUID;

public record IntervieweeLightDto(
	UUID intervieweeId,
	LightUserDto user,
	int score,
	Instant lastInvited
) {
	public static IntervieweeLightDto fromIntervieweeEntity(Interviewee interviewee) {
		int score = interviewee.getScore() != null ? interviewee.getScore() : -1;
		return new IntervieweeLightDto(
				interviewee.getIntervieweeId(),
				LightUserDto.fromUserEntity(interviewee.getApplication().getUser()),
				score,
				interviewee.getLastInvited()
		);

	}
}
