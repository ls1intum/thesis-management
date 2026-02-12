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
	List<MinimalUserDto> advisors,
	List<MinimalUserDto> supervisors,
	MinimalResearchGroupDto researchGroup
) {

	public static TopicOverviewDto fromTopicEntity(Topic topic) {
		if (topic == null) {
			return null;
		}

		List<MinimalUserDto> advisors = new ArrayList<>();
		List<MinimalUserDto> supervisors = new ArrayList<>();

		for (TopicRole role : topic.getRoles()) {
			if (role.getId().getRole() == ThesisRoleName.ADVISOR) {
				advisors.add(MinimalUserDto.fromUserEntity(role.getUser()));
			} else if (role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
				supervisors.add(MinimalUserDto.fromUserEntity(role.getUser()));
			}
		}

		return new TopicOverviewDto(
			topic.getId(),
			topic.getTitle(),
			topic.getTopicState(),
			topic.getThesisTypes() == null || topic.getThesisTypes().isEmpty() ? null
				: topic.getThesisTypes(),
			topic.getCreatedAt(),
			advisors,
			supervisors,
			MinimalResearchGroupDto.fromResearchGroupEntity(topic.getResearchGroup())
		);
	}
}
