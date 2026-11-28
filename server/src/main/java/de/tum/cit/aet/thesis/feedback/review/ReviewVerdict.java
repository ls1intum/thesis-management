package de.tum.cit.aet.thesis.feedback.review;

import com.fasterxml.jackson.annotation.JsonAlias;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;

import java.util.List;

/**
 * The structured-output target of a call that produces a whole review — the fan-out's merge and the
 * single-prompt baseline. Kept apart from {@link ReviewResult} so the JSON schema the model sees
 * holds only what the model is asked for: the comparison against earlier feedback is filled in by
 * code, and listing it in the schema would invite the model to invent it.
 *
 * @param assessment the overall verdict, or {@code null} when the model emitted an unknown token
 * @param score      the model's 0-100 quality score
 * @param summary    a two-to-three sentence summary of strengths and weaknesses
 * @param findings   the deduplicated findings, ranked most to least severe
 */
public record ReviewVerdict(
		@JsonAlias("category") AssessmentCategory assessment,
		Integer score,
		String summary,
		List<Finding> findings
) {
	/**
	 * The verdict as a review result without any comparison.
	 *
	 * @return the result
	 */
	public ReviewResult toResult() {
		return new ReviewResult(assessment, score, summary, findings);
	}
}
