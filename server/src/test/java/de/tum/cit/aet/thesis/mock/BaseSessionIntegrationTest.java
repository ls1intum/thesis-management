package de.tum.cit.aet.thesis.mock;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

import jakarta.servlet.http.Cookie;

import java.util.List;

/**
 * Base class for tests that run the production security chains (session cookie, CSRF, Bearer only at the
 * session exchange). Unlike {@link BaseIntegrationTest}, which authenticates with a test token on every
 * endpoint, requests here must carry a real session obtained through {@code POST /v2/auth/session}.
 */
@ActiveProfiles(profiles = "session-test", inheritProfiles = false)
@Import(SessionTestSecurityConfig.class)
public abstract class BaseSessionIntegrationTest extends BaseIntegrationTest {
	protected static final String CHROME_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
	protected static final String COOKIE_NAME = "sid";

	/**
	 * A logged-in browser.
	 *
	 * @param universityId the user
	 * @param cookieValue the session cookie value
	 * @param csrfToken the CSRF token the server handed out
	 */
	public record Login(String universityId, String cookieValue, String csrfToken) {
	}

	protected String keycloakBearer(String universityId) {
		return generateTestAuthenticationHeader(universityId, List.of());
	}

	/** Creates the user with exactly the given roles and logs in through the session exchange. */
	protected Login login(String universityId, String... roles) throws Exception {
		createTestUser(universityId, List.of(roles));

		// creating the test user went through the session exchange; start from a clean slate so that the
		// returned login is the user's only session
		jdbcTemplateUpdate("DELETE FROM user_sessions WHERE user_id IN (SELECT user_id FROM users WHERE university_id = ?)", universityId);

		return exchange(universityId);
	}

	protected Login exchange(String universityId) throws Exception {
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/session")
						.header(HttpHeaders.AUTHORIZATION, keycloakBearer(universityId))
						.header(HttpHeaders.USER_AGENT, CHROME_MAC))
				.andExpect(status().isOk())
				.andReturn();

		String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		String cookieValue = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

		return new Login(universityId, cookieValue, body.get("csrfToken").asString());
	}

	/** Adds the session cookie, the User-Agent and, for unsafe methods, the CSRF token. */
	protected MockHttpServletRequestBuilder as(Login login, MockHttpServletRequestBuilder request) {
		request.cookie(new Cookie(COOKIE_NAME, login.cookieValue())).header(HttpHeaders.USER_AGENT, CHROME_MAC);

		HttpMethod method = HttpMethod.valueOf(request.buildRequest(new org.springframework.mock.web.MockServletContext()).getMethod());

		if (method != HttpMethod.GET && method != HttpMethod.HEAD && method != HttpMethod.OPTIONS) {
			request.header("X-CSRF-Token", login.csrfToken());
		}

		return request;
	}
}
