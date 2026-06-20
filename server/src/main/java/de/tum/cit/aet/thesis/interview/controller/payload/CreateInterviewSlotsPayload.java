package de.tum.cit.aet.thesis.interview.controller.payload;

import de.tum.cit.aet.thesis.interview.dto.InterviewSlotDto;

import java.util.List;
import java.util.UUID;

public record CreateInterviewSlotsPayload(UUID interviewProcessId, List<InterviewSlotDto> interviewSlots) {
}
