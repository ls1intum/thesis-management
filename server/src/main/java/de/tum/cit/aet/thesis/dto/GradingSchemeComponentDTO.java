package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.entity.GradingSchemeComponent;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record GradingSchemeComponentDTO(
		UUID componentId,
		String name,
		BigDecimal weight,
		Boolean isBonus,
		Integer position
) {
	public static GradingSchemeComponentDTO fromEntity(GradingSchemeComponent entity) {
		return new GradingSchemeComponentDTO(
				entity.getId(),
				entity.getName(),
				entity.getWeight(),
				entity.getIsBonus(),
				entity.getPosition()
		);
	}
}
