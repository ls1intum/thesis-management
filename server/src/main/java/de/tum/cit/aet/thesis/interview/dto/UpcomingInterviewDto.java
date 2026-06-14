package de.tum.cit.aet.thesis.interview.dto;

import de.tum.cit.aet.thesis.interview.entity.InterviewProcess;
import de.tum.cit.aet.thesis.interview.entity.InterviewSlot;

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
