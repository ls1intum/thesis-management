package de.tum.cit.aet.thesis.controller.payload;

import java.math.BigDecimal;

public record GradeComponentPayload(
		String name,
		BigDecimal weight,
		boolean isBonus,
		BigDecimal grade
) {
}
