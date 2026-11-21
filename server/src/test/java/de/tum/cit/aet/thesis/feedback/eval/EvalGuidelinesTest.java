package de.tum.cit.aet.thesis.feedback.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import org.junit.jupiter.api.Test;

class EvalGuidelinesTest {

	@Test
	void proposalsAreJudgedAgainstTheFixedProposalGuidelines() {
		assertThat(EvalGuidelines.forType(ReviewType.PROPOSAL)).isEqualTo(EvalGuidelines.build());
	}

	@Test
	void aThesisIsRejectedRatherThanJudgedAgainstProposalRules() {
		// Proposal page limits, proposal sections and a preliminary schedule would measure proposal
		// compliance, not thesis review quality.
		assertThatThrownBy(() -> EvalGuidelines.forType(ReviewType.THESIS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("thesis");
	}
}
