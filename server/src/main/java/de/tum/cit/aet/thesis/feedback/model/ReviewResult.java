package de.tum.cit.aet.thesis.feedback.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * What every reviewer implementation returns: an overall verdict on the document plus the
 * consolidated, actionable findings.
 *
 * @param assessment the overall verdict, or {@code null} when the model emitted an unknown token
 * @param score      the model's 0-100 quality score; use {@link #normalizedScore()} to read it
 * @param summary    a two-to-three sentence summary of strengths and weaknesses
 * @param findings   the deduplicated findings, ranked most to least severe; never {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ReviewResult(
		@JsonAlias("category") AssessmentCategory assessment,
		Integer score,
		String summary,
		List<Finding> findings
) {
	/** Canonicalizes {@code findings}: a model may omit the list or put nulls in it. */
	public ReviewResult {
		findings = findings == null ? List.of()
				: findings.stream().filter(Objects::nonNull).toList();
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
}
