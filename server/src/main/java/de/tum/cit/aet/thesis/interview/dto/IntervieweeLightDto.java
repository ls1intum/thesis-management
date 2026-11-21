package de.tum.cit.aet.thesis.interview.dto;

import de.tum.cit.aet.thesis.core.user.dto.MinimalUserDto;
import de.tum.cit.aet.thesis.interview.entity.Interviewee;

import java.time.Instant;
import java.util.UUID;

public record IntervieweeLightDto(
	UUID intervieweeId,
	MinimalUserDto user,
	int score,
	Instant lastInvited
) {
	public static IntervieweeLightDto fromIntervieweeEntity(Interviewee interviewee) {
		int score = interviewee.getScore() != null ? interviewee.getScore() : -1;
		return new IntervieweeLightDto(
				interviewee.getIntervieweeId(),
				MinimalUserDto.fromUserEntity(interviewee.getApplication().getUser()),
				score,
				interviewee.getLastInvited()
		);

	}
}
