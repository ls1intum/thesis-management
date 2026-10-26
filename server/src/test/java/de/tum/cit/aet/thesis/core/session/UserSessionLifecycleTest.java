package de.tum.cit.aet.thesis.core.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.core.session.cron.UserSessionCleanup;
import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.entity.UserSessionEndReason;
import de.tum.cit.aet.thesis.core.session.repository.UserSessionRepository;
import de.tum.cit.aet.thesis.core.session.service.UserSessionTokens;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.core.user.service.AccessManagementService;
import de.tum.cit.aet.thesis.mock.BaseSessionIntegrationTest;
import de.tum.cit.aet.thesis.mock.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Testcontainers
@Import(UserSessionLifecycleTest.MutableClockTestConfig.class)
class UserSessionLifecycleTest extends BaseSessionIntegrationTest {
	private static final Instant START = Instant.parse("2026-03-02T09:00:00Z");

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@TestConfiguration
	static class MutableClockTestConfig {
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock();
		}
	}

	@Autowired
	private Clock clock;

	@Autowired
	private UserSessionRepository sessionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccessManagementService accessManagementService;

	@Autowired
	private UserSessionCleanup cleanup;

	@BeforeEach
	void pinClock() {
		((MutableClock) clock).setInstant(START);
	}

	@AfterEach
	void unfreezeClock() {
		((MutableClock) clock).unfreeze();
	}

	private String cookieValueOf(MvcResult result) {
		String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
	}

	@Test
	void roleChange_rotatesTheCookieOnTheNextRequest_oldCookieWorksOnlyInsideTheGraceWindow() throws Exception {
		Login login = login("rotate-user", "student");
		User user = userRepository.findByUniversityId("rotate-user").orElseThrow();

		accessManagementService.assignAdvisorRole(user);

		MvcResult first = mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/user-info")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.groups").value(org.hamcrest.Matchers.hasItem("advisor")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("sid=")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
				.andReturn();
		String rotated = cookieValueOf(first);
		assertThat(rotated).isNotEqualTo(login.cookieValue());
		Login rotatedLogin = new Login(login.universityId(), rotated, login.csrfToken());

		// the replacement works and keeps the CSRF token, so the web app needs no update
		mockMvc.perform(as(rotatedLogin, MockMvcRequestBuilders.get("/v2/auth/session")))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andExpect(jsonPath("$.csrfToken").value(login.csrfToken()));

		// a delayed request that still carries the previous value inside the grace window is not logged out
		((MutableClock) clock).setInstant(START.plusSeconds(29));
		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/auth/session")))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

		((MutableClock) clock).setInstant(START.plusSeconds(31));
		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/auth/session")))
				.andExpect(status().isUnauthorized())
				// the browser already holds the replacement; the stale request must not delete it
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
		mockMvc.perform(as(rotatedLogin, MockMvcRequestBuilders.get("/v2/auth/session")))
				.andExpect(status().isOk());
	}

	@Test
	void parallelRequestsDuringRotation_neitherFailsAndOnlyOneReplacesTheCookie() throws Exception {
		Login login = login("parallel-user", "student");
		accessManagementService.assignAdvisorRole(userRepository.findByUniversityId("parallel-user").orElseThrow());

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<MvcResult>> futures = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			Callable<MvcResult> request = () -> {
				start.await();
				return mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/auth/session"))).andReturn();
			};
			futures.add(executor.submit(request));
		}
		start.countDown();

		int rotations = 0;
		for (Future<MvcResult> future : futures) {
			MvcResult result = future.get();
			assertThat(result.getResponse().getStatus()).isEqualTo(200);
			if (result.getResponse().getHeader(HttpHeaders.SET_COOKIE) != null) {
				rotations++;
			}
		}
		executor.shutdown();

		assertThat(rotations).isEqualTo(1);
		assertThat(sessionRepository.findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(
				userRepository.findByUniversityId("parallel-user").orElseThrow().getId())).hasSize(1);
	}

	@Test
	void accountDeletion_endsTheSessionsOfTheUser() throws Exception {
		Login login = login("deleted-user", "student");
		Login second = exchange("deleted-user");

		mockMvc.perform(as(login, MockMvcRequestBuilders.delete("/v2/user-deletion/me")))
				.andExpect(status().isOk());

		mockMvc.perform(as(login, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isUnauthorized());
		mockMvc.perform(as(second, MockMvcRequestBuilders.get("/v2/auth/session"))).andExpect(status().isUnauthorized());
		User user = userRepository.findByUniversityId("deleted-user").orElseThrow();
		assertThat(sessionRepository.findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(user.getId())).isEmpty();
	}

	@Test
	void invalidCookiesFromOneIp_areThrottled_butValidSessionsAndAnonymousRequestsAreNot() throws Exception {
		Login valid = login("limiter-user", "student");
		String ip = "198.51.100.9";

		for (int i = 0; i < 20; i++) {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
							.cookie(new Cookie(COOKIE_NAME, UserSessionTokens.generate().cookieValue()))
							.with(request -> {
								request.setRemoteAddr(ip);
								return request;
							}))
					.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.cookie(new Cookie(COOKIE_NAME, UserSessionTokens.generate().cookieValue()))
						.with(request -> {
							request.setRemoteAddr(ip);
							return request;
						}))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER));

		// the same address with a real session, and without any cookie, keeps working
		mockMvc.perform(as(valid, MockMvcRequestBuilders.get("/v2/user-info")).with(request -> {
					request.setRemoteAddr(ip);
					return request;
				}))
				.andExpect(status().isOk());
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics").param("onlyOwnResearchGroup", "false").with(request -> {
					request.setRemoteAddr(ip);
					return request;
				}))
				.andExpect(status().isOk());

		// another address is not affected, and the block lifts after a minute without new guesses
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.cookie(new Cookie(COOKIE_NAME, UserSessionTokens.generate().cookieValue())))
				.andExpect(status().isUnauthorized());
		((MutableClock) clock).setInstant(START.plus(Duration.ofMinutes(2)));
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.cookie(new Cookie(COOKIE_NAME, UserSessionTokens.generate().cookieValue()))
						.with(request -> {
							request.setRemoteAddr(ip);
							return request;
						}))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cleanupJob_deletesSessionsThatEndedLongAgo() throws Exception {
		Login login = login("cleanup-user", "student");
		User user = userRepository.findByUniversityId("cleanup-user").orElseThrow();
		UserSession session = sessionRepository.findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(user.getId()).getFirst();
		session.setEndedAt(START.minus(Duration.ofDays(31)));
		session.setEndReason(UserSessionEndReason.LOGOUT);
		sessionRepository.save(session);

		cleanup.run();

		assertThat(sessionRepository.findById(session.getId())).isEmpty();
		assertThat(login.universityId()).isEqualTo("cleanup-user");
	}
}
