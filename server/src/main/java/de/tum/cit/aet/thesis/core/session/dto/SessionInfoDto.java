package de.tum.cit.aet.thesis.core.session.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * What the web app learns about its session. The session credential itself is never part of a response; it
 * travels only in the HttpOnly cookie.
 *
 * @param csrfToken the token to send in the {@code X-CSRF-Token} header on state-changing requests
 * @param expiresAt when the session ends at the latest (absolute timeout)
 * @param userId the user the session belongs to; the web app compares it before replaying a request after the
 *               session was replaced, so that one user's request is never executed as another user
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record SessionInfoDto(String csrfToken, Instant expiresAt, UUID userId) {
}
