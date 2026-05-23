package de.tum.cit.aet.thesis.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Test-only {@link Clock} that can be pinned to a fixed instant or fall back to live system
 * time. Tests pin it via {@link #setInstant(Instant)} for deterministic date arithmetic and
 * call {@link #unfreeze()} (typically in {@code @AfterEach}) to restore live behavior so
 * unrelated tests using {@code Instant.now()} are not affected.
 */
public class MutableClock extends Clock {
	private volatile Instant pinned;
	private volatile ZoneId zone = ZoneId.of("UTC");

	public void setInstant(Instant instant) {
		this.pinned = instant;
	}

	public void unfreeze() {
		this.pinned = null;
	}

	@Override
	public Instant instant() {
		return pinned != null ? pinned : Instant.now();
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId newZone) {
		MutableClock copy = new MutableClock();
		copy.pinned = this.pinned;
		copy.zone = newZone;
		return copy;
	}
}
