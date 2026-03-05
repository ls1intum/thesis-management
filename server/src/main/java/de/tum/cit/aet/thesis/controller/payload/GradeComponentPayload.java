package de.tum.cit.aet.thesis.controller.payload;

import de.tum.cit.aet.thesis.service.ThesisService;

import java.math.BigDecimal;

public record GradeComponentPayload(
		String name,
		BigDecimal weight,
		boolean isBonus,
		BigDecimal grade
) implements ThesisService.GradeComponentData {
}
