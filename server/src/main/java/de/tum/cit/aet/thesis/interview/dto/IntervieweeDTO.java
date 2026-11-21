package de.tum.cit.aet.thesis.interview.dto;

import de.tum.cit.aet.thesis.core.application.dto.ApplicationSummaryDto;
import de.tum.cit.aet.thesis.core.user.dto.LightUserDto;
import de.tum.cit.aet.thesis.interview.entity.Interviewee;

import java.time.Instant;
import java.util.UUID;

public record IntervieweeDTO(
	UUID intervieweeId,
	LightUserDto user,
	int score,
	Instant lastInvited,
	String interviewNote,
	ApplicationSummaryDto application
) {
	public static IntervieweeDTO fromIntervieweeEntity(Interviewee interviewee) {
		int score = interviewee.getScore() != null ? interviewee.getScore() : -1;
		return new IntervieweeDTO(
				interviewee.getIntervieweeId(),
				LightUserDto.fromUserEntity(interviewee.getApplication().getUser()),
				score,
				interviewee.getLastInvited(),
				interviewee.getAssessments().isEmpty() ? "" : interviewee.getAssessments().getFirst().getInterviewNote(),
				ApplicationSummaryDto.fromApplicationEntity(interviewee.getApplication())
		);

	}
}
