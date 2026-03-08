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
public record TopicOverviewDto(
	UUID topicId,
	String title,
	TopicState state,
	Set<String> thesisTypes,
	Instant createdAt,
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
			supervisors,
			examiners,
			MinimalResearchGroupDto.fromResearchGroupEntity(topic.getResearchGroup())
		);
	}
}
