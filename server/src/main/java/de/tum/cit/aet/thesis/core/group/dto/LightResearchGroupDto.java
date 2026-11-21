package de.tum.cit.aet.thesis.core.group.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.core.user.dto.MinimalUserDto;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record LightResearchGroupDto(
	UUID id,
	MinimalUserDto head,
	String name,
	String abbreviation
) {

public static LightResearchGroupDto fromResearchGroupEntity(ResearchGroup group) {
	if (group == null) {
	return null;
	}

	return new LightResearchGroupDto(
		group.getId(),
		MinimalUserDto.fromUserEntity(group.getHead()),
		group.getName(),
			group.getAbbreviation()
	);
}
}
