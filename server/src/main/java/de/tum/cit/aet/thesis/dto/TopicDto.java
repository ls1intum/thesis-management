package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.TopicState;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.TopicRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TopicDto(
    UUID topicId,
    String title,
    Set<String> thesisTypes,
    String problemStatement,
    String requirements,
    String goals,
    String references,
    Instant closedAt,
    Instant publishedAt,
    Instant updatedAt,
    Instant createdAt,
    Instant intendedStart,
    Instant applicationDeadline,
    TopicState state,
    LightUserDto createdBy,
    LightResearchGroupDto researchGroup,

    List<LightUserDto> advisors,
    List<LightUserDto> supervisors
) {

  public static TopicDto fromTopicEntity(Topic topic) {
    if (topic == null) {
      return null;
    }

    List<LightUserDto> advisors = new ArrayList<>();
    List<LightUserDto> supervisors = new ArrayList<>();

    for (TopicRole role : topic.getRoles()) {
      if (role.getId().getRole() == ThesisRoleName.ADVISOR) {
        advisors.add(LightUserDto.fromUserEntity(role.getUser()));
      } else if (role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
        supervisors.add(LightUserDto.fromUserEntity(role.getUser()));
      }
    }

    return new TopicDto(
        topic.getId(),
        topic.getTitle(),
        topic.getThesisTypes() == null || topic.getThesisTypes().isEmpty() ? null
            : topic.getThesisTypes(),
        topic.getProblemStatement(),
        topic.getRequirements(),
        topic.getGoals(),
        topic.getReferences(),
        topic.getClosedAt(),
        topic.getPublishedAt(),
        topic.getUpdatedAt(),
        topic.getCreatedAt(),
        topic.getIntendedStart(),
        topic.getApplicationDeadline(),
        topic.getTopicState(),
        LightUserDto.fromUserEntity(topic.getCreatedBy()),
        LightResearchGroupDto.fromResearchGroupEntity(topic.getResearchGroup()),
        advisors,
        supervisors
    );
  }
}
