package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ThesisGradeComponent;

import java.math.BigDecimal;
import java.util.UUID;

public record ThesisGradeComponentDTO(
		UUID gradeComponentId,
		String name,
		BigDecimal weight,
		Boolean isBonus,
		BigDecimal grade,
		Integer position
) {
	public static ThesisGradeComponentDTO fromEntity(ThesisGradeComponent entity) {
		return new ThesisGradeComponentDTO(
				entity.getId(),
				entity.getName(),
				entity.getWeight(),
				entity.getIsBonus(),
				entity.getGrade(),
				entity.getPosition()
		);
	}
}
