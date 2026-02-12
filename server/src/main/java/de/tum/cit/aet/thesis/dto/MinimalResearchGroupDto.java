package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.entity.ResearchGroup;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MinimalResearchGroupDto(UUID id, String name) {
	public static MinimalResearchGroupDto fromResearchGroupEntity(ResearchGroup group) {
		if (group == null) {
			return null;
		}

		return new MinimalResearchGroupDto(
			group.getId(),
			group.getName()
		);
	}
}
