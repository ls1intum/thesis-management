package de.tum.cit.aet.thesis.core.session.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.core.session.repository.UserSessionRepository;
import de.tum.cit.aet.thesis.core.session.service.UserSessionTokens;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.mock.BaseSessionIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import jakarta.servlet.http.Cookie;

import java.time.Instant;

@Testcontainers
class UserSessionControllerTest extends BaseSessionIntegrationTest {
	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private UserSessionRepository sessionRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void exchange_createsSessionAndReturnsCookieWithExactFlagsAndCsrfToken() throws Exception {
		createTestUser("exchange-user", java.util.List.of("student"));

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/session")
						.header(HttpHeaders.AUTHORIZATION, keycloakBearer("exchange-user"))
						.header(HttpHeaders.USER_AGENT, CHROME_MAC))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
				.andExpect(jsonPath("$.csrfToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresAt").isNotEmpty())
				.andReturn();

		String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookie).startsWith("sid=")
				.contains("HttpOnly")
				.contains("SameSite=Strict")
				.contains("Path=/")
				.doesNotContain("Domain")
				.doesNotContain("Secure");
		assertThat(setCookie).containsPattern("Max-Age=8639[0-9]|Max-Age=86400");

		String cookieValue = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
		var parsed = UserSessionTokens.parse(cookieValue).orElseThrow();
		var row = sessionRepository.findByTokenIdentifier(parsed.identifier()).orElseThrow();
		assertThat(row.getVerifierHash()).hasSize(32).isNotEqualTo(parsed.verifier().getBytes());
		assertThat(row.getUserAgentSignature()).isEqualTo("chrome|macos");
	}

	@Test
	void exchange_newUserGetsStudentGroupBeforeTheSessionExists() throws Exception {
		Login login = exchange("brand-new-user");

		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/user-info")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.universityId").value("brand-new-user"))
				.andExpect(jsonPath("$.groups", hasItem("student")));
	}

	@Test
	void exchange_anonymisedUserIsRejectedAndNoSessionIsCreated() throws Exception {
		createTestUser("anonymised-user", java.util.List.of("student"));
		User user = userRepository.findByUniversityId("anonymised-user").orElseThrow();
		user.setAnonymizedAt(Instant.now());
		userRepository.save(user);

		long sessionsBefore = sessionRepository.count();

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/session")
						.header(HttpHeaders.AUTHORIZATION, keycloakBearer("anonymised-user")))
				.andExpect(status().isForbidden());

		assertThat(sessionRepository.count()).isEqualTo(sessionsBefore);
	}

	@Test
	void exchange_requiresAValidKeycloakToken() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/session"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/session")
						.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void session_authenticatesAndRolesComeFromUserGroups() throws Exception {
		Login admin = login("role-admin", "admin");
		Login student = login("role-student", "student");

		mockMvc.perform(as(admin, MockMvcRequestBuilders.get("/v2/users"))).andExpect(status().isOk());
		mockMvc.perform(as(student, MockMvcRequestBuilders.get("/v2/users"))).andExpect(status().isForbidden());

		// a role change takes effect on the next request, with the same cookie, without a new login
		jdbcTemplateUpdate("INSERT INTO user_groups (user_id, \"group\") SELECT user_id, 'admin' FROM users WHERE university_id = 'role-student'");
		mockMvc.perform(as(student, MockMvcRequestBuilders.get("/v2/users"))).andExpect(status().isOk());
	}

	@Test
	void bearerTokenIsRejectedEverywhereExceptTheExchange() throws Exception {
		login("bearer-user", "student");

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header(HttpHeaders.AUTHORIZATION, keycloakBearer("bearer-user")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/auth/session")
						.header(HttpHeaders.AUTHORIZATION, keycloakBearer("bearer-user")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getSession_returnsCsrfTokenForTheCurrentSession() throws Exception {
		Login login = login("get-session-user", "student");

		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/auth/session")))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
				.andExpect(jsonPath("$.csrfToken").value(login.csrfToken()));
	}

	@Test
	void getSession_withoutCookieIs401() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/auth/session"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unsafeRequestWithSessionButWithoutOrWithWrongCsrfTokenIs403() throws Exception {
		Login login = login("csrf-user", "student");
		Cookie cookie = new Cookie(COOKIE_NAME, login.cookieValue());

		mockMvc.perform(MockMvcRequestBuilders.put("/v2/user-info/notifications")
						.cookie(cookie)
						.header(HttpHeaders.USER_AGENT, CHROME_MAC)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"x\",\"email\":\"a@b.c\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(MockMvcRequestBuilders.put("/v2/user-info/notifications")
						.cookie(cookie)
						.header(HttpHeaders.USER_AGENT, CHROME_MAC)
						.header("X-CSRF-Token", "wrong")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"x\",\"email\":\"a@b.c\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(MockMvcRequestBuilders.delete("/v2/auth/session").cookie(cookie).header(HttpHeaders.USER_AGENT, CHROME_MAC))
				.andExpect(status().isForbidden());
	}

	@Test
	void unsafeRequestWithoutAnyCookieIs401NotACsrfFailure() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/v2/auth/session"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(MockMvcRequestBuilders.put("/v2/user-info/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unknownCookieIsClearedProtectedRequestsAre401AndPublicOnesStillWork() throws Exception {
		Cookie stale = new Cookie(COOKIE_NAME, UserSessionTokens.generate().cookieValue());

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info").cookie(stale))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics").param("onlyOwnResearchGroup", "false").cookie(stale))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
	}

	@Test
	void logout_endsTheSessionAndTheCookieNoLongerWorks() throws Exception {
		Login login = login("logout-user", "student");

		mockMvc.perform(as(login, MockMvcRequestBuilders.delete("/v2/auth/session")))
				.andExpect(status().isNoContent())
				.andExpect(header().string("Clear-Site-Data", "\"cache\""))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/auth/session")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/user-info")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userInfoIsAPlainRead_itNeitherSyncsTheProfileNorTouchesLastLogin() throws Exception {
		Login login = login("plain-read-user", "student");
		jdbcTemplateUpdate("UPDATE users SET last_login_at = '2020-01-01 00:00:00', first_name = 'Stale' WHERE university_id = 'plain-read-user'");

		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/user-info")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Stale"));

		User user = userRepository.findByUniversityId("plain-read-user").orElseThrow();
		assertThat(user.getLastLoginAt()).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
	}

	private JsonNode listSessions(Login login) throws Exception {
		String body = mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/auth/sessions")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(body);
	}

	@Test
	void listSessions_returnsOnlyOwnSessionsAndMarksTheCurrentOne() throws Exception {
		Login first = login("list-user", "student");
		Login second = exchange("list-user");
		Login stranger = login("list-stranger", "student");

		var sessions = listSessions(first);

		assertThat(sessions).hasSize(2);
		long current = 0;
		for (var session : sessions) {
			assertThat(session.get("device").asString()).isEqualTo("Chrome on macOS");
			assertThat(session.get("id").asString()).isNotBlank();
			assertThat(session.has("createdAt")).isTrue();
			assertThat(session.has("lastUsedAt")).isTrue();
			if (session.get("current").asBoolean()) {
				current++;
			}
		}
		assertThat(current).isEqualTo(1);
		assertThat(listSessions(second)).hasSize(2);
		assertThat(listSessions(stranger)).hasSize(1);
		assertThat(sessions.toString()).doesNotContain(first.cookieValue()).doesNotContain(second.cookieValue());
	}

	@Test
	void endOtherSession_endsItAndCannotReachAnotherUsersSession() throws Exception {
		Login first = login("end-user", "student");
		Login second = exchange("end-user");
		Login stranger = login("end-stranger", "student");

		String strangerSessionId = listSessions(stranger).get(0).get("id").asString();
		String otherSessionId = null;
		for (var session : listSessions(first)) {
			if (!session.get("current").asBoolean()) {
				otherSessionId = session.get("id").asString();
			}
		}

		mockMvc.perform(as(first, MockMvcRequestBuilders.delete("/v2/auth/sessions/" + strangerSessionId)))
				.andExpect(status().isNotFound());
		mockMvc.perform(as(stranger, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isOk());

		mockMvc.perform(as(first, MockMvcRequestBuilders.delete("/v2/auth/sessions/" + otherSessionId)))
				.andExpect(status().isNoContent());
		mockMvc.perform(as(second, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isUnauthorized());
		mockMvc.perform(as(first, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isOk());
	}

	@Test
	void endAllOtherSessions_keepsTheCurrentOne() throws Exception {
		Login first = login("end-all-user", "student");
		Login second = exchange("end-all-user");
		Login third = exchange("end-all-user");

		mockMvc.perform(as(second, MockMvcRequestBuilders.delete("/v2/auth/sessions")))
				.andExpect(status().isNoContent());

		mockMvc.perform(as(first, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isUnauthorized());
		mockMvc.perform(as(third, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isUnauthorized());
		mockMvc.perform(as(second, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isOk());
	}

	@Test
	void endingTheCurrentSessionThroughTheListClearsTheCookie() throws Exception {
		Login login = login("end-current-user", "student");
		String currentId = listSessions(login).get(0).get("id").asString();

		mockMvc.perform(as(login, MockMvcRequestBuilders.delete("/v2/auth/sessions/" + currentId)))
				.andExpect(status().isNoContent())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isUnauthorized());
	}
}
