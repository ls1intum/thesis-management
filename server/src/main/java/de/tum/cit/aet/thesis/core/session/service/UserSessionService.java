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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
	private static final int MAX_ROTATION_HOPS = 5;

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
		/**
		 * Reads the client properties from a request. The IP is the remote address, which reflects the real
		 * client when the server trusts the forwarded headers of its proxy.
		 *
		 * @param request the request
		 * @return the client properties
		 */
		public static ClientInfo from(HttpServletRequest request) {
			return new ClientInfo(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
		}
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
			return authenticateEnded(session, client, now);
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
			Optional<Created> rotated = rotateOrRestoreFlag(session, client, now);

			if (rotated.isEmpty()) {
				return new Rejected(Reason.ENDED);
			}

			return new Authenticated(rotated.get().session(), user.get(), Optional.of(rotated.get().cookieValue()));
		}

		touch(session, client, now);

		return new Authenticated(session, user.get(), Optional.empty());
	}

	/**
	 * Ends a session. Does nothing if it already ended. If the session was rotated, its replacement(s) end too:
	 * a logout that still carries the cookie value from before a rotation must not leave the session alive. The
	 * replacements are looked up in the database, not on the given entity, which may have been loaded before a
	 * concurrent rotation finished.
	 *
	 * @param session the session to end
	 * @param reason why it ends
	 */
	public void end(UserSession session, UserSessionEndReason reason) {
		Instant now = clock.instant();

		if (repository.endById(session.getId(), now, reason) > 0) {
			log.info("Session {} of user {} ended: {}", UserSessionTokens.logId(session.getTokenIdentifier()), session.getUserId(), reason);
		}

		endReplacements(session.getId(), session.getUserId(), reason, now);
	}

	private void endReplacements(UUID sessionId, UUID userId, UserSessionEndReason reason, Instant now) {
		// Settings can hold an old row id through multiple rotations. Revocation must reach every replacement,
		// even beyond the bounded chain accepted during authentication's short cookie grace window.
		Deque<UUID> pending = new ArrayDeque<>();
		Set<UUID> visited = new LinkedHashSet<>();
		pending.add(sessionId);

		while (!pending.isEmpty()) {
			UUID predecessorId = pending.removeFirst();
			if (!visited.add(predecessorId)) {
				continue;
			}

			for (UUID replacementId : repository.findIdsByRotatedFrom(predecessorId)) {
				if (repository.endById(replacementId, now, reason) > 0) {
					log.info("Session {} of user {} ended with its predecessor: {}", replacementId, userId, reason);
				}
				pending.addLast(replacementId);
			}
		}
	}

	/**
	 * Ends the session that a browser presents while it logs in again, so that logging in twice does not leave an
	 * orphaned session behind. Cookie values that are malformed, unknown, or belong to another user are ignored.
	 *
	 * @param cookieValue the raw cookie value of the request, may be null
	 * @param user the user who just authenticated with Keycloak
	 */
	public void replaceExisting(String cookieValue, User user) {
		UserSessionTokens.parse(cookieValue)
				.flatMap(parsed -> repository.findByTokenIdentifier(parsed.identifier())
						.filter(session -> UserSessionTokens.matches(session.getVerifierHash(), parsed.verifier())))
				.filter(session -> session.getUserId().equals(user.getId()))
				.ifPresent(session -> end(session, UserSessionEndReason.LOGOUT));
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
	 * Ends one of the caller's own sessions, following replacements if it rotated after the list was loaded.
	 *
	 * @param userId the caller
	 * @param sessionId the session row id
	 * @return whether a session of that user was ended
	 */
	public boolean endOwn(UUID userId, UUID sessionId) {
		Optional<UserSession> session = repository.findByIdAndUserId(sessionId, userId)
				.filter(candidate -> candidate.getEndedAt() == null || candidate.getEndReason() == UserSessionEndReason.ROTATED);

		session.ifPresent(candidate -> end(candidate, UserSessionEndReason.LOGOUT));

		return session.isPresent();
	}

	/**
	 * Returns the ids of a session and of the sessions it was rotated into. A request that still carries the cookie
	 * value from before a rotation (inside the grace window) is authenticated as the old row, while the browser
	 * already uses the replacement; both belong to the same login.
	 *
	 * @param sessionId the session that authenticated the request
	 * @return the session and its replacements, found in the database
	 */
	public Set<UUID> currentAndSuccessors(UUID sessionId) {
		Set<UUID> ids = new LinkedHashSet<>();
		collectReplacements(sessionId, ids, 0);

		return ids;
	}

	private void collectReplacements(UUID sessionId, Set<UUID> ids, int depth) {
		ids.add(sessionId);

		if (depth >= MAX_ROTATION_HOPS) {
			return;
		}

		repository.findIdsByRotatedFrom(sessionId).forEach(replacementId -> collectReplacements(replacementId, ids, depth + 1));
	}

	/**
	 * Ends all of the caller's sessions except the one making the request and its replacements. One statement
	 * ends them, and a replacement that a concurrent rotation inserts is spared because it records its
	 * predecessor, so this device is never logged out by its own rotation.
	 *
	 * @param userId the caller
	 * @param currentSessionId the session to keep
	 * @return the number of ended sessions
	 */
	public int endOthers(UUID userId, UUID currentSessionId) {
		int ended = repository.endAllExcept(userId, currentAndSuccessors(currentSessionId), clock.instant(), UserSessionEndReason.LOGOUT);

		if (ended > 0) {
			log.info("User {} ended {} other sessions", userId, ended);
		}

		return ended;
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

	private AuthResult authenticateEnded(UserSession session, ClientInfo client, Instant now) {
		if (session.getEndReason() != UserSessionEndReason.ROTATED || session.getSupersededUntil() == null) {
			return new Rejected(Reason.ENDED);
		}

		// The grace window must not outlive the absolute timeout of the session it was rotated from.
		if (!now.isBefore(session.getSupersededUntil()) || !now.isBefore(session.getExpiresAt())) {
			return new Rejected(Reason.SUPERSEDED_EXPIRED);
		}

		// The same browser binding as for a live session: a pre-rotation cookie copied to another browser must not
		// work during the grace window. Nothing is ended here, the live replacement belongs to the real browser.
		if (!userAgentMatches(session, client)) {
			return new Rejected(Reason.USER_AGENT_MISMATCH);
		}

		// A parallel request that still carries the cookie value from before a rotation. It is only accepted
		// while the session it was rotated into is alive: after a logout the old value must not live on.
		if (!replacementIsAlive(session.getSupersededBy())) {
			return new Rejected(Reason.ENDED);
		}

		return loadUsableUser(session)
				.<AuthResult>map(user -> new Authenticated(session, user, Optional.empty()))
				.orElseGet(() -> new Rejected(Reason.USER_GONE));
	}

	/** Follows a rotation chain (a session may rotate again within the grace window) to its newest session. */
	private boolean replacementIsAlive(UUID replacementId) {
		UUID next = replacementId;

		for (int hops = 0; next != null && hops < MAX_ROTATION_HOPS; hops++) {
			Optional<UserSession> replacement = repository.findById(next);

			if (replacement.isEmpty()) {
				return false;
			}

			if (replacement.get().getEndedAt() == null) {
				return true;
			}

			next = replacement.get().getEndReason() == UserSessionEndReason.ROTATED ? replacement.get().getSupersededBy() : null;
		}

		return false;
	}

	private Optional<User> loadUsableUser(UserSession session) {
		return userRepository.findById(session.getUserId())
				.filter(user -> !user.isAnonymized() && user.getDeletionRequestedAt() == null);
	}

	private boolean userAgentMatches(UserSession session, ClientInfo client) {
		String stored = session.getUserAgentSignature();
		String current = UserAgentSignature.of(client.userAgent());

		if (stored == null || UserAgentSignature.sameDevice(stored, current)) {
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

	/**
	 * Rotates a session; if that fails (for example a database error) the rotation flag, which {@code claimRotation}
	 * already cleared, is set again so that the next request retries instead of the role change being forgotten.
	 */
	private Optional<Created> rotateOrRestoreFlag(UserSession old, ClientInfo client, Instant now) {
		try {
			return rotate(old, client, now);
		} catch (RuntimeException exception) {
			repository.markRotationRequiredForUser(old.getUserId());
			throw exception;
		}
	}

	/**
	 * Replaces a session. Creating the replacement and marking the old session as rotated are separate database
	 * operations, so a logout can end the old session in between. In that case the replacement is ended again and
	 * nothing is handed out: a session that was ended must not be revived by a concurrent rotation.
	 */
	private Optional<Created> rotate(UserSession old, ClientInfo client, Instant now) {
		UserSessionTokens.Issued issued = UserSessionTokens.generate();

		UserSession replacement = new UserSession();
		replacement.setTokenIdentifier(issued.identifier());
		replacement.setVerifierHash(UserSessionTokens.hashVerifier(issued.verifier()));
		replacement.setUserId(old.getUserId());
		replacement.setCreatedAt(old.getCreatedAt());
		replacement.setExpiresAt(old.getExpiresAt());
		replacement.setCsrfToken(old.getCsrfToken());
		replacement.setRotatedFrom(old.getId());
		replacement.setLastUsedAt(now);
		replacement.setCreatedIp(old.getCreatedIp());
		applyClient(replacement, client, false);

		replacement = repository.save(replacement);

		if (repository.markRotated(old.getId(), now, replacement.getId(), now.plus(properties.rotationGrace())) == 0) {
			repository.endById(replacement.getId(), now, UserSessionEndReason.REVOKED);
			log.info("Session {} of user {} ended while it was being rotated; the replacement was dropped",
					UserSessionTokens.logId(old.getTokenIdentifier()), old.getUserId());

			return Optional.empty();
		}

		log.info("Session {} of user {} rotated to {}", UserSessionTokens.logId(old.getTokenIdentifier()), old.getUserId(),
				UserSessionTokens.logId(replacement.getTokenIdentifier()));

		return Optional.of(new Created(replacement, issued.cookieValue()));
	}

	private void evictOldestSessions(UUID userId) {
		Instant now = clock.instant();
		List<UserSession> live = new ArrayList<>();

		// A session past one of its timeouts is dead but may not have been swept yet: it must neither count against
		// the limit nor be kept while a live session is evicted instead.
		for (UserSession session : repository.findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(userId)) {
			if (isPastTimeout(session, now)) {
				end(session, UserSessionEndReason.EXPIRED);
			} else {
				live.add(session);
			}
		}

		int excess = live.size() - properties.maxPerUser();

		for (int i = 0; i < excess; i++) {
			end(live.get(i), UserSessionEndReason.EVICTED);
		}
	}

	private boolean isPastTimeout(UserSession session, Instant now) {
		return !now.isBefore(session.getExpiresAt()) || !now.isBefore(session.getLastUsedAt().plus(properties.idleTimeout()));
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
