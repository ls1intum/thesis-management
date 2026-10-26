package de.tum.cit.aet.thesis.core.organization.controller.payload;

import java.util.UUID;

public record StudyProgramPayload(
		String key,
		String name,
		UUID schoolId,
		Boolean active
) {
}
