package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Objects;

/**
 * Everything a {@link ThesisReviewer} needs for one run, and nothing about where it came from: no
 * thesis, no user, no persistence. A reviewer can therefore be exercised from a test or an offline
 * experiment with a PDF on disk and hand-built guidelines.
 *
 * @param type       whether the document is reviewed as a proposal or as a final thesis
 * @param guidelines the research group's structured guidelines, already checked to be ready
 * @param document   the PDF to review; the reviewer decides which parts of it it needs
 * @param progress   sink for per-call progress events; {@link ProgressReporter#NOOP} when nobody is
 *                   listening
 * @param earlierFeedback the entries already raised on earlier versions of the same document type,
 *                   which the reviewer compares the new version against; empty for a first review
 */
public record ReviewRequest(ReviewType type, StructuredGuidelines guidelines, Resource document,
		ProgressReporter progress, List<EarlierFeedback> earlierFeedback) {
	/** Rejects an incomplete request: every field is required for a review to mean anything. */
	public ReviewRequest {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(guidelines, "guidelines");
		Objects.requireNonNull(document, "document");
		Objects.requireNonNull(progress, "progress");
		earlierFeedback = earlierFeedback == null ? List.of() : List.copyOf(earlierFeedback);
	}

	/**
	 * A request without earlier feedback, for a first review or a caller that does not compare.
	 *
	 * @param type       whether the document is reviewed as a proposal or as a final thesis
	 * @param guidelines the research group's structured guidelines
	 * @param document   the PDF to review
	 * @param progress   sink for per-call progress events
	 */
	public ReviewRequest(ReviewType type, StructuredGuidelines guidelines, Resource document,
			ProgressReporter progress) {
		this(type, guidelines, document, progress, List.of());
	}

	/** Convenience constructor for callers that do not care about progress reporting. */
	public ReviewRequest(ReviewType type, StructuredGuidelines guidelines, Resource document) {
		this(type, guidelines, document, ProgressReporter.NOOP, List.of());
	}
}
