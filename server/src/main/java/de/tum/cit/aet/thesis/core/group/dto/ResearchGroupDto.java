package de.tum.cit.aet.thesis.core.group.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.core.user.dto.MinimalUserDto;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ResearchGroupDto(
	UUID id,
	MinimalUserDto head,
	String name,
	String abbreviation,
	String description,
	String websiteUrl,
	String campus,
	Long memberCount
) {

public static ResearchGroupDto fromResearchGroupEntity(ResearchGroup group) {
	return fromResearchGroupEntity(group, null);
}

public static ResearchGroupDto fromResearchGroupEntity(ResearchGroup group, Long memberCount) {
	if (group == null) {
	return null;
	}

	return new ResearchGroupDto(
		group.getId(),
		MinimalUserDto.fromUserEntity(group.getHead()),
		group.getName(),
		group.getAbbreviation(),
		group.getDescription(),
		group.getWebsiteUrl(),
		group.getCampus(),
		memberCount
	);
}
}
