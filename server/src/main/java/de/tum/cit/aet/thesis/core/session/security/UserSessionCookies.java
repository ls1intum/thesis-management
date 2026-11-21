package de.tum.cit.aet.thesis.core.session.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.Optional;

/**
 * Builds and reads the session cookie: {@code HttpOnly}, {@code Secure}, {@code SameSite=Strict}, path
 * {@code /}, no {@code Domain}. Over https the {@code __Host-} prefix makes browsers enforce those
 * attributes; for plain-http local development the prefix and {@code Secure} are dropped, because browsers
 * reject them there. Which of the two applies follows the scheme of {@code thesis-management.client.host}.
 */
@Component
public class UserSessionCookies {
	private final boolean secure;
	private final String name;

	/**
	 * Creates the cookie factory.
	 *
	 * @param clientHost the public URL of the web app
	 */
	public UserSessionCookies(@Value("${thesis-management.client.host}") String clientHost) {
		this.secure = clientHost.startsWith("https://");
		this.name = secure ? "__Host-sid" : "sid";
	}

	/**
	 * Returns the cookie name.
	 *
	 * @return {@code __Host-sid} over https, {@code sid} over http
	 */
	public String name() {
		return name;
	}

	/**
	 * Reads the session cookie value from a request.
	 *
	 * @param request the request
	 * @return the raw value, if the cookie is present
	 */
	public Optional<String> read(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();

		if (cookies == null) {
			return Optional.empty();
		}

		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return Optional.ofNullable(cookie.getValue());
			}
		}

		return Optional.empty();
	}

	/**
	 * Builds the cookie that carries a session.
	 *
	 * @param value the credential
	 * @param maxAge the remaining absolute lifetime of the session
	 * @return the cookie
	 */
	public ResponseCookie create(String value, Duration maxAge) {
		return builder(value).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
	}

	/**
	 * Builds a cookie that makes the browser delete the session cookie.
	 *
	 * @return the expiring cookie
	 */
	public ResponseCookie clear() {
		return builder("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder builder(String value) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(secure)
				.sameSite("Strict")
				.path("/");
	}
}
