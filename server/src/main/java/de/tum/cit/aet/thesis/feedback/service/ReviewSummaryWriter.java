package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.entity.AIReviewSummary;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.repository.AIReviewSummaryRepository;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Keeps the single {@code (thesis, review type)} row that records the AI pipeline's latest score,
 * assessment, and summary — the "Overall Score" the feedback overview shows.
 *
 * <p>Only the auto flow writes here. That flow always persists its findings as real, already-visible
 * feedback rows, so the summary describes content the student can see. A preview must not write a
 * summary: it is supervisor-only and provisional, and persisting a discarded preview's score would
 * leak content the instructor never approved.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class ReviewSummaryWriter {
	private static final Logger log = LoggerFactory.getLogger(ReviewSummaryWriter.class);

	private final AIReviewSummaryRepository repository;
	private final ReviewDocuments documents;

	public ReviewSummaryWriter(AIReviewSummaryRepository repository, ReviewDocuments documents) {
		this.repository = repository;
		this.documents = documents;
	}

	/**
	 * Upserts the summary for one {@code (thesis, review type)} pair, unless the reviewed revision
	 * has meanwhile been superseded.
	 *
	 * @param thesis            the thesis that was reviewed
	 * @param reviewType        whether the proposal or the thesis document was reviewed
	 * @param result            the merged review result to persist
	 * @param documentVersionId the id of the document revision the review ran against, so the UI can
	 *                          tell a current summary from one describing an older upload
	 */
	public void record(Thesis thesis, ReviewType reviewType, ReviewResult result, UUID documentVersionId) {
		// A review takes long enough for the student to upload a new document while it runs. The
		// single (thesis, type) row would then be overwritten with a result about a superseded
		// revision — clobbering the summary of a review that already finished for the newer one.
		// Drop the stale result instead; the row stays on whatever revision is actually current.
		if (!documents.isCurrentRevision(thesis.getId(), reviewType, documentVersionId)) {
			log.info("Discarding AI review summary for thesis {} ({}): reviewed revision {} is no longer current",
					thesis.getId(), reviewType, documentVersionId);
			return;
		}

		try {
			save(thesis, reviewType, result, documentVersionId);
		} catch (DataIntegrityViolationException e) {
			// Two review runs for the same (thesis, type) raced: both found no existing row and both
			// tried to insert. Retry as an update against the row the other one just created, so a
			// concurrent request fails on this bookkeeping rather than on the review itself.
			log.debug("Retrying AI review summary upsert for thesis {} ({}) after a concurrent insert",
					thesis.getId(), reviewType);
			save(thesis, reviewType, result, documentVersionId);
		}
	}

	private void save(Thesis thesis, ReviewType reviewType, ReviewResult result, UUID documentVersionId) {
		AIReviewSummary summary = repository.findByThesisIdAndType(thesis.getId(), reviewType)
				.orElseGet(AIReviewSummary::new);
		summary.setThesis(thesis);
		summary.setType(reviewType);
		summary.setScore(result.normalizedScore());
		summary.setAssessment(result.assessment());
		summary.setSummary(result.summary());
		summary.setDocumentVersionId(documentVersionId);
		repository.save(summary);
	}
}
