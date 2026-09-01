package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import org.springframework.core.io.Resource;

import java.util.Objects;

/**
 * Everything a {@link ThesisReviewer} needs for one run, and nothing about where it came from: no
 * thesis, no user, no persistence. A reviewer can therefore be exercised from a test or an offline
 * experiment with a PDF on disk and hand-built guidelines.
 *
 * @param type       whether the document is reviewed as a proposal or as a final thesis
 * @param guidelines the research group's structured guidelines, already checked to be ready
 * @param document   the PDF to review; the reviewer decides which parts of it it needs
 */
public record ReviewRequest(ReviewType type, StructuredGuidelines guidelines, Resource document) {
	public ReviewRequest {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(guidelines, "guidelines");
		Objects.requireNonNull(document, "document");
	}
}
