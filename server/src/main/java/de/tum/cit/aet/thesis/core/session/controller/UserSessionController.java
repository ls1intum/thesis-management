package de.tum.cit.aet.thesis.core.session.controller;

import de.tum.cit.aet.thesis.core.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.core.session.dto.SessionInfoDto;
import de.tum.cit.aet.thesis.core.session.dto.UserSessionDto;
import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.entity.UserSessionEndReason;
import de.tum.cit.aet.thesis.core.session.security.UserSessionAuthenticationFilter;
import de.tum.cit.aet.thesis.core.session.security.UserSessionCookies;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.ClientInfo;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Created;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.service.AuthenticationService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoints for the application-owned session. Keycloak proves who the user is once, at
 * {@code POST /v2/auth/session}; every other request is authenticated by the session cookie.
 */
@RestController
@RequestMapping("/v2/auth")
public class UserSessionController {
	private final AuthenticationService authenticationService;
	private final UserSessionService sessionService;
	private final UserSessionCookies cookies;
	private final Clock clock;

	/**
	 * Creates the controller.
	 *
	 * @param authenticationService the service that syncs the user from the Keycloak token
	 * @param sessionService the session service
	 * @param cookies the cookie factory
	 * @param clock the clock used to compute the cookie lifetime
	 */
	public UserSessionController(AuthenticationService authenticationService, UserSessionService sessionService, UserSessionCookies cookies, Clock clock) {
		this.authenticationService = authenticationService;
		this.sessionService = sessionService;
		this.cookies = cookies;
		this.clock = clock;
	}

	/**
	 * Exchanges a verified Keycloak access token for a session. This is the only endpoint that accepts a
	 * Bearer token. It creates or updates the user from the token's claims, assigns the default student
	 * group to new users, and then creates the session.
	 *
	 * @param jwt the validated Keycloak token
	 * @param request the request
	 * @return the CSRF token and expiry; the session itself is in the {@code Set-Cookie} header
	 */
	@PostMapping("/session")
	public ResponseEntity<SessionInfoDto> createSession(JwtAuthenticationToken jwt, HttpServletRequest request) {
		User user = authenticationService.updateAuthenticatedUser(jwt);

		// Logging in while the browser still holds a session of this user replaces it, so logging in twice does not
		// leave an orphaned session. A cookie of a different user (shared computer, no logout) is not touched: it
		// belongs to that user, is overwritten by this response and ends with that user's idle timeout.
		cookies.read(request).ifPresent(cookieValue -> sessionService.replaceExisting(cookieValue, user));

		Created created = sessionService.create(user, ClientInfo.from(request));

		return sessionResponse(created.session(), cookies.create(created.cookieValue(), remainingLifetime(created.session())));
	}

	/**
	 * Tells the web app whether it has a valid session and hands out the CSRF token.
	 *
	 * @param session the session that authenticated the request
	 * @return the CSRF token and expiry
	 */
	@GetMapping("/session")
	public ResponseEntity<SessionInfoDto> getSession(@RequestAttribute(UserSessionAuthenticationFilter.SESSION_ATTRIBUTE) UserSession session) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(new SessionInfoDto(session.getCsrfToken(), session.getExpiresAt(), session.getUserId()));
	}

	/**
	 * Ends the current session on the server and clears the cookie.
	 *
	 * @param session the session that authenticated the request
	 * @return an empty response
	 */
	@DeleteMapping("/session")
	public ResponseEntity<Void> deleteSession(@RequestAttribute(UserSessionAuthenticationFilter.SESSION_ATTRIBUTE) UserSession session) {
		sessionService.end(session, UserSessionEndReason.LOGOUT);

		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
				.header("Clear-Site-Data", "\"cache\"")
				.cacheControl(CacheControl.noStore())
				.build();
	}

	/**
	 * Lists the caller's active sessions (devices and browsers).
	 *
	 * @param current the session that authenticated the request
	 * @return the active sessions, oldest first
	 */
	@GetMapping("/sessions")
	public ResponseEntity<List<UserSessionDto>> listSessions(@RequestAttribute(UserSessionAuthenticationFilter.SESSION_ATTRIBUTE) UserSession current) {
		Set<UUID> currentIds = sessionService.currentAndSuccessors(current.getId());
		List<UserSessionDto> sessions = sessionService.listActive(current.getUserId()).stream()
				.map(session -> UserSessionDto.from(session, currentIds))
				.toList();

		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(sessions);
	}

	/**
	 * Ends one of the caller's own sessions, for example on a lost device. Sessions of other users cannot be
	 * reached and are reported as not found.
	 *
	 * @param id the session row id
	 * @param current the session that authenticated the request
	 * @return an empty response; the cookie is cleared if the caller ended their own current session
	 */
	@DeleteMapping("/sessions/{id}")
	public ResponseEntity<Void> endSession(@PathVariable UUID id, @RequestAttribute(UserSessionAuthenticationFilter.SESSION_ATTRIBUTE) UserSession current) {
		if (!sessionService.endOwn(current.getUserId(), id)) {
			throw new ResourceNotFoundException("Session not found");
		}

		ResponseEntity.HeadersBuilder<?> response = ResponseEntity.noContent();

		if (id.equals(current.getId())) {
			response.header(HttpHeaders.SET_COOKIE, cookies.clear().toString());
		}

		return response.build();
	}

	/**
	 * Ends all of the caller's sessions except the one that made the request.
	 *
	 * @param current the session that authenticated the request
	 * @return an empty response
	 */
	@DeleteMapping("/sessions")
	public ResponseEntity<Void> endOtherSessions(@RequestAttribute(UserSessionAuthenticationFilter.SESSION_ATTRIBUTE) UserSession current) {
		sessionService.endOthers(current.getUserId(), current.getId());

		return ResponseEntity.noContent().build();
	}

	private ResponseEntity<SessionInfoDto> sessionResponse(UserSession session, ResponseCookie cookie) {
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.cacheControl(CacheControl.noStore())
				.body(new SessionInfoDto(session.getCsrfToken(), session.getExpiresAt(), session.getUserId()));
	}

	private Duration remainingLifetime(UserSession session) {
		return Duration.between(clock.instant(), session.getExpiresAt());
	}
}
