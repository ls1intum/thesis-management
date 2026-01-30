package de.tum.cit.aet.thesis.controller.payload;

import java.util.List;
import java.util.UUID;

public record CreateInterviewProcessPayload(UUID topicId, List<UUID> intervieweeApplicationIds) {
}
