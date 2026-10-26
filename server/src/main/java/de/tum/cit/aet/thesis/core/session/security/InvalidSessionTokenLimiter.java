package de.tum.cit.aet.thesis.core.session.security;

import de.tum.cit.aet.thesis.core.session.config.UserSessionProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects guessing of session cookies. Every cookie that is malformed, unknown or carries a wrong verifier is
 * recorded per source IP; once an IP exceeds the configured number within a minute, further requests from it
 * that carry an invalid cookie are throttled. Requests with a valid session or without a cookie are never
 * affected, so users behind a shared campus address are not locked out by one misbehaving client.
 */
@Component
public class InvalidSessionTokenLimiter {
	private static final Duration WINDOW = Duration.ofMinutes(1);

	private final Map<String, Deque<Instant>> guesses = new ConcurrentHashMap<>();
	private final UserSessionProperties properties;
	private final Clock clock;

	/**
	 * Creates the limiter.
	 *
	 * @param properties the session limits
	 * @param clock the clock used for the sliding window
	 */
	public InvalidSessionTokenLimiter(UserSessionProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Records one invalid cookie from an address.
	 *
	 * @param ip the source address
	 */
	public void recordGuess(String ip) {
		Instant now = clock.instant();
		Deque<Instant> window = guesses.computeIfAbsent(ip, key -> new ArrayDeque<>());

		synchronized (window) {
			dropOld(window, now);
			window.addLast(now);
		}
	}

	/**
	 * Tells whether an address has sent too many invalid cookies recently.
	 *
	 * @param ip the source address
	 * @return whether requests with an invalid cookie from this address should be rejected
	 */
	public boolean isThrottled(String ip) {
		Deque<Instant> window = guesses.get(ip);

		if (window == null) {
			return false;
		}

		synchronized (window) {
			dropOld(window, clock.instant());

			return window.size() > properties.invalidTokenLimitPerMinute();
		}
	}

	/** Forgets addresses without recent invalid cookies, so the map does not grow without bound. */
	public void prune() {
		Instant now = clock.instant();

		guesses.entrySet().removeIf(entry -> {
			synchronized (entry.getValue()) {
				dropOld(entry.getValue(), now);

				return entry.getValue().isEmpty();
			}
		});
	}

	private void dropOld(Deque<Instant> window, Instant now) {
		Instant cutoff = now.minus(WINDOW);

		while (!window.isEmpty() && !window.peekFirst().isAfter(cutoff)) {
			window.removeFirst();
		}
	}
}
