package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LightUserDto(
	UUID userId,
	String avatar,
	String universityId,
	String matriculationNumber,
	String firstName,
	String lastName,
	String email,
	String studyDegree,
	String studyProgram,
	Map<String, String> customData,
	Instant joinedAt,
	List<String> groups
) {
	public static LightUserDto fromUserEntity(User user) {
		if (user == null) {
			return null;
		}

		return new LightUserDto(
				user.getId(), user.getAdjustedAvatar(), user.getUniversityId(), user.getMatriculationNumber(),
				user.getFirstName(), user.getLastName(), user.getEmail() != null ? user.getEmail().toString() : null, user.getStudyDegree(), user.getStudyProgram(),
				user.getCustomData(),
				user.getJoinedAt(), user.getGroups().stream().map(x -> x.getId().getGroup()).toList()
		);
	}
}
