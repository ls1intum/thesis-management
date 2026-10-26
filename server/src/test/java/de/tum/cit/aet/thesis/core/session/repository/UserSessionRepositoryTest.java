package de.tum.cit.aet.thesis.core.session.repository;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.entity.UserSessionEndReason;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Testcontainers
class UserSessionRepositoryTest extends BaseIntegrationTest {
	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private UserSessionRepository userSessionRepository;

	@Autowired
	private UserRepository userRepository;

	private User createUser(String universityId) {
		User user = new User();
		user.setUniversityId(universityId);
		user.setJoinedAt(Instant.now());
		user.setUpdatedAt(Instant.now());
		return userRepository.save(user);
	}

	private UserSession newSession(UUID userId, String identifier) {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		UserSession session = new UserSession();
		session.setTokenIdentifier(identifier);
		session.setVerifierHash(new byte[] {1, 2, 3, 4});
		session.setUserId(userId);
		session.setCreatedAt(now);
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(24, ChronoUnit.HOURS));
		session.setCsrfToken("csrf-" + identifier);
		session.setCreatedIp("203.0.113.7");
		session.setLastIp("203.0.113.7");
		session.setUserAgent("agent");
		session.setUserAgentSignature("chrome|macos");
		return userSessionRepository.save(session);
	}

	@Test
	void findByTokenIdentifier_returnsRowAndHashRoundTrips() {
		User user = createUser("session-user-1");
		newSession(user.getId(), "identifier-1");

		UserSession loaded = userSessionRepository.findByTokenIdentifier("identifier-1").orElseThrow();

		assertThat(loaded.getVerifierHash()).containsExactly(1, 2, 3, 4);
		assertThat(loaded.getUserId()).isEqualTo(user.getId());
		assertThat(loaded.getCsrfToken()).isEqualTo("csrf-identifier-1");
		assertThat(loaded.isRotationRequired()).isFalse();
		assertThat(userSessionRepository.findByTokenIdentifier("missing")).isEmpty();
	}

	@Test
	void markRotationRequiredForUser_flagsOnlyThatUsersActiveSessions() {
		User first = createUser("session-user-2");
		User second = createUser("session-user-3");
		UserSession active = newSession(first.getId(), "identifier-2a");
		UserSession ended = newSession(first.getId(), "identifier-2b");
		ended.setEndedAt(Instant.now());
		ended.setEndReason(UserSessionEndReason.LOGOUT);
		userSessionRepository.save(ended);
		UserSession other = newSession(second.getId(), "identifier-3a");

		int updated = userSessionRepository.markRotationRequiredForUser(first.getId());

		assertThat(updated).isEqualTo(1);
		assertThat(userSessionRepository.findById(active.getId()).orElseThrow().isRotationRequired()).isTrue();
		assertThat(userSessionRepository.findById(ended.getId()).orElseThrow().isRotationRequired()).isFalse();
		assertThat(userSessionRepository.findById(other.getId()).orElseThrow().isRotationRequired()).isFalse();
	}

	@Test
	void findActiveByUser_returnsOldestFirstAndSkipsEnded() {
		User user = createUser("session-user-4");
		UserSession older = newSession(user.getId(), "identifier-4a");
		older.setCreatedAt(older.getCreatedAt().minus(1, ChronoUnit.HOURS));
		userSessionRepository.save(older);
		UserSession newer = newSession(user.getId(), "identifier-4b");
		UserSession ended = newSession(user.getId(), "identifier-4c");
		ended.setEndedAt(Instant.now());
		ended.setEndReason(UserSessionEndReason.EVICTED);
		userSessionRepository.save(ended);

		List<UserSession> active = userSessionRepository.findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(user.getId());

		assertThat(active).extracting(UserSession::getId).containsExactly(older.getId(), newer.getId());
	}

	@Test
	void deleteEndedBefore_removesOnlyOldEndedRows() {
		User user = createUser("session-user-5");
		Instant now = Instant.now();
		UserSession oldEnded = newSession(user.getId(), "identifier-5a");
		oldEnded.setEndedAt(now.minus(40, ChronoUnit.DAYS));
		oldEnded.setEndReason(UserSessionEndReason.EXPIRED);
		userSessionRepository.save(oldEnded);
		UserSession recentEnded = newSession(user.getId(), "identifier-5b");
		recentEnded.setEndedAt(now.minus(2, ChronoUnit.DAYS));
		recentEnded.setEndReason(UserSessionEndReason.LOGOUT);
		userSessionRepository.save(recentEnded);
		UserSession active = newSession(user.getId(), "identifier-5c");

		int deleted = userSessionRepository.deleteEndedBefore(now.minus(30, ChronoUnit.DAYS));

		assertThat(deleted).isEqualTo(1);
		assertThat(userSessionRepository.findById(oldEnded.getId())).isEmpty();
		assertThat(userSessionRepository.findById(recentEnded.getId())).isPresent();
		assertThat(userSessionRepository.findById(active.getId())).isPresent();
	}

	@Test
	void claimRotation_succeedsOnlyOnceAndOnlyForFlaggedSessions() {
		User user = createUser("session-user-7");
		UserSession flagged = newSession(user.getId(), "identifier-7a");
		UserSession plain = newSession(user.getId(), "identifier-7b");
		userSessionRepository.markRotationRequiredForUser(user.getId());
		plain.setRotationRequired(false);
		userSessionRepository.save(plain);

		assertThat(userSessionRepository.claimRotation(flagged.getId())).isEqualTo(1);
		assertThat(userSessionRepository.claimRotation(flagged.getId())).isZero();
		assertThat(userSessionRepository.claimRotation(plain.getId())).isZero();
		assertThat(userSessionRepository.findById(flagged.getId()).orElseThrow().isRotationRequired()).isFalse();
	}

	@Test
	void findIdleCandidates_returnsActiveSessionsNotUsedSince() {
		User user = createUser("session-user-8");
		Instant now = Instant.now();
		UserSession stale = newSession(user.getId(), "identifier-8a");
		stale.setLastUsedAt(now.minus(5, ChronoUnit.HOURS));
		userSessionRepository.save(stale);
		UserSession fresh = newSession(user.getId(), "identifier-8b");
		UserSession endedStale = newSession(user.getId(), "identifier-8c");
		endedStale.setLastUsedAt(now.minus(6, ChronoUnit.HOURS));
		endedStale.setEndedAt(now);
		endedStale.setEndReason(UserSessionEndReason.LOGOUT);
		userSessionRepository.save(endedStale);

		List<UserSession> candidates = userSessionRepository.findByEndedAtIsNullAndLastUsedAtBefore(now.minus(4, ChronoUnit.HOURS));

		assertThat(candidates).extracting(UserSession::getId).containsExactly(stale.getId());
		assertThat(candidates).doesNotContain(fresh);
	}

	@Test
	void findIdsByRotatedFrom_returnsTheReplacementsOfASession() {
		User user = createUser("session-user-9");
		UserSession old = newSession(user.getId(), "identifier-9a");
		UserSession replacement = newSession(user.getId(), "identifier-9b");
		replacement.setRotatedFrom(old.getId());
		userSessionRepository.save(replacement);
		newSession(user.getId(), "identifier-9c");

		assertThat(userSessionRepository.findIdsByRotatedFrom(old.getId())).containsExactly(replacement.getId());
		assertThat(userSessionRepository.findIdsByRotatedFrom(replacement.getId())).isEmpty();
	}

	@Test
	void endAllExcept_endsOtherActiveSessionsOfTheUserAndSparesTheKeptOnesAndTheirReplacements() {
		User user = createUser("session-user-10");
		User other = createUser("session-user-11");
		UserSession kept = newSession(user.getId(), "identifier-10a");
		UserSession replacementOfKept = newSession(user.getId(), "identifier-10b");
		replacementOfKept.setRotatedFrom(kept.getId());
		userSessionRepository.save(replacementOfKept);
		UserSession elsewhere = newSession(user.getId(), "identifier-10c");
		UserSession strangers = newSession(other.getId(), "identifier-11a");

		int ended = userSessionRepository.endAllExcept(user.getId(), List.of(kept.getId()), Instant.now(), UserSessionEndReason.LOGOUT);

		assertThat(ended).isEqualTo(1);
		assertThat(userSessionRepository.findById(kept.getId()).orElseThrow().getEndedAt()).isNull();
		assertThat(userSessionRepository.findById(replacementOfKept.getId()).orElseThrow().getEndedAt()).isNull();
		assertThat(userSessionRepository.findById(elsewhere.getId()).orElseThrow().getEndedAt()).isNotNull();
		assertThat(userSessionRepository.findById(strangers.getId()).orElseThrow().getEndedAt()).isNull();
	}

	@Test
	void deletingUser_cascadesToSessions() {
		User user = createUser("session-user-6");
		UserSession session = newSession(user.getId(), "identifier-6");

		userRepository.deleteById(user.getId());

		assertThat(userSessionRepository.findById(session.getId())).isEmpty();
	}
}
