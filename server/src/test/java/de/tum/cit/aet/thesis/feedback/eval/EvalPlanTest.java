package de.tum.cit.aet.thesis.feedback.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EvalPlanTest {

	private static final String[] REQUIRED = {
			"--models=openai/gpt-oss-120b", "--corpus=/tmp/corpus",
			"--base-url=http://endpoint", "--api-key=secret"};

	private static String[] with(String... extra) {
		String[] args = new String[REQUIRED.length + extra.length];
		System.arraycopy(REQUIRED, 0, args, 0, REQUIRED.length);
		System.arraycopy(extra, 0, args, REQUIRED.length, extra.length);
		return args;
	}

	@Test
	void everyApproachIsSweptUnlessNarrowed() {
		// The default is the comparison itself; naming one arm is the special case.
		assertThat(EvalPlan.parse(REQUIRED).approaches()).containsExactly(
				"single-prompt", "category-fan-out-unverified", "category-fan-out-verified", "agentic");
	}

	@Test
	void theTwoFanOutArmsRunTheSameReviewerAndDifferOnlyInVerification() {
		EvalApproach unverified = EvalApproach.fromId("category-fan-out-unverified");
		EvalApproach verified = EvalApproach.fromId("category-fan-out-verified");

		assertThat(unverified.reviewer()).isEqualTo(verified.reviewer()).isEqualTo("category-fan-out");
		assertThat(unverified.verifyFindings()).isFalse();
		assertThat(verified.verifyFindings()).isTrue();
	}

	@Test
	void aBareFanOutIsRejectedBecauseItWouldInheritTheVerificationDefault() {
		assertThatThrownBy(() -> EvalPlan.parse(with("--approaches=category-fan-out")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("category-fan-out-verified");
	}

	@Test
	void modelsAndApproachesAreCommaSeparatedAndKeepTheirOrder() {
		EvalPlan plan = EvalPlan.parse(with("--models=a/one, b/two", "--approaches=agentic"));

		assertThat(plan.models()).containsExactly("a/one", "b/two");
		assertThat(plan.approaches()).containsExactly("agentic");
	}

	@Test
	void aMisspelledApproachFailsBeforeAnythingIsBooted() {
		// Otherwise the context comes up with no ThesisReviewer bean and the sweep dies hours in
		// with a bean-not-found error that says nothing about the typo.
		assertThatThrownBy(() -> EvalPlan.parse(with("--approaches=agentik")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("agentik");
	}

	@Test
	void aMissingRequiredValueNamesTheFlagAndItsEnvironmentVariable() {
		assertThatThrownBy(() -> EvalPlan.parse(new String[]{"--models=a/one"}))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("--corpus")
				.hasMessageContaining("EVAL_CORPUS");
	}

	@Test
	void aMalformedArgumentIsRejectedRatherThanIgnored() {
		assertThatThrownBy(() -> EvalPlan.parse(with("seeds", "3")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("--key=value");
	}

	@Test
	void theRunCountIsTheFullCrossProduct() {
		EvalPlan plan = EvalPlan.parse(with("--models=a/one,b/two", "--seeds=3"));

		assertThat(plan.totalRuns(4)).isEqualTo(2 * 4 * 4 * 3);
	}

	@Test
	void aModelNameWithASlashCannotSilentlyNestADirectory() {
		assertThat(EvalPlan.slug("openai/gpt-oss-120b")).isEqualTo("openai_gpt-oss-120b");
		assertThat(EvalPlan.slug("Qwen/Qwen3-32B")).isEqualTo("qwen_qwen3-32b");
	}
}
