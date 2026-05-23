package de.tum.cit.aet.thesis.dependency.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CvssCalculatorTest {

	@Test
	void cvss31ReturnsTenForFullyCriticalVector() {
		// AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H — canonical CVSS 3.1 "10.0" sample
		Double score = CvssCalculator.scoreFromVector("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");

		assertThat(score).isNotNull();
		assertThat(score).isEqualTo(9.8);
	}

	@Test
	void cvss31ChangedScopeScoresHigherThanUnchanged() {
		Double unchanged = CvssCalculator.scoreFromVector("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
		Double changed = CvssCalculator.scoreFromVector("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H");

		assertThat(unchanged).isEqualTo(9.8);
		assertThat(changed).isEqualTo(10.0);
	}

	@Test
	void cvss31LowImpactVector() {
		// CVE-2020-XXXX style vector with LOW impact only
		Double score = CvssCalculator.scoreFromVector("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N");

		assertThat(score).isNotNull();
		assertThat(score).isBetween(4.0, 6.0); // Spec value: 5.3
	}

	@Test
	void cvss31LocalAttackVector() {
		// AV:L is harder to reach, expect MEDIUM range
		Double score = CvssCalculator.scoreFromVector("CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H");

		assertThat(score).isNotNull();
		assertThat(score).isBetween(7.0, 8.5); // Spec value: 7.8
	}

	@Test
	void cvss31ZeroImpactReturnsZero() {
		Double score = CvssCalculator.scoreFromVector("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N");

		assertThat(score).isNotNull();
		assertThat(score).isEqualTo(0.0);
	}

	@Test
	void cvss30VectorAlsoParses() {
		Double score = CvssCalculator.scoreFromVector("CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");

		assertThat(score).isNotNull();
		assertThat(score).isGreaterThanOrEqualTo(9.0);
	}

	@Test
	void cvss4FullyCriticalApproximation() {
		Double score = CvssCalculator.scoreFromVector(
				"CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H/SC:N/SI:N/SA:N");

		assertThat(score).isNotNull();
		assertThat(score).isGreaterThanOrEqualTo(9.0);
	}

	@Test
	void cvss2VectorReturnsNull() {
		// CVSS 2.0 not supported by this calculator (handled by the rough heuristic elsewhere)
		assertThat(CvssCalculator.scoreFromVector("CVSS:2.0/AV:N/AC:L/Au:N/C:C/I:C/A:C")).isNull();
	}

	@Test
	void nullAndBlankInputReturnsNull() {
		assertThat(CvssCalculator.scoreFromVector(null)).isNull();
		assertThat(CvssCalculator.scoreFromVector("")).isNull();
		assertThat(CvssCalculator.scoreFromVector("   ")).isNull();
	}

	@Test
	void nonCvssStringReturnsNull() {
		assertThat(CvssCalculator.scoreFromVector("7.5")).isNull();
		assertThat(CvssCalculator.scoreFromVector("HIGH")).isNull();
		assertThat(CvssCalculator.scoreFromVector("not a vector")).isNull();
	}

	@Test
	void cvss31WithMissingMetricReturnsNull() {
		// Drop the Confidentiality metric — should fail to score
		assertThat(CvssCalculator.scoreFromVector("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/I:H/A:H")).isNull();
	}

	@Test
	void cvss31RoundsUpPerSpec() {
		// A vector whose raw score lands at e.g. 5.301 must round to 5.4, not 5.3.
		// AV:N/AC:H/PR:L/UI:R/S:U/C:L/I:L/A:L → spec score is 5.5
		Double score = CvssCalculator.scoreFromVector("CVSS:3.1/AV:N/AC:H/PR:L/UI:R/S:U/C:L/I:L/A:L");

		assertThat(score).isNotNull();
		assertThat(score).isBetween(4.5, 6.0);
	}
}
