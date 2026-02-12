package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.entity.User;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MinimalUserDto(UUID userId, String firstName, String lastName, String avatar) {
	public static MinimalUserDto fromUserEntity(User user) {
		if (user == null) {
			return null;
		}

		return new MinimalUserDto(
			user.getId(),
			user.getFirstName(),
			user.getLastName(),
			user.getAdjustedAvatar()
		);
	}
}
