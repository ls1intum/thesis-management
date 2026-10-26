package de.tum.cit.aet.thesis.core.session.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * What the web app learns about its session. The session credential itself is never part of a response; it
 * travels only in the HttpOnly cookie.
 *
 * @param csrfToken the token to send in the {@code X-CSRF-Token} header on state-changing requests
 * @param expiresAt when the session ends at the latest (absolute timeout)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record SessionInfoDto(String csrfToken, Instant expiresAt) {
}
