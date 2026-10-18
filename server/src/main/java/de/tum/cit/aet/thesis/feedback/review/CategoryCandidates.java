package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;

import java.util.List;

/**
 * What one category pass of {@link CategoryFanOutReviewer} produced before the merge, and what
 * {@link FindingVerifier} did with it.
 *
 * <p>The merge rewrites, deduplicates and caps its input, so the final result says nothing about
 * how many findings each category generated or which of them verification removed. Both are what a
 * comparison of the verified and the unverified pipeline measures, so the reviewer hands this
 * record to {@link ReviewMetricsRecorder#recordCandidates} for every category that answered.
 *
 * @param category        the category the pass reviewed
 * @param generated       everything the category pass returned
 * @param ungrounded      dropped by the deterministic tier: a quote that is not in the document
 * @param rejected        dropped by the model tier: a requirement the category's rules do not state
 * @param kept            what went on to the merge
 * @param ruleCheckFailed whether the model tier failed and the grounded findings passed unverified
 */
public record CategoryCandidates(
		ReviewCategory category,
		List<Finding> generated,
		List<Finding> ungrounded,
		List<RejectedFinding> rejected,
		List<Finding> kept,
		boolean ruleCheckFailed
) {
	/** Copies every list, so a recorder may keep the record after the run has moved on. */
	public CategoryCandidates {
		generated = List.copyOf(generated);
		ungrounded = List.copyOf(ungrounded);
		rejected = List.copyOf(rejected);
		kept = List.copyOf(kept);
	}

	/**
	 * A pass whose findings went to the merge unchecked, because verification is off.
	 *
	 * @param category  the category the pass reviewed
	 * @param generated everything it returned
	 * @return the record, with every finding kept
	 */
	public static CategoryCandidates unverified(ReviewCategory category, List<Finding> generated) {
		return new CategoryCandidates(category, generated, List.of(), List.of(), generated, false);
	}

	/**
	 * A finding the verifier dropped, with the reason it gave.
	 *
	 * @param finding the dropped finding
	 * @param reason  the verifier's one-sentence justification; may be {@code null}
	 */
	public record RejectedFinding(Finding finding, String reason) {
	}
}
