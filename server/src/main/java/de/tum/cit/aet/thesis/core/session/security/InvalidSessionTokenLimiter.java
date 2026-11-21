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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Detects guessing of session cookies. Every cookie that is malformed, unknown or carries a wrong verifier is
 * recorded per source IP; once an IP exceeds the configured number within a minute, further requests from it
 * that carry an invalid cookie are throttled. Requests with a valid session or without a cookie are never
 * affected, so users behind a shared campus address are not locked out by one misbehaving client.
 */
@Component
public class InvalidSessionTokenLimiter {
	/** Upper bound for the number of tracked addresses, so made-up source addresses cannot grow the map without limit. */
	static final int MAX_TRACKED_ADDRESSES = 10_000;

	private static final Duration WINDOW = Duration.ofMinutes(1);

	/** A full map is scanned at most this often, however many new addresses keep arriving. */
	private static final Duration MIN_PRUNE_INTERVAL = Duration.ofSeconds(5);

	private final Map<String, Deque<Instant>> guesses = new ConcurrentHashMap<>();
	private final AtomicReference<Instant> lastPrune = new AtomicReference<>(Instant.EPOCH);
	private final AtomicInteger pruneRuns = new AtomicInteger();
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

		if (!guesses.containsKey(ip) && guesses.size() >= MAX_TRACKED_ADDRESSES) {
			Instant last = lastPrune.get();

			// only one request per interval pays for the scan; the others skip it and fail open
			if (!now.isBefore(last.plus(MIN_PRUNE_INTERVAL)) && lastPrune.compareAndSet(last, now)) {
				prune();
			}

			if (guesses.size() >= MAX_TRACKED_ADDRESSES) {
				return;
			}
		}

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

	/**
	 * Returns how often the map was scanned for stale addresses.
	 *
	 * @return the number of scans
	 */
	int pruneRuns() {
		return pruneRuns.get();
	}

	/**
	 * Returns how many addresses are currently tracked.
	 *
	 * @return the number of addresses with recorded invalid cookies
	 */
	int trackedAddresses() {
		return guesses.size();
	}

	/** Forgets addresses without recent invalid cookies, so the map does not grow without bound. */
	public void prune() {
		Instant now = clock.instant();
		pruneRuns.incrementAndGet();

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
