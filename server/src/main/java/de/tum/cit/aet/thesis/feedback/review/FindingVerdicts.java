package de.tum.cit.aet.thesis.feedback.review;

import java.util.List;
import java.util.Objects;

/**
 * What the verification pass returns: one verdict per candidate finding.
 *
 * <p>A record rather than a bare list because it is the structured-output target of an LLM call,
 * and a top-level array is not a valid JSON schema root for that.
 *
 * @param verdicts the per-finding decisions; never {@code null}
 */
public record FindingVerdicts(List<Verdict> verdicts) {

	/** Canonicalizes {@code verdicts}: a model may omit the list or put nulls in it. */
	public FindingVerdicts {
		verdicts = verdicts == null ? List.of()
				: verdicts.stream().filter(Objects::nonNull).toList();
	}

	/**
	 * One finding's verdict.
	 *
	 * @param finding the 1-based number of the finding this verdict refers to
	 * @param keep    whether the finding survives; {@code null} is read as "keep"
	 * @param reason  a one-sentence justification, logged when a finding is dropped
	 */
	public record Verdict(Integer finding, Boolean keep, String reason) {

		/**
		 * Whether this verdict drops its finding. A missing or unparseable {@code keep} is read as
		 * "keep": the verifier exists to remove fabrications, and a malformed verdict is not
		 * evidence that a finding is one.
		 *
		 * @return {@code true} when the finding should be discarded
		 */
		public boolean drops() {
			return Boolean.FALSE.equals(keep);
		}
	}
}
