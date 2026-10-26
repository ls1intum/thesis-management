package de.tum.cit.aet.thesis.core.session.service;

import de.tum.cit.aet.thesis.core.session.config.UserSessionProperties;
import de.tum.cit.aet.thesis.core.session.config.UserSessionProperties.UserAgentMismatchAction;
import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.entity.UserSessionEndReason;
import de.tum.cit.aet.thesis.core.session.repository.UserSessionRepository;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates, authenticates, rotates and ends Thesis Management sessions.
 *
 * <p>Keycloak is only involved when a session is created. From then on a session is valid until its idle or
 * absolute timeout passes, it is ended, or its user is deleted, independent of Keycloak's availability.
 * Roles are not stored in the session; they are read from {@code user_groups} on every request.</p>
 */
@Service
public class UserSessionService {
	private static final Logger log = LoggerFactory.getLogger(UserSessionService.class);

	private static final int CSRF_TOKEN_BYTES = 32;
	private static final int MAX_IP_LENGTH = 64;
	private static final int MAX_USER_AGENT_LENGTH = 255;

	private final UserSessionRepository repository;
	private final UserRepository userRepository;
	private final UserSessionProperties properties;
	private final Clock clock;

	/**
	 * Creates the service.
	 *
	 * @param repository the session repository
	 * @param userRepository the user repository
	 * @param properties the session limits
	 * @param clock the clock used for all time decisions
	 */
	public UserSessionService(
			UserSessionRepository repository,
			UserRepository userRepository,
			UserSessionProperties properties,
			Clock clock
	) {
		this.repository = repository;
		this.userRepository = userRepository;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Properties of the client making a request, used for activity history and anomaly detection.
	 *
	 * @param ip the source IP address
	 * @param userAgent the User-Agent header
	 */
	public record ClientInfo(String ip, String userAgent) {
	}

	/**
	 * A newly created or rotated session together with the cookie value that identifies it.
	 *
	 * @param session the stored session
	 * @param cookieValue the value for the session cookie
	 */
	public record Created(UserSession session, String cookieValue) {
	}

	/** Result of authenticating a cookie value. */
	public sealed interface AuthResult permits Authenticated, Rejected {
	}

	/**
	 * The cookie value belongs to a valid session.
	 *
	 * @param session the session (the replacement if it was rotated)
	 * @param user the session's user
	 * @param newCookieValue the new cookie value if the session was rotated and the client must replace its cookie
	 */
	public record Authenticated(UserSession session, User user, Optional<String> newCookieValue) implements AuthResult {
	}

	/**
	 * The cookie value does not belong to a valid session.
	 *
	 * @param reason why it was rejected
	 */
	public record Rejected(Reason reason) implements AuthResult {
	}

	/** Why a cookie value was rejected. */
	public enum Reason {
		MALFORMED,
		UNKNOWN,
		BAD_VERIFIER,
		ENDED,
		EXPIRED_ABSOLUTE,
		EXPIRED_IDLE,
		USER_GONE,
		USER_AGENT_MISMATCH,
		SUPERSEDED_EXPIRED;

		/**
		 * Whether this rejection looks like guessing rather than a stale cookie of a real session.
		 *
		 * @return true for malformed, unknown and bad-verifier values
		 */
		public boolean countsAsGuess() {
			return this == MALFORMED || this == UNKNOWN || this == BAD_VERIFIER;
		}
	}

	/**
	 * Creates a session for a user whose identity Keycloak has just verified. Ends the user's oldest
	 * sessions if the per-user limit is exceeded.
	 *
	 * @param user the authenticated user
	 * @param client the requesting client
	 * @return the session and its cookie value
	 */
	public Created create(User user, ClientInfo client) {
		Instant now = clock.instant();
		UserSessionTokens.Issued issued = UserSessionTokens.generate();

		UserSession session = new UserSession();
		session.setTokenIdentifier(issued.identifier());
		session.setVerifierHash(UserSessionTokens.hashVerifier(issued.verifier()));
		session.setUserId(user.getId());
		session.setCreatedAt(now);
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plus(properties.absoluteTimeout()));
		session.setCsrfToken(UserSessionTokens.randomToken(CSRF_TOKEN_BYTES));
		applyClient(session, client, true);

		session = repository.save(session);

		log.info("Session {} created for user {} from {}", UserSessionTokens.logId(session.getTokenIdentifier()), user.getId(), session.getCreatedIp());

		evictOldestSessions(user.getId());

		return new Created(session, issued.cookieValue());
	}

	/**
	 * Authenticates a cookie value. Valid sessions are touched (at most once per write interval) and
	 * rotated if their user's roles changed.
	 *
	 * @param cookieValue the raw cookie value
	 * @param client the requesting client
	 * @return the authenticated session or the reason for rejection
	 */
	public AuthResult authenticate(String cookieValue, ClientInfo client) {
		Optional<UserSessionTokens.Parsed> parsed = UserSessionTokens.parse(cookieValue);

		if (parsed.isEmpty()) {
			return new Rejected(Reason.MALFORMED);
		}

		Optional<UserSession> found = repository.findByTokenIdentifier(parsed.get().identifier());

		if (found.isEmpty()) {
			return new Rejected(Reason.UNKNOWN);
		}

		UserSession session = found.get();

		if (!UserSessionTokens.matches(session.getVerifierHash(), parsed.get().verifier())) {
			return new Rejected(Reason.BAD_VERIFIER);
		}

		Instant now = clock.instant();

		if (session.getEndedAt() != null) {
			return authenticateEnded(session, now);
		}

		if (!now.isBefore(session.getExpiresAt())) {
			end(session, UserSessionEndReason.EXPIRED);
			return new Rejected(Reason.EXPIRED_ABSOLUTE);
		}

		if (!now.isBefore(session.getLastUsedAt().plus(properties.idleTimeout()))) {
			end(session, UserSessionEndReason.EXPIRED);
			return new Rejected(Reason.EXPIRED_IDLE);
		}

		Optional<User> user = loadUsableUser(session);

		if (user.isEmpty()) {
			end(session, UserSessionEndReason.REVOKED);
			return new Rejected(Reason.USER_GONE);
		}

		if (!userAgentMatches(session, client)) {
			end(session, UserSessionEndReason.USER_AGENT_MISMATCH);
			return new Rejected(Reason.USER_AGENT_MISMATCH);
		}

		if (session.isRotationRequired() && repository.claimRotation(session.getId()) == 1) {
			Created rotated = rotate(session, client, now);
			return new Authenticated(rotated.session(), user.get(), Optional.of(rotated.cookieValue()));
		}

		touch(session, client, now);

		return new Authenticated(session, user.get(), Optional.empty());
	}

	/**
	 * Ends a session. Does nothing if it already ended.
	 *
	 * @param session the session to end
	 * @param reason why it ends
	 */
	public void end(UserSession session, UserSessionEndReason reason) {
		if (repository.endById(session.getId(), clock.instant(), reason) > 0) {
			log.info("Session {} of user {} ended: {}", UserSessionTokens.logId(session.getTokenIdentifier()), session.getUserId(), reason);
		}
	}

	/**
	 * Lists a user's sessions that can still authenticate, oldest first.
	 *
	 * @param userId the user
	 * @return the active sessions
	 */
	public List<UserSession> listActive(UUID userId) {
		Instant now = clock.instant();

		return repository.findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(userId).stream()
				.filter(session -> now.isBefore(session.getExpiresAt())
						&& now.isBefore(session.getLastUsedAt().plus(properties.idleTimeout())))
				.toList();
	}

	/**
	 * Ends one of the caller's own sessions.
	 *
	 * @param userId the caller
	 * @param sessionId the session row id
	 * @return whether a session of that user was ended
	 */
	public boolean endOwn(UUID userId, UUID sessionId) {
		Optional<UserSession> session = repository.findByIdAndUserId(sessionId, userId)
				.filter(candidate -> candidate.getEndedAt() == null);

		session.ifPresent(candidate -> end(candidate, UserSessionEndReason.LOGOUT));

		return session.isPresent();
	}

	/**
	 * Ends all of the caller's sessions except the one making the request.
	 *
	 * @param userId the caller
	 * @param currentSessionId the session to keep
	 * @return the number of ended sessions
	 */
	public int endOthers(UUID userId, UUID currentSessionId) {
		List<UserSession> others = repository.findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(userId).stream()
				.filter(session -> !session.getId().equals(currentSessionId))
				.toList();

		others.forEach(session -> end(session, UserSessionEndReason.LOGOUT));

		return others.size();
	}

	/**
	 * Ends every active session of a user, for example when the account is deleted.
	 *
	 * @param userId the user
	 */
	public void revokeAll(UUID userId) {
		int ended = repository.endAllForUser(userId, clock.instant(), UserSessionEndReason.REVOKED);

		if (ended > 0) {
			log.info("Revoked {} sessions of user {}", ended, userId);
		}
	}

	/**
	 * Flags all of a user's sessions for rotation after a role change; each is replaced on its next request.
	 *
	 * @param userId the user whose roles changed
	 */
	public void markForRotation(UUID userId) {
		repository.markRotationRequiredForUser(userId);
	}

	/**
	 * Housekeeping: ends sessions past their timeouts and deletes sessions that ended longer ago than the
	 * retention period. Timeouts are also enforced on every request, so this is not a security control.
	 *
	 * @return the number of deleted session rows
	 */
	public int cleanup() {
		Instant now = clock.instant();

		repository.findByEndedAtIsNullAndExpiresAtBefore(now)
				.forEach(session -> end(session, UserSessionEndReason.EXPIRED));
		repository.findByEndedAtIsNullAndLastUsedAtBefore(now.minus(properties.idleTimeout()))
				.forEach(session -> end(session, UserSessionEndReason.EXPIRED));

		return repository.deleteEndedBefore(now.minus(properties.endedRetention()));
	}

	private AuthResult authenticateEnded(UserSession session, Instant now) {
		if (session.getEndReason() != UserSessionEndReason.ROTATED || session.getSupersededUntil() == null) {
			return new Rejected(Reason.ENDED);
		}

		if (!now.isBefore(session.getSupersededUntil())) {
			return new Rejected(Reason.SUPERSEDED_EXPIRED);
		}

		// A parallel request that still carries the cookie value from before a rotation.
		return loadUsableUser(session)
				.<AuthResult>map(user -> new Authenticated(session, user, Optional.empty()))
				.orElseGet(() -> new Rejected(Reason.USER_GONE));
	}

	private Optional<User> loadUsableUser(UserSession session) {
		return userRepository.findById(session.getUserId())
				.filter(user -> !user.isAnonymized() && user.getDeletionRequestedAt() == null);
	}

	private boolean userAgentMatches(UserSession session, ClientInfo client) {
		String stored = session.getUserAgentSignature();
		String current = UserAgentSignature.of(client.userAgent());

		if (stored == null || stored.equals(current)) {
			return true;
		}

		log.warn("Session {} of user {} presented by a different browser or system: {} instead of {}",
				UserSessionTokens.logId(session.getTokenIdentifier()), session.getUserId(), current, stored);

		return properties.userAgentMismatchAction() == UserAgentMismatchAction.LOG;
	}

	private void touch(UserSession session, ClientInfo client, Instant now) {
		if (now.isBefore(session.getLastUsedAt().plus(properties.lastUsedWriteInterval()))) {
			return;
		}

		String ip = limit(client.ip(), MAX_IP_LENGTH);

		if (!Objects.equals(ip, session.getLastIp())) {
			log.info("Session {} of user {} is now used from {} (was {})",
					UserSessionTokens.logId(session.getTokenIdentifier()), session.getUserId(), ip, session.getLastIp());
		}

		repository.touchLastUsed(session.getId(), now, ip);
		session.setLastUsedAt(now);
		session.setLastIp(ip);
	}

	private Created rotate(UserSession old, ClientInfo client, Instant now) {
		UserSessionTokens.Issued issued = UserSessionTokens.generate();

		UserSession replacement = new UserSession();
		replacement.setTokenIdentifier(issued.identifier());
		replacement.setVerifierHash(UserSessionTokens.hashVerifier(issued.verifier()));
		replacement.setUserId(old.getUserId());
		replacement.setCreatedAt(old.getCreatedAt());
		replacement.setExpiresAt(old.getExpiresAt());
		replacement.setCsrfToken(old.getCsrfToken());
		replacement.setLastUsedAt(now);
		replacement.setCreatedIp(old.getCreatedIp());
		applyClient(replacement, client, false);

		replacement = repository.save(replacement);
		repository.markRotated(old.getId(), now, replacement.getId(), now.plus(properties.rotationGrace()));

		log.info("Session {} of user {} rotated to {}", UserSessionTokens.logId(old.getTokenIdentifier()), old.getUserId(),
				UserSessionTokens.logId(replacement.getTokenIdentifier()));

		return new Created(replacement, issued.cookieValue());
	}

	private void evictOldestSessions(UUID userId) {
		List<UserSession> active = repository.findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(userId);
		int excess = active.size() - properties.maxPerUser();

		for (int i = 0; i < excess; i++) {
			end(active.get(i), UserSessionEndReason.EVICTED);
		}
	}

	private void applyClient(UserSession session, ClientInfo client, boolean initial) {
		String ip = limit(client.ip(), MAX_IP_LENGTH);

		if (initial) {
			session.setCreatedIp(ip);
		}

		session.setLastIp(ip);
		session.setUserAgent(limit(client.userAgent(), MAX_USER_AGENT_LENGTH));
		session.setUserAgentSignature(UserAgentSignature.of(client.userAgent()));
	}

	private static String limit(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}

		return value.substring(0, maxLength);
	}
}
