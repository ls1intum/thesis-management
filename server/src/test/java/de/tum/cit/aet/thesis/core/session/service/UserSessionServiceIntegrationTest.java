package de.tum.cit.aet.thesis.core.session.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.entity.UserSessionEndReason;
import de.tum.cit.aet.thesis.core.session.repository.UserSessionRepository;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.AuthResult;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Authenticated;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.ClientInfo;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Created;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Reason;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Rejected;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.mock.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Testcontainers
@Import(UserSessionServiceIntegrationTest.MutableClockTestConfig.class)
class UserSessionServiceIntegrationTest extends BaseIntegrationTest {
	private static final String CHROME_MAC_130 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
	private static final String CHROME_MAC_131 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
	private static final String FIREFOX_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:132.0) Gecko/20100101 Firefox/132.0";
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
	private UserSessionService sessionService;

	@Autowired
	private UserSessionRepository sessionRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void pinClock() {
		((MutableClock) clock).setInstant(START);
	}

	@AfterEach
	void unfreezeClock() {
		((MutableClock) clock).unfreeze();
	}

	private void at(Duration sinceStart) {
		((MutableClock) clock).setInstant(START.plus(sinceStart));
	}

	private User createUser(String universityId) {
		User user = new User();
		user.setUniversityId(universityId);
		user.setJoinedAt(Instant.now());
		user.setUpdatedAt(Instant.now());
		return userRepository.save(user);
	}

	private ClientInfo client(String userAgent) {
		return new ClientInfo("203.0.113.7", userAgent);
	}

	private AuthResult authenticate(Created created) {
		return sessionService.authenticate(created.cookieValue(), client(CHROME_MAC_130));
	}

	private Rejected rejected(AuthResult result) {
		assertThat(result).isInstanceOf(Rejected.class);
		return (Rejected) result;
	}

	private Authenticated authenticated(AuthResult result) {
		assertThat(result).isInstanceOf(Authenticated.class);
		return (Authenticated) result;
	}

	@Test
	void authenticate_validSession_touchesLastUsedOnlyAfterWriteInterval() {
		User user = createUser("svc-touch");
		Created created = sessionService.create(user, client(CHROME_MAC_130));

		at(Duration.ofMinutes(1));
		Authenticated first = authenticated(authenticate(created));
		assertThat(first.user().getId()).isEqualTo(user.getId());
		assertThat(first.newCookieValue()).isEmpty();
		assertThat(sessionRepository.findById(created.session().getId()).orElseThrow().getLastUsedAt()).isEqualTo(START);

		at(Duration.ofMinutes(6));
		authenticated(authenticate(created));
		assertThat(sessionRepository.findById(created.session().getId()).orElseThrow().getLastUsedAt())
				.isEqualTo(START.plus(Duration.ofMinutes(6)));
	}

	@Test
	void authenticate_idleBoundary() {
		User user = createUser("svc-idle");
		Created valid = sessionService.create(user, client(CHROME_MAC_130));
		Created expired = sessionService.create(user, client(CHROME_MAC_130));

		at(Duration.ofHours(4).minusSeconds(1));
		authenticated(authenticate(valid));

		at(Duration.ofHours(4));
		assertThat(rejected(authenticate(expired)).reason()).isEqualTo(Reason.EXPIRED_IDLE);
		UserSession row = sessionRepository.findById(expired.session().getId()).orElseThrow();
		assertThat(row.getEndedAt()).isNotNull();
		assertThat(row.getEndReason()).isEqualTo(UserSessionEndReason.EXPIRED);
	}

	@Test
	void authenticate_absoluteBoundary_evenWhenActive() {
		User user = createUser("svc-absolute");
		Created created = sessionService.create(user, client(CHROME_MAC_130));

		for (int hours = 3; hours <= 21; hours += 3) {
			at(Duration.ofHours(hours));
			authenticated(authenticate(created));
		}

		at(Duration.ofHours(24).minusSeconds(1));
		authenticated(authenticate(created));

		at(Duration.ofHours(24));
		assertThat(rejected(authenticate(created)).reason()).isEqualTo(Reason.EXPIRED_ABSOLUTE);
	}

	@Test
	void authenticate_invalidCookies_areClassifiedAndOnlyGuessesCount() {
		User user = createUser("svc-guess");
		Created created = sessionService.create(user, client(CHROME_MAC_130));
		String identifier = UserSessionTokens.parse(created.cookieValue()).orElseThrow().identifier();

		Rejected malformed = rejected(sessionService.authenticate("garbage", client(CHROME_MAC_130)));
		Rejected unknown = rejected(sessionService.authenticate(UserSessionTokens.generate().cookieValue(), client(CHROME_MAC_130)));
		Rejected badVerifier = rejected(sessionService.authenticate(identifier + "." + UserSessionTokens.generate().verifier(), client(CHROME_MAC_130)));

		assertThat(malformed.reason()).isEqualTo(Reason.MALFORMED);
		assertThat(unknown.reason()).isEqualTo(Reason.UNKNOWN);
		assertThat(badVerifier.reason()).isEqualTo(Reason.BAD_VERIFIER);
		assertThat(List.of(malformed, unknown, badVerifier)).allMatch(r -> r.reason().countsAsGuess());

		sessionService.end(created.session(), UserSessionEndReason.LOGOUT);
		Rejected ended = rejected(authenticate(created));
		assertThat(ended.reason()).isEqualTo(Reason.ENDED);
		assertThat(ended.reason().countsAsGuess()).isFalse();
	}

	@Test
	void authenticate_anonymisedUser_isRejectedAndSessionRevoked() {
		User user = createUser("svc-anon");
		Created created = sessionService.create(user, client(CHROME_MAC_130));
		user.setAnonymizedAt(Instant.now());
		userRepository.save(user);

		assertThat(rejected(authenticate(created)).reason()).isEqualTo(Reason.USER_GONE);
		assertThat(sessionRepository.findById(created.session().getId()).orElseThrow().getEndReason())
				.isEqualTo(UserSessionEndReason.REVOKED);
	}

	@Test
	void authenticate_userWithPendingDeletion_isRejected() {
		User user = createUser("svc-deletion");
		Created created = sessionService.create(user, client(CHROME_MAC_130));
		user.setDeletionRequestedAt(Instant.now());
		userRepository.save(user);

		assertThat(rejected(authenticate(created)).reason()).isEqualTo(Reason.USER_GONE);
	}

	@Test
	void authenticate_browserFamilyChange_endsSession() {
		User user = createUser("svc-ua-family");
		Created created = sessionService.create(user, client(CHROME_MAC_130));

		AuthResult result = sessionService.authenticate(created.cookieValue(), client(FIREFOX_MAC));

		assertThat(rejected(result).reason()).isEqualTo(Reason.USER_AGENT_MISMATCH);
		assertThat(sessionRepository.findById(created.session().getId()).orElseThrow().getEndReason())
				.isEqualTo(UserSessionEndReason.USER_AGENT_MISMATCH);
	}

	@Test
	void authenticate_browserVersionChange_staysValid() {
		User user = createUser("svc-ua-version");
		Created created = sessionService.create(user, client(CHROME_MAC_130));

		AuthResult result = sessionService.authenticate(created.cookieValue(), client(CHROME_MAC_131));

		authenticated(result);
	}

	@Test
	void create_eleventhSession_evictsTheOldest() {
		User user = createUser("svc-cap");
		Created oldest = null;
		for (int i = 0; i < 11; i++) {
			at(Duration.ofMinutes(i));
			Created created = sessionService.create(user, client(CHROME_MAC_130));
			if (i == 0) {
				oldest = created;
			}
		}

		assertThat(sessionService.listActive(user.getId())).hasSize(10);
		assertThat(sessionRepository.findById(oldest.session().getId()).orElseThrow().getEndReason())
				.isEqualTo(UserSessionEndReason.EVICTED);
		assertThat(rejected(authenticate(oldest)).reason()).isEqualTo(Reason.ENDED);
	}

	@Test
	void rotation_createsNewSessionKeepingExpiryAndCsrfToken() {
		User user = createUser("svc-rotate");
		Created created = sessionService.create(user, client(CHROME_MAC_130));
		sessionService.markForRotation(user.getId());

		at(Duration.ofMinutes(1));
		Authenticated result = authenticated(authenticate(created));

		assertThat(result.newCookieValue()).isPresent();
		assertThat(result.newCookieValue().get()).isNotEqualTo(created.cookieValue());
		assertThat(result.session().getExpiresAt()).isEqualTo(created.session().getExpiresAt());
		assertThat(result.session().getCreatedAt()).isEqualTo(created.session().getCreatedAt());
		assertThat(result.session().getCsrfToken()).isEqualTo(created.session().getCsrfToken());
		assertThat(result.session().isRotationRequired()).isFalse();

		Authenticated viaNewCookie = authenticated(sessionService.authenticate(result.newCookieValue().get(), client(CHROME_MAC_130)));
		assertThat(viaNewCookie.newCookieValue()).isEmpty();
	}

	@Test
	void rotation_oldCookieWorksInsideGraceWithoutRotatingAgain_thenIsRejected() {
		User user = createUser("svc-grace");
		Created created = sessionService.create(user, client(CHROME_MAC_130));
		sessionService.markForRotation(user.getId());
		authenticated(authenticate(created));

		at(Duration.ofSeconds(29));
		Authenticated parallel = authenticated(authenticate(created));
		assertThat(parallel.newCookieValue()).isEmpty();
		assertThat(sessionService.listActive(user.getId())).hasSize(1);

		at(Duration.ofSeconds(31));
		assertThat(rejected(authenticate(created)).reason()).isEqualTo(Reason.SUPERSEDED_EXPIRED);
	}

	@Test
	void end_onAPreRotationSession_alsoEndsItsReplacement() {
		User user = createUser("svc-end-superseded");
		Created created = sessionService.create(user, client(CHROME_MAC_130));
		sessionService.markForRotation(user.getId());
		Authenticated rotated = authenticated(authenticate(created));
		String replacementCookie = rotated.newCookieValue().orElseThrow();

		// a logout that still carries the cookie value from before the rotation (inside the grace window)
		at(Duration.ofSeconds(5));
		Authenticated viaOldCookie = authenticated(authenticate(created));
		sessionService.end(viaOldCookie.session(), UserSessionEndReason.LOGOUT);

		assertThat(rejected(sessionService.authenticate(replacementCookie, client(CHROME_MAC_130))).reason())
				.isEqualTo(Reason.ENDED);
		assertThat(sessionService.listActive(user.getId())).isEmpty();
	}

	@Test
	void preRotationCookie_isRejectedOnceTheReplacementWasEnded() {
		User user = createUser("svc-reverse-grace");
		Created created = sessionService.create(user, client(CHROME_MAC_130));
		sessionService.markForRotation(user.getId());
		Authenticated rotated = authenticated(authenticate(created));

		// the user logs out with the replacement; a copy of the pre-rotation value must not outlive that
		at(Duration.ofSeconds(5));
		sessionService.end(rotated.session(), UserSessionEndReason.LOGOUT);

		at(Duration.ofSeconds(10));
		assertThat(rejected(authenticate(created)).reason()).isEqualTo(Reason.ENDED);
	}

	@Test
	void preRotationCookie_stillWorksInsideGraceAfterTheSessionRotatedTwice() {
		User user = createUser("svc-double-rotation");
		Created created = sessionService.create(user, client(CHROME_MAC_130));
		sessionService.markForRotation(user.getId());
		Authenticated first = authenticated(authenticate(created));

		at(Duration.ofSeconds(5));
		sessionService.markForRotation(user.getId());
		authenticated(sessionService.authenticate(first.newCookieValue().orElseThrow(), client(CHROME_MAC_130)));

		at(Duration.ofSeconds(10));
		authenticated(authenticate(created));
	}

	@Test
	void replaceExisting_endsTheCallersCurrentSessionOnlyForTheSameUser() {
		User user = createUser("svc-replace");
		User other = createUser("svc-replace-other");
		Created current = sessionService.create(user, client(CHROME_MAC_130));
		Created foreign = sessionService.create(other, client(CHROME_MAC_130));

		sessionService.replaceExisting(current.cookieValue(), user);
		sessionService.replaceExisting(foreign.cookieValue(), user);
		sessionService.replaceExisting("garbage", user);
		sessionService.replaceExisting(null, user);

		assertThat(rejected(authenticate(current)).reason()).isEqualTo(Reason.ENDED);
		authenticated(authenticate(foreign));
	}

	@Test
	void cleanup_endsOverdueSessionsAndDeletesOldEndedOnes() {
		User user = createUser("svc-cleanup");
		Created overdue = sessionService.create(user, client(CHROME_MAC_130));
		Created loggedOut = sessionService.create(user, client(CHROME_MAC_130));
		sessionService.end(loggedOut.session(), UserSessionEndReason.LOGOUT);

		at(Duration.ofDays(31));
		Created fresh = sessionService.create(user, client(CHROME_MAC_130));

		int deleted = sessionService.cleanup();

		assertThat(deleted).isEqualTo(1);
		assertThat(sessionRepository.findById(loggedOut.session().getId())).isEmpty();
		UserSession overdueRow = sessionRepository.findById(overdue.session().getId()).orElseThrow();
		assertThat(overdueRow.getEndReason()).isEqualTo(UserSessionEndReason.EXPIRED);
		assertThat(sessionRepository.findById(fresh.session().getId()).orElseThrow().getEndedAt()).isNull();
	}

	@Test
	void listActive_excludesExpiredSessions() {
		User user = createUser("svc-list");
		sessionService.create(user, client(CHROME_MAC_130));

		at(Duration.ofHours(5));
		Created recent = sessionService.create(user, client(CHROME_MAC_130));

		assertThat(sessionService.listActive(user.getId())).extracting(UserSession::getId)
				.containsExactly(recent.session().getId());
	}

	@Test
	void endOwn_onlyEndsTheCallersSession() {
		User owner = createUser("svc-own");
		User other = createUser("svc-other");
		Created ownSession = sessionService.create(owner, client(CHROME_MAC_130));
		Created otherSession = sessionService.create(other, client(CHROME_MAC_130));

		assertThat(sessionService.endOwn(owner.getId(), otherSession.session().getId())).isFalse();
		assertThat(sessionService.endOwn(owner.getId(), UUID.randomUUID())).isFalse();
		assertThat(sessionService.endOwn(owner.getId(), ownSession.session().getId())).isTrue();

		authenticated(authenticate(otherSession));
		assertThat(rejected(authenticate(ownSession)).reason()).isEqualTo(Reason.ENDED);
	}

	@Test
	void endOthers_keepsTheCurrentSession() {
		User user = createUser("svc-others");
		Created current = sessionService.create(user, client(CHROME_MAC_130));
		sessionService.create(user, client(CHROME_MAC_130));
		sessionService.create(user, client(CHROME_MAC_130));

		int ended = sessionService.endOthers(user.getId(), current.session().getId());

		assertThat(ended).isEqualTo(2);
		assertThat(sessionService.listActive(user.getId())).extracting(UserSession::getId)
				.containsExactly(current.session().getId());
	}

	@Test
	void endOthers_withAPreRotationSession_keepsTheCallersLiveReplacement() {
		User user = createUser("svc-others-grace");
		Created current = sessionService.create(user, client(CHROME_MAC_130));
		Created elsewhere = sessionService.create(user, client(CHROME_MAC_130));
		sessionService.markForRotation(user.getId());
		Authenticated rotated = authenticated(authenticate(current));

		// a parallel request that still carries the pre-rotation cookie asks to end all other sessions
		at(Duration.ofSeconds(5));
		Authenticated viaOldCookie = authenticated(authenticate(current));
		int ended = sessionService.endOthers(user.getId(), viaOldCookie.session().getId());

		assertThat(ended).isEqualTo(1);
		authenticated(sessionService.authenticate(rotated.newCookieValue().orElseThrow(), client(CHROME_MAC_130)));
		assertThat(rejected(authenticate(elsewhere)).reason()).isEqualTo(Reason.ENDED);
	}

	@Test
	void endOthers_whileTheCallersReplacementIsStillBeingCreated_doesNotEndIt() {
		User user = createUser("svc-others-inflight");
		Created current = sessionService.create(user, client(CHROME_MAC_130));
		Created elsewhere = sessionService.create(user, client(CHROME_MAC_130));

		// rotation has inserted the replacement but has not yet marked the old row as rotated (supersededBy is null)
		UserSession replacement = new UserSession();
		replacement.setTokenIdentifier("in-flight-identifier");
		replacement.setVerifierHash(new byte[] {1});
		replacement.setUserId(user.getId());
		replacement.setCreatedAt(START);
		replacement.setLastUsedAt(START);
		replacement.setExpiresAt(START.plus(Duration.ofHours(24)));
		replacement.setCsrfToken("csrf");
		replacement.setRotatedFrom(current.session().getId());
		replacement = sessionRepository.save(replacement);

		int ended = sessionService.endOthers(user.getId(), current.session().getId());

		assertThat(ended).isEqualTo(1);
		assertThat(sessionRepository.findById(replacement.getId()).orElseThrow().getEndedAt()).isNull();
		assertThat(rejected(authenticate(elsewhere)).reason()).isEqualTo(Reason.ENDED);
	}

	@Test
	void revokeAll_endsEveryActiveSessionOfTheUser() {
		User user = createUser("svc-revoke");
		Created first = sessionService.create(user, client(CHROME_MAC_130));
		Created second = sessionService.create(user, client(CHROME_MAC_130));

		sessionService.revokeAll(user.getId());

		assertThat(rejected(authenticate(first)).reason()).isEqualTo(Reason.ENDED);
		assertThat(rejected(authenticate(second)).reason()).isEqualTo(Reason.ENDED);
		assertThat(sessionRepository.findById(first.session().getId()).orElseThrow().getEndReason())
				.isEqualTo(UserSessionEndReason.REVOKED);
	}
}
