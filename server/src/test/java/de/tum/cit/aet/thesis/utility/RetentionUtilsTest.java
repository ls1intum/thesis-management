package de.tum.cit.aet.thesis.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

class RetentionUtilsTest {

	private static Instant berlinDate(int year, int month, int day) {
		return ZonedDateTime.of(year, month, day, 12, 0, 0, 0, RetentionUtils.BERLIN).toInstant();
	}

	private static Instant endOfYear(int year) {
		return ZonedDateTime.of(year, 12, 31, 23, 59, 59, 0, RetentionUtils.BERLIN).toInstant();
	}

	private static ThesisStateChange stateChange(Thesis thesis, ThesisState state, Instant changedAt) {
		ThesisStateChangeId id = new ThesisStateChangeId();
		id.setState(state);
		ThesisStateChange sc = new ThesisStateChange();
		sc.setId(id);
		sc.setThesis(thesis);
		sc.setChangedAt(changedAt);
		return sc;
	}

	private static Thesis createThesis(Instant createdAt, Instant endDate, Set<ThesisStateChange> states) {
		Thesis thesis = new Thesis();
		thesis.setCreatedAt(createdAt);
		thesis.setEndDate(endDate);
		if (states != null) {
			thesis.setStates(states);
		}
		return thesis;
	}

	@Nested
	class EndDateBased {

		@Test
		void endDateInJanuary_expiresEndOfCalendarYearPlusFive() {
			// endDate = Jan 15, 2020 → retention expires Dec 31, 2025
			Thesis thesis = createThesis(berlinDate(2019, 6, 1), berlinDate(2020, 1, 15), null);

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);

			assertThat(expiry).isEqualTo(endOfYear(2025));
		}

		@Test
		void endDateInDecember_expiresEndOfCalendarYearPlusFive() {
			// endDate = Dec 20, 2020 → retention expires Dec 31, 2025
			Thesis thesis = createThesis(berlinDate(2019, 6, 1), berlinDate(2020, 12, 20), null);

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);

			assertThat(expiry).isEqualTo(endOfYear(2025));
		}

		@Test
		void endDateOnNewYearsEve_expiresEndOfCalendarYearPlusFive() {
			// endDate = Dec 31, 2020 → retention expires Dec 31, 2025
			Thesis thesis = createThesis(berlinDate(2019, 6, 1), berlinDate(2020, 12, 31), null);

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);

			assertThat(expiry).isEqualTo(endOfYear(2025));
		}

		@Test
		void endDateOnNewYearsDay_expiresEndOfThatCalendarYearPlusFive() {
			// endDate = Jan 1, 2021 → retention expires Dec 31, 2026
			Thesis thesis = createThesis(berlinDate(2020, 6, 1), berlinDate(2021, 1, 1), null);

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);

			assertThat(expiry).isEqualTo(endOfYear(2026));
		}
	}

	@Nested
	class StateChangeFallback {

		@Test
		void usesLatestStateChangeWhenEndDateIsNull() {
			// No endDate, latest state change on Mar 5, 2019 → retention expires Dec 31, 2024
			Thesis thesis = createThesis(berlinDate(2018, 1, 1), null, new HashSet<>());
			thesis.getStates().add(stateChange(thesis, ThesisState.PROPOSAL, berlinDate(2018, 6, 1)));
			thesis.getStates().add(stateChange(thesis, ThesisState.DROPPED_OUT, berlinDate(2019, 3, 5)));

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);

			assertThat(expiry).isEqualTo(endOfYear(2024));
		}

		@Test
		void usesStateChangeWhenLaterThanEndDate() {
			// endDate = Dec 2019, but state change in Jan 2020 → use Jan 2020 → expires Dec 31, 2025
			Thesis thesis = createThesis(berlinDate(2018, 1, 1), berlinDate(2019, 12, 15), new HashSet<>());
			thesis.getStates().add(stateChange(thesis, ThesisState.FINISHED, berlinDate(2020, 1, 10)));

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);

			// State change crosses year boundary, proving it was used over endDate
			assertThat(expiry).isEqualTo(endOfYear(2025));
		}

		@Test
		void ignoresStateChangeWhenEarlierThanEndDate() {
			// endDate = Jun 2020, state change in Mar 2019 → use endDate → expires Dec 31, 2025
			Thesis thesis = createThesis(berlinDate(2018, 1, 1), berlinDate(2020, 6, 15), new HashSet<>());
			thesis.getStates().add(stateChange(thesis, ThesisState.WRITING, berlinDate(2019, 3, 1)));

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);

			assertThat(expiry).isEqualTo(endOfYear(2025));
		}
	}

	@Nested
	class CreatedAtFallback {

		@Test
		void usesCreatedAtWhenNoEndDateAndNoStateChanges() {
			// No endDate, no state changes, createdAt = Jul 2018 → retention expires Dec 31, 2023
			Thesis thesis = createThesis(berlinDate(2018, 7, 15), null, new HashSet<>());

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);

			assertThat(expiry).isEqualTo(endOfYear(2023));
		}

		@Test
		void throwsWhenAllDatesAreNull() {
			Thesis thesis = createThesis(null, null, new HashSet<>());

			assertThatThrownBy(() -> RetentionUtils.computeRetentionExpiry(thesis))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("no activity dates");
		}
	}
}
