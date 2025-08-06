package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import java.util.UUID;

public record LightResearchGroupDto(
    UUID id,
    LightUserDto head,
    String name,
    String abbreviation
) {

  public static LightResearchGroupDto fromResearchGroupEntity(ResearchGroup group) {
    if (group == null) {
      return null;
    }

    return new LightResearchGroupDto(
        group.getId(),
        LightUserDto.fromUserEntity(group.getHead()),
        group.getName(),
            group.getAbbreviation()
    );
  }
}