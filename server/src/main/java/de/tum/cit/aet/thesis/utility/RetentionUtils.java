package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisStateChange;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class RetentionUtils {
	public static final int RETENTION_YEARS = 5;
	public static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

	private RetentionUtils() {
	}

	/**
	 * Computes when retention expires for a thesis: 5 years after end of calendar year
	 * of the latest thesis activity (endDate, max stateChange, or createdAt).
	 */
	public static Instant computeRetentionExpiry(Thesis thesis) {
		// Use endDate as the primary indicator for completed theses
		Instant latestActivity = thesis.getEndDate();

		// Fall back to the latest state change if endDate is not set
		Instant latestStateChange = thesis.getStates().stream()
				.map(ThesisStateChange::getChangedAt)
				.max(Instant::compareTo)
				.orElse(null);
		if (latestActivity == null) {
			latestActivity = latestStateChange;
		} else if (latestStateChange != null && latestStateChange.isAfter(latestActivity)) {
			latestActivity = latestStateChange;
		}

		// Fall back to createdAt if neither endDate nor state changes are available
		if (latestActivity == null) {
			latestActivity = thesis.getCreatedAt();
		}

		ZonedDateTime zdt = latestActivity.atZone(BERLIN);
		return ZonedDateTime.of(zdt.getYear() + RETENTION_YEARS, 12, 31, 23, 59, 59, 0, BERLIN)
				.toInstant();
	}
}
