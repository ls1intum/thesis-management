package de.tum.cit.aet.thesis.core.organization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.organization.entity.School;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MinimalSchoolDto(UUID id, String name, String abbreviation) {
	public static MinimalSchoolDto fromSchoolEntity(School school) {
		if (school == null) {
			return null;
		}

		return new MinimalSchoolDto(school.getId(), school.getName(), school.getAbbreviation());
	}
}
