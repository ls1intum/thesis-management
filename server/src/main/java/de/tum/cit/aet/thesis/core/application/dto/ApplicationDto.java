package de.tum.cit.aet.thesis.core.application.dto;

import de.tum.cit.aet.thesis.core.application.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.core.application.constants.ApplicationReviewReason;
import de.tum.cit.aet.thesis.core.application.constants.ApplicationState;
import de.tum.cit.aet.thesis.core.application.entity.Application;
import de.tum.cit.aet.thesis.core.application.entity.ApplicationReviewer;
import de.tum.cit.aet.thesis.core.group.dto.LightResearchGroupDto;
import de.tum.cit.aet.thesis.core.topic.dto.TopicDto;
import de.tum.cit.aet.thesis.core.user.dto.LightUserDto;
import de.tum.cit.aet.thesis.core.user.dto.UserDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplicationDto(
	UUID applicationId,
	UserDto user,
	TopicDto topic,
	String thesisTitle,
	String thesisType,
	String motivation,
	ApplicationState state,
	Instant desiredStartDate,
	String comment,
	ApplicationRejectReason rejectReason,
	Instant createdAt,
	List<ApplicationReviewerDto> reviewers,
	Instant reviewedAt,
	LightResearchGroupDto researchGroup
) {

public static ApplicationDto fromApplicationEntity(Application application,
	boolean protectedData) {
	if (application == null) {
	return null;
	}

	return new ApplicationDto(
		application.getId(),
		UserDto.fromUserEntity(application.getUser()),
		TopicDto.fromTopicEntity(application.getTopic()),
		application.getTopic() != null ? application.getTopic().getTitle()
			: application.getThesisTitle(),
		application.getThesisType(),
		application.getMotivation(),
		application.getState(),
		application.getDesiredStartDate(),
		protectedData ? application.getComment() : null,
		application.getRejectReason(),
		application.getCreatedAt(),
		protectedData ? application.getReviewers().stream()
			.map(ApplicationReviewerDto::fromApplicationReviewerEntity).toList() : null,
		application.getReviewedAt(),
		LightResearchGroupDto.fromResearchGroupEntity(application.getResearchGroup())
	);
}

public record ApplicationReviewerDto(
	LightUserDto user,
	ApplicationReviewReason reason,
	Instant reviewedAt
) {

	public static ApplicationReviewerDto fromApplicationReviewerEntity(
		ApplicationReviewer reviewer) {
	if (reviewer == null) {
		return null;
	}

	return new ApplicationReviewerDto(
		LightUserDto.fromUserEntity(reviewer.getUser()),
		reviewer.getReason(),
		reviewer.getReviewedAt()
	);
	}
}
}
