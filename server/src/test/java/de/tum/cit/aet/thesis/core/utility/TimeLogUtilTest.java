package de.tum.cit.aet.thesis.core.utility;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeLogUtilTest {

	@Test
	void formatDurationFrom_subMillisecondElapsed_returnsMicroseconds() {
		long now = System.nanoTime();
		// elapsed will be a very small number of microseconds
		String result = TimeLogUtil.formatDurationFrom(now);
		assertTrue(result.endsWith("µs"), "Expected µs unit, got: " + result);
	}

	@Test
	void formatDurationFrom_aFewMilliseconds_returnsMilliseconds() {
		// 5 ms back in nanos
		long start = System.nanoTime() - 5_000_000L;
		String result = TimeLogUtil.formatDurationFrom(start);
		assertTrue(result.endsWith("ms"), "Expected ms unit, got: " + result);
	}

	@Test
	void formatDurationFrom_aFewSeconds_returnsSeconds() {
		// 5 s back in nanos
		long start = System.nanoTime() - 5_000_000_000L;
		String result = TimeLogUtil.formatDurationFrom(start);
		assertTrue(result.endsWith("sec"), "Expected sec unit, got: " + result);
	}

	@Test
	void formatDurationFrom_aFewMinutes_returnsMinutes() {
		// 2 minutes back in nanos
		long start = System.nanoTime() - 120_000_000_000L;
		String result = TimeLogUtil.formatDurationFrom(start);
		assertTrue(result.endsWith("min"), "Expected min unit, got: " + result);
	}

	@Test
	void formatDurationFrom_aFewHours_returnsHours() {
		// 2 hours back in nanos
		long start = System.nanoTime() - 7_200_000_000_000L;
		String result = TimeLogUtil.formatDurationFrom(start);
		assertTrue(result.endsWith("hours"), "Expected hours unit, got: " + result);
	}

	@Test
	void constructor_isInvocable() {
		// Cover the implicit default constructor.
		new TimeLogUtil();
	}
}
