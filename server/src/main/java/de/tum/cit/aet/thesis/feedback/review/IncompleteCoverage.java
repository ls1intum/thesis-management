package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Marks a {@link ReviewResult} as incomplete when some review categories were never checked.
 *
 * <p>Every strategy can come up short in its own way — a fan-out loses a category whose LLM call
 * failed, an agent runs out of turns before it gets to one — but what must then happen to the
 * result is identical, which is why it lives here rather than in either pipeline.
 *
 * <p>A result that covers a subset of the checks still reads like a verdict on all of them. Left
 * alone, a run where eight of the nine categories never happened and the ninth found nothing would
 * be shown — and persisted — as a clean bill of health. The live progress list does mark each
 * missing check, but it is gone the moment the review finishes and never reaches whoever opens the
 * thesis later.
 *
 * <p>An incomplete run therefore keeps its findings, which are real, and gives up the two claims it
 * cannot support: the overall score and the assessment are dropped, and the summary leads with
 * which checks are missing. Both the supervisor's preview and the persisted summary row read
 * exactly those three fields, so the caveat travels with the result rather than living in a log
 * line.
 */
public final class IncompleteCoverage {

	private IncompleteCoverage() {
	}

	/**
	 * Qualifies a result whose run did not cover every category.
	 *
	 * @param result  the result as the strategy produced it, describing only the categories it
	 *                checked; may be {@code null}
	 * @param missing the categories that were never checked; empty for a complete run
	 * @return {@code result} unchanged for a complete run, otherwise a qualified copy
	 */
	public static ReviewResult qualify(ReviewResult result, List<ReviewCategory> missing) {
		if (missing.isEmpty() || result == null) {
			return result;
		}

		String names = missing.stream().map(ReviewCategory::getDisplayName).collect(Collectors.joining(", "));
		String warning = ("Incomplete review: %d of %d checks could not be completed (%s). "
				+ "The findings below cover only the checks that ran, so no overall score or assessment is given.")
						.formatted(missing.size(), ReviewCategory.values().length, names);
		String summary = result.summary() == null || result.summary().isBlank()
				? warning
				: warning + "\n\n" + result.summary();

		return new ReviewResult(null, null, summary, result.findings());
	}
}
