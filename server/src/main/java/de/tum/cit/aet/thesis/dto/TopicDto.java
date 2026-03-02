package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.TopicState;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.TopicRole;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
	MinimalUserDto createdBy,
	LightResearchGroupDto researchGroup,

	List<MinimalUserDto> supervisors,
	List<MinimalUserDto> examiners
) {

public static TopicDto fromTopicEntity(Topic topic) {
	if (topic == null) {
	return null;
	}

	List<MinimalUserDto> supervisors = new ArrayList<>();
	List<MinimalUserDto> examiners = new ArrayList<>();

	for (TopicRole role : topic.getRoles()) {
	if (role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
		supervisors.add(MinimalUserDto.fromUserEntity(role.getUser()));
	} else if (role.getId().getRole() == ThesisRoleName.EXAMINER) {
		examiners.add(MinimalUserDto.fromUserEntity(role.getUser()));
	}
	}

	return new TopicDto(
		topic.getId(),
		topic.getTitle(),
		topic.getThesisTypes(),
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
		MinimalUserDto.fromUserEntity(topic.getCreatedBy()),
		LightResearchGroupDto.fromResearchGroupEntity(topic.getResearchGroup()),
		supervisors,
		examiners
	);
}
}
