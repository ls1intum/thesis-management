package de.tum.cit.aet.thesis.core.organization.controller.payload;

import java.util.UUID;

public record DepartmentPayload(
		UUID schoolId,
		String name,
		String abbreviation,
		Boolean active
) {
}
