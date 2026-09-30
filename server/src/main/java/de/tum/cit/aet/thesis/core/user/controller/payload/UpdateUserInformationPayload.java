package de.tum.cit.aet.thesis.core.user.controller.payload;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UpdateUserInformationPayload(
		String firstName,
		String lastName,
		String gender,
		String nationality,
		String email,
		String studyDegree,
		UUID studyProgramId,
		Instant enrolledAt,
		String specialSkills,
		String interests,
		String projects,
		Map<String, String> customData
) { }
