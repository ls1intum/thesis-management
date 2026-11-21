package de.tum.cit.aet.thesis.core.session.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.service.UserAgentSignature;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * One of a user's active sessions, as shown in the settings. The id is the internal row id and cannot be used
 * to authenticate; the session credential is never exposed.
 *
 * @param id the session row id, used to end the session
 * @param createdAt when the user logged in
 * @param lastUsedAt when the session was last used (rounded to the write interval)
 * @param ip the last IP address the session was used from
 * @param device a readable label such as "Chrome on macOS"
 * @param current whether this is the session that made the request
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record UserSessionDto(UUID id, Instant createdAt, Instant lastUsedAt, String ip, String device, boolean current) {
	/**
	 * Converts a session for the list of the user's sessions.
	 *
	 * @param session the session
	 * @param currentSessionIds the id of the session that made the request and of the sessions it was rotated into
	 * @return the DTO
	 */
	public static UserSessionDto from(UserSession session, Set<UUID> currentSessionIds) {
		return new UserSessionDto(
				session.getId(),
				session.getCreatedAt(),
				session.getLastUsedAt(),
				session.getLastIp(),
				UserAgentSignature.label(session.getUserAgentSignature()),
				currentSessionIds.contains(session.getId())
		);
	}
}
