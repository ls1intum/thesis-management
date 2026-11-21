package de.tum.cit.aet.thesis.core.topic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.group.dto.MinimalResearchGroupDto;
import de.tum.cit.aet.thesis.core.topic.constants.TopicState;
import de.tum.cit.aet.thesis.core.topic.entity.Topic;
import de.tum.cit.aet.thesis.core.topic.entity.TopicRole;
import de.tum.cit.aet.thesis.core.user.dto.MinimalUserDto;
import de.tum.cit.aet.thesis.thesis.constants.ThesisRoleName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TopicOverviewDto(
	UUID topicId,
	String title,
	TopicState state,
	Set<String> thesisTypes,
	Instant createdAt,
	Instant applicationDeadline,
	List<MinimalUserDto> supervisors,
	List<MinimalUserDto> examiners,
	MinimalResearchGroupDto researchGroup
) {

	public static TopicOverviewDto fromTopicEntity(Topic topic) {
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

		return new TopicOverviewDto(
			topic.getId(),
			topic.getTitle(),
			topic.getTopicState(),
			topic.getThesisTypes(),
			topic.getCreatedAt(),
			topic.getApplicationDeadline(),
			supervisors,
			examiners,
			MinimalResearchGroupDto.fromResearchGroupEntity(topic.getResearchGroup())
		);
	}
}
