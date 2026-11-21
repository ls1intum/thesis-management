package de.tum.cit.aet.thesis.core.session;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.core.session.service.UserSessionTokens;
import de.tum.cit.aet.thesis.mock.BaseSessionIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs against a real embedded server over HTTP. MockMvc never performs the error dispatch that the servlet
 * container does after {@code sendError()} or an unhandled exception, so the status codes of such responses (403,
 * 429, 500) can only be checked here: the error dispatch is evaluated by the security chain again, and without its
 * own rule it would be refused as unauthenticated and every one of those statuses would turn into a 401.
 */
@Testcontainers
@Import(UserSessionRealServerTest.FailingEndpoint.class)
class UserSessionRealServerTest extends BaseSessionIntegrationTest {
	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
		// the production setting (the test resources have their own application.yml, which replaces the main one)
		registry.add("server.forward-headers-strategy", () -> "native");
	}

	@TestConfiguration
	@RestController
	static class FailingEndpoint {
		static final AtomicInteger CALLS = new AtomicInteger();

		@PostMapping("/v2/test-unhandled-failure")
		public String fail() {
			CALLS.incrementAndGet();
			throw new IllegalStateException("fails after its side effects");
		}
	}

	@LocalServerPort
	private int port;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> send(String method, String path, String cookie, String csrfToken, String... extraHeaders) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api" + path))
				.header(HttpHeaders.USER_AGENT, CHROME_MAC)
				.method(method, HttpRequest.BodyPublishers.noBody());

		if (cookie != null) {
			request.header(HttpHeaders.COOKIE, COOKIE_NAME + "=" + cookie);
		}

		if (csrfToken != null) {
			request.header("X-CSRF-Token", csrfToken);
		}

		for (int i = 0; i < extraHeaders.length; i += 2) {
			request.header(extraHeaders[i], extraHeaders[i + 1]);
		}

		return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void aDeniedRoleIsAnswered403NotAsAnUnauthenticatedRequest() throws Exception {
		Login student = login("real-student", "student");

		assertThat(send("GET", "/v2/users", student.cookieValue(), null).statusCode()).isEqualTo(403);
	}

	@Test
	void aMissingCsrfTokenIsAnswered403() throws Exception {
		Login student = login("real-csrf", "student");

		assertThat(send("DELETE", "/v2/auth/session", student.cookieValue(), null).statusCode()).isEqualTo(403);
		// the session was not ended by the refused request
		assertThat(send("GET", "/v2/auth/session", student.cookieValue(), null).statusCode()).isEqualTo(200);
	}

	@Test
	void anUnhandledFailureIsAnswered500AndTheRequestRanExactlyOnce() throws Exception {
		Login student = login("real-failure", "student");
		FailingEndpoint.CALLS.set(0);

		assertThat(send("POST", "/v2/test-unhandled-failure", student.cookieValue(), student.csrfToken()).statusCode()).isEqualTo(500);
		assertThat(FailingEndpoint.CALLS.get()).isEqualTo(1);
	}

	@Test
	void aThrottledAddressIsAnswered429WithRetryAfter() throws Exception {
		// an own forwarded address keeps the other tests, which all come from the loopback address, unthrottled
		String forwarded = "198.51.100.250";
		HttpResponse<String> response = null;

		for (int i = 0; i < 25; i++) {
			response = send("GET", "/v2/user-info", UserSessionTokens.generate().cookieValue(), null, "X-Forwarded-For", forwarded);
		}

		assertThat(response.statusCode()).isEqualTo(429);
		assertThat(response.headers().firstValue(HttpHeaders.RETRY_AFTER)).contains("60");
	}

	@Test
	void theClientAddressComesFromXForwardedForAndNeverFromTheForwardedHeader() throws Exception {
		createTestUser("real-address", List.of("student"));

		HttpRequest exchange = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v2/auth/session"))
				.header(HttpHeaders.AUTHORIZATION, keycloakBearer("real-address"))
				.header(HttpHeaders.USER_AGENT, CHROME_MAC)
				// Traefik passes a client-supplied "Forwarded" header through unchanged, so it must not be trusted
				.header("Forwarded", "for=203.0.113.99")
				.header("X-Forwarded-For", "198.51.100.7")
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();
		HttpResponse<String> created = http.send(exchange, HttpResponse.BodyHandlers.ofString());
		String setCookie = created.headers().firstValue(HttpHeaders.SET_COOKIE).orElseThrow();
		String cookie = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));

		HttpResponse<String> sessions = send("GET", "/v2/auth/sessions", cookie, null);
		JsonNode list = objectMapper.readTree(sessions.body());

		JsonNode current = null;

		for (JsonNode session : list) {
			if (session.get("current").asBoolean()) {
				current = session;
			}
		}

		assertThat(current).isNotNull();
		assertThat(current.get("ip").asString()).isEqualTo("198.51.100.7");
	}
}
