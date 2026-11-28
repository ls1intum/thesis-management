package de.tum.cit.aet.thesis.feedback.review;

import java.util.List;
import java.util.Objects;

/**
 * Structured-output target of the comparison call in {@link EarlierFeedbackChecker}: one verdict
 * per completed earlier feedback entry.
 *
 * @param verdicts the verdicts; never {@code null}
 */
public record EarlierFeedbackVerdicts(List<Verdict> verdicts) {
	/** Canonicalizes {@code verdicts}: a model may omit the list or put nulls in it. */
	public EarlierFeedbackVerdicts {
		verdicts = verdicts == null ? List.of() : verdicts.stream().filter(Objects::nonNull).toList();
	}

	/**
	 * The verdict on one earlier entry.
	 *
	 * @param ref         the entry's ref as given, e.g. {@code E3}
	 * @param status      RESOLVED, PERSISTS, or UNCLEAR
	 * @param page        for PERSISTS: the 1-based page of the new version that shows the defect
	 * @param section     for PERSISTS: the section of the new version that shows the defect
	 * @param quote       for PERSISTS: a verbatim passage of the new version showing the defect
	 * @param title       for PERSISTS: a one-line restatement of the defect
	 * @param explanation for PERSISTS: a more explicit explanation, with an example of a
	 *                    conforming passage
	 */
	public record Verdict(String ref, String status, Integer page, String section, String quote, String title,
			String explanation) {
	}
}
