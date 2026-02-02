package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import java.util.UUID;

public record ResearchGroupDto(
    UUID id,
    LightUserDto head,
    String name,
    String abbreviation,
    String description,
    String websiteUrl,
    String campus
) {

  public static ResearchGroupDto fromResearchGroupEntity(ResearchGroup group) {
    if (group == null) {
      return null;
    }

    return new ResearchGroupDto(
        group.getId(),
        LightUserDto.fromUserEntity(group.getHead()),
        group.getName(),
        group.getAbbreviation(),
        group.getDescription(),
        group.getWebsiteUrl(),
        group.getCampus()
    );
  }
}
