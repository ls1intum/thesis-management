package de.tum.cit.aet.thesis.core.session.security;

import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.service.UserSessionTokens;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stores the CSRF token with the session instead of in a cookie. The token reaches the web app in the JSON
 * body of the session endpoints, and the web app sends it back in the {@code X-CSRF-Token} header, which a
 * cross-site request cannot do.
 */
public class SessionCsrfTokenRepository implements CsrfTokenRepository {
	/** Name of the request header carrying the token. */
	public static final String HEADER_NAME = "X-CSRF-Token";

	private static final String PARAMETER_NAME = "_csrf";
	private static final int CSRF_TOKEN_BYTES = 32;

	@Override
	public CsrfToken generateToken(HttpServletRequest request) {
		// Only reached for requests without a session, which never need a valid token. The token is not stored.
		return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, UserSessionTokens.randomToken(CSRF_TOKEN_BYTES));
	}

	@Override
	public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
		// the token lives and dies with the session row
	}

	@Override
	public CsrfToken loadToken(HttpServletRequest request) {
		Object session = request.getAttribute(UserSessionAuthenticationFilter.SESSION_ATTRIBUTE);

		if (session instanceof UserSession userSession) {
			return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, userSession.getCsrfToken());
		}

		return null;
	}
}
