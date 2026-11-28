package de.tum.cit.aet.thesis.feedback.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * What every reviewer implementation returns: an overall verdict on the document plus the
 * consolidated, actionable findings, and — when earlier feedback existed — how the new version
 * relates to it.
 *
 * <p>Not a structured-output target: the model's own answer is parsed into
 * {@code feedback.review.ReviewVerdict}, which lacks the comparison fields, so a model is never
 * asked to fill them in. They are set by the reviewer from the dedicated comparison call or the
 * agent's comparison tools.
 *
 * @param assessment   the overall verdict, or {@code null} when the model emitted an unknown token
 * @param score        the model's 0-100 quality score; use {@link #normalizedScore()} to read it
 * @param summary      a two-to-three sentence summary of strengths and weaknesses
 * @param findings     the deduplicated findings, ranked most to least severe; never {@code null}
 * @param followUps    defects of completed earlier entries that recur in this version; never
 *                     {@code null}
 * @param resolvedRefs refs of completed earlier entries whose defect this version no longer
 *                     shows; never {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ReviewResult(
		@JsonAlias("category") AssessmentCategory assessment,
		Integer score,
		String summary,
		List<Finding> findings,
		List<FollowUp> followUps,
		List<String> resolvedRefs
) {
	/** Canonicalizes the lists: a model may omit them or put nulls in them. */
	public ReviewResult {
		findings = findings == null ? List.of()
				: findings.stream().filter(Objects::nonNull).toList();
		followUps = followUps == null ? List.of()
				: followUps.stream().filter(Objects::nonNull).toList();
		resolvedRefs = resolvedRefs == null ? List.of()
				: resolvedRefs.stream().filter(Objects::nonNull).toList();
	}

	/**
	 * A result without any comparison against earlier feedback.
	 *
	 * @param assessment the overall verdict
	 * @param score      the 0-100 quality score
	 * @param summary    the summary
	 * @param findings   the findings
	 */
	public ReviewResult(AssessmentCategory assessment, Integer score, String summary, List<Finding> findings) {
		this(assessment, score, summary, findings, List.of(), List.of());
	}

	/**
	 * The score, but only when it is actually usable. It comes straight from a model's structured
	 * output — prompts ask for an integer 0-100, yet nothing enforces that at the schema level — so
	 * a missing or out-of-range value is reported as "no score" rather than shown or persisted as a
	 * bogus number.
	 *
	 * @return the score when it is an integer in [0, 100], otherwise {@code null}
	 */
	public Integer normalizedScore() {
		return score == null || score < 0 || score > 100 ? null : score;
	}

	/**
	 * A copy with another summary, keeping everything else.
	 *
	 * @param newSummary the replacement summary
	 * @return the copy
	 */
	public ReviewResult withSummary(String newSummary) {
		return new ReviewResult(assessment, score, newSummary, findings, followUps, resolvedRefs);
	}

	/**
	 * A copy carrying the given comparison against earlier feedback.
	 *
	 * @param newFollowUps    the recurring defects
	 * @param newResolvedRefs the refs of the entries this version resolved
	 * @return the copy
	 */
	public ReviewResult withComparison(List<FollowUp> newFollowUps, List<String> newResolvedRefs) {
		return new ReviewResult(assessment, score, summary, findings, newFollowUps, newResolvedRefs);
	}
}
