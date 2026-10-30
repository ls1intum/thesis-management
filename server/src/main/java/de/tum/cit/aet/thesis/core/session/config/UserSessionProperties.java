package de.tum.cit.aet.thesis.core.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Tunable limits of the application-owned user session ({@code thesis-management.session.*}).
 *
 * @param idleTimeout how long a session may stay unused before it is rejected
 * @param absoluteTimeout how long after login a session is rejected regardless of activity
 * @param maxPerUser maximum number of concurrent sessions per user; the oldest is ended when exceeded
 * @param lastUsedWriteInterval minimum time between two writes of a session's last-used timestamp
 * @param rotationGrace how long the previous cookie value keeps working after a session was rotated
 * @param endedRetention how long ended sessions are kept as account activity history
 * @param invalidTokenLimitPerMinute invalid session cookies per source IP and minute before that IP is throttled
 * @param userAgentMismatchAction what to do when the browser family or operating system changes mid-session
 */
@ConfigurationProperties(prefix = "thesis-management.session")
public record UserSessionProperties(
		@DefaultValue("PT4H") Duration idleTimeout,
		@DefaultValue("PT24H") Duration absoluteTimeout,
		@DefaultValue("10") int maxPerUser,
		@DefaultValue("PT5M") Duration lastUsedWriteInterval,
		@DefaultValue("PT30S") Duration rotationGrace,
		@DefaultValue("P30D") Duration endedRetention,
		@DefaultValue("20") int invalidTokenLimitPerMinute,
		@DefaultValue("END") UserAgentMismatchAction userAgentMismatchAction
) {
	/** Reaction to a changed browser family or operating system within one session. */
	public enum UserAgentMismatchAction {
		END,
		LOG
	}
}
