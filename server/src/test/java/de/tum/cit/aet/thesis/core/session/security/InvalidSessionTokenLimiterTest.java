package de.tum.cit.aet.thesis.core.session.security;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.core.session.config.UserSessionProperties;
import de.tum.cit.aet.thesis.core.session.config.UserSessionProperties.UserAgentMismatchAction;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

class InvalidSessionTokenLimiterTest {
	private final UserSessionProperties properties = new UserSessionProperties(Duration.ofHours(4), Duration.ofHours(24), 10,
			Duration.ofMinutes(5), Duration.ofSeconds(30), Duration.ofDays(30), 3, UserAgentMismatchAction.END);

	@Test
	void anAddressIsThrottledOnlyAfterExceedingTheLimitWithinAMinute() {
		InvalidSessionTokenLimiter limiter = new InvalidSessionTokenLimiter(properties, Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"), ZoneOffset.UTC));

		for (int i = 0; i < 3; i++) {
			limiter.recordGuess("198.51.100.1");
		}
		assertThat(limiter.isThrottled("198.51.100.1")).isFalse();

		limiter.recordGuess("198.51.100.1");
		assertThat(limiter.isThrottled("198.51.100.1")).isTrue();
		assertThat(limiter.isThrottled("198.51.100.2")).isFalse();
	}

	@Test
	void theNumberOfTrackedAddressesIsBoundedEvenWhenSourceAddressesAreMadeUp() {
		InvalidSessionTokenLimiter limiter = new InvalidSessionTokenLimiter(properties, Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"), ZoneOffset.UTC));

		for (int i = 0; i < InvalidSessionTokenLimiter.MAX_TRACKED_ADDRESSES + 500; i++) {
			limiter.recordGuess("10." + (i / 65536) + "." + ((i / 256) % 256) + "." + (i % 256));
		}

		assertThat(limiter.trackedAddresses()).isLessThanOrEqualTo(InvalidSessionTokenLimiter.MAX_TRACKED_ADDRESSES);
	}

	@Test
	void aFullMapIsNotScannedAgainForEveryNewAddress() {
		InvalidSessionTokenLimiter limiter = new InvalidSessionTokenLimiter(properties, Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"), ZoneOffset.UTC));

		for (int i = 0; i < InvalidSessionTokenLimiter.MAX_TRACKED_ADDRESSES + 2000; i++) {
			limiter.recordGuess("10." + (i / 65536) + "." + ((i / 256) % 256) + "." + (i % 256));
		}

		// without a minimum interval every one of the 2000 requests beyond the capacity would scan all entries
		assertThat(limiter.pruneRuns()).isLessThanOrEqualTo(1);
	}
}
