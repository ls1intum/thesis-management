package de.tum.cit.aet.thesis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides a system-wide {@link Clock} bean so services can read the current time
 * through dependency injection rather than calling {@code Instant.now()} directly.
 * Tests override this bean with a fixed clock to make time-sensitive logic deterministic.
 */
@Configuration
public class ClockConfig {
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
