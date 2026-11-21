package de.tum.cit.aet.thesis.interview.controller.payload;

import java.util.List;
import java.util.UUID;

public record InviteIntervieweesPayload(List<UUID> intervieweeIds) { }
