package de.tum.cit.aet.thesis.core.session.security;

import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.AuthResult;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Authenticated;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.ClientInfo;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Reason;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Rejected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/**
 * Authenticates requests from the session cookie. A request without cookie, or with a cookie that does not
 * belong to a valid session, continues anonymously: public endpoints keep working and protected ones answer
 * 401 through the normal entry point. A cookie that is no longer valid is cleared in the response.
 */
public class UserSessionAuthenticationFilter extends OncePerRequestFilter {
	/** Request attribute holding the {@link UserSession} that authenticated the request. */
	public static final String SESSION_ATTRIBUTE = "de.tum.cit.aet.thesis.core.session.SESSION";

	private static final Logger log = LoggerFactory.getLogger(UserSessionAuthenticationFilter.class);

	private final UserSessionService sessionService;
	private final UserSessionCookies cookies;
	private final UserAuthorities authorities;
	private final Clock clock;
	private final InvalidSessionTokenLimiter limiter;

	/**
	 * Creates the filter.
	 *
	 * @param sessionService the session service
	 * @param cookies the cookie factory
	 * @param authorities the loader for the user's roles
	 * @param clock the clock used to compute the cookie lifetime
	 * @param limiter the detector for guessing of session cookies
	 */
	public UserSessionAuthenticationFilter(
			UserSessionService sessionService,
			UserSessionCookies cookies,
			UserAuthorities authorities,
			Clock clock,
			InvalidSessionTokenLimiter limiter
	) {
		this.sessionService = sessionService;
		this.cookies = cookies;
		this.authorities = authorities;
		this.clock = clock;
		this.limiter = limiter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Optional<String> cookieValue = cookies.read(request);

		if (cookieValue.isEmpty()) {
			chain.doFilter(request, response);
			return;
		}

		ClientInfo client = ClientInfo.from(request);
		AuthResult result = sessionService.authenticate(cookieValue.get(), client);

		if (result instanceof Rejected rejected) {
			handleRejected(rejected.reason(), client, request, response);

			if (rejected.reason().countsAsGuess()) {
				limiter.recordGuess(client.ip());

				if (limiter.isThrottled(client.ip())) {
					log.warn("Throttling {}: too many invalid session cookies", client.ip());
					response.setHeader(HttpHeaders.RETRY_AFTER, "60");
					response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
					return;
				}
			}

			chain.doFilter(request, response);
			return;
		}

		Authenticated authenticated = (Authenticated) result;
		UserSession session = authenticated.session();

		authenticated.newCookieValue().ifPresent(value -> response.addHeader(
				HttpHeaders.SET_COOKIE,
				cookies.create(value, Duration.between(clock.instant(), session.getExpiresAt())).toString()));

		request.setAttribute(SESSION_ATTRIBUTE, session);

		String universityId = authenticated.user().getUniversityId();
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UserSessionAuthentication(universityId, session, authorities.forUser(authenticated.user())));
		SecurityContextHolder.setContext(context);

		chain.doFilter(request, response);
	}

	private void handleRejected(Reason reason, ClientInfo client, HttpServletRequest request, HttpServletResponse response) {
		if (reason.countsAsGuess()) {
			// the signature of guessing: worth an alert, with the source for correlation
			log.warn("Rejected session cookie ({}) from {} for {} {}", reason, client.ip(), request.getMethod(), request.getRequestURI());
		} else {
			log.info("Rejected session cookie ({}) for {} {}", reason, request.getMethod(), request.getRequestURI());
		}

		// After a rotation the browser may already hold the replacement cookie while a delayed request still
		// carries the old value; clearing the cookie then would log the user out.
		if (reason != Reason.SUPERSEDED_EXPIRED) {
			response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear().toString());
		}
	}
}
