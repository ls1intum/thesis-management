package de.tum.cit.aet.thesis.controller.payload;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InviteIntervieweesPayload(List<UUID> intervieweeIds) { }
