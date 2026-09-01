package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.model.Finding;

import java.util.List;
import java.util.Objects;

/**
 * What one {@link CategoryReviewer} pass returns, before the merge step deduplicates and ranks
 * across categories. Kept as a record rather than a bare list because it is the structured-output
 * target of an LLM call, and a top-level array is not a valid JSON schema root for that.
 *
 * @param findings the issues this category's pass found; never {@code null}
 */
public record CategoryFindings(List<Finding> findings) {
	public CategoryFindings {
		findings = findings == null ? List.of()
				: findings.stream().filter(Objects::nonNull).toList();
	}
}
