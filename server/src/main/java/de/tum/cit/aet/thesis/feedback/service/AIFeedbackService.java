package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.FeedbackClassificationDTO;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.FeedbackClassificationResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.review.ReviewRequest;
import de.tum.cit.aet.thesis.feedback.review.ThesisReviewer;
import de.tum.cit.aet.thesis.feedback.service.ReviewDocuments.ReviewDocument;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSource;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.thesis.controller.payload.RequestChangesPayload.RequestedChange;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.service.ThesisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The application-side half of the AI feedback feature: gate on the research group's guidelines,
 * pick the document to review, hand it to whichever {@link ThesisReviewer} is configured, and turn
 * the findings into either persisted thesis feedback (auto flow, student-driven) or editable drafts
 * for an instructor preview. Also serves the much smaller classification request an instructor
 * makes while typing a feedback line by hand.
 *
 * <p>Knows nothing about prompts, models, or PDFs — swapping the review strategy does not touch
 * this class.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class AIFeedbackService {
	private static final Logger log = LoggerFactory.getLogger(AIFeedbackService.class);

	/**
	 * Most urgent first. {@link de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity} is
	 * declared in that order, so its natural order is the presentation order.
	 */
	private static final Comparator<AIFeedbackDraftDTO> BY_SEVERITY = Comparator.comparing(
			AIFeedbackDraftDTO::severity, Comparator.nullsLast(Comparator.naturalOrder()));

	/**
	 * Upper bound on the text handed to the classification LLM. A feedback line is a sentence or
	 * two; anything longer is pasted prose, and classifying its opening is enough to pick a label
	 * without paying for the whole blob.
	 */
	private static final int MAX_CLASSIFICATION_CHARS = 2000;

	private final ThesisReviewer reviewer;
	private final ThesisService thesisService;
	private final GuidelinesGate guidelinesGate;
	private final ReviewDocuments documents;
	private final ReviewSummaryWriter summaryWriter;
	private final FeedbackClassificationService feedbackClassificationService;

	/**
	 * Creates the AI feedback service.
	 *
	 * @param reviewer                      the configured review strategy
	 * @param thesisService                 the service used to persist feedback
	 * @param guidelinesGate                the per-research-group gate on the AI features
	 * @param documents                     resolves which uploaded document a review runs against
	 * @param summaryWriter                 persists the latest score/assessment/summary per
	 *                                      (thesis, review type)
	 * @param feedbackClassificationService the single-call classifier used to suggest a category and
	 *                                      severity for a manually written feedback line
	 */
	public AIFeedbackService(ThesisReviewer reviewer, ThesisService thesisService, GuidelinesGate guidelinesGate,
			ReviewDocuments documents, ReviewSummaryWriter summaryWriter,
			FeedbackClassificationService feedbackClassificationService) {
		this.reviewer = reviewer;
		this.thesisService = thesisService;
		this.guidelinesGate = guidelinesGate;
		this.documents = documents;
		this.summaryWriter = summaryWriter;
		this.feedbackClassificationService = feedbackClassificationService;
	}

	/**
	 * Runs a review and persists the findings on the thesis as feedback items with
	 * {@code generationSource = AI}. Access control is delegated to the caller (typically the
	 * controller, which enforces student/supervisor access based on the review type).
	 *
	 * @param thesis     the thesis whose uploaded document is reviewed
	 * @param reviewType whether to review the proposal or the thesis document
	 * @return the updated thesis with the new feedback rows attached
	 */
	public Thesis autoReviewAndSave(Thesis thesis, ReviewType reviewType) {
		Reviewed reviewed = execute(thesis, reviewType);

		List<RequestedChange> changes = reviewed.drafts().stream()
				.map(draft -> new RequestedChange(
						draft.feedback(),
						false,
						draft.category(),
						draft.severity(),
						// Null → inherit the batch source (AI) passed to requestChanges below.
						null))
				.toList();

		Thesis updated = thesis;
		if (changes.isEmpty()) {
			log.info("AI auto review for thesis {} ({}) produced no actionable findings", thesis.getId(), reviewType);
		} else {
			updated = thesisService.requestChanges(thesis, toFeedbackType(reviewType), changes, ThesisFeedbackSource.AI);
		}

		// Last, so the summary only ever describes findings that actually landed: requestChanges is
		// transactional, and persisting the score first would leave it standing on its own if saving
		// the feedback rows failed and rolled them back.
		summaryWriter.record(thesis, reviewType, reviewed.result(), reviewed.documentVersionId());

		return updated;
	}

	/**
	 * Runs a review and returns the findings as editable drafts without saving anything. The
	 * instructor UI can then let the user tweak, accept, or drop individual entries before
	 * persisting them. Deliberately writes no summary — see {@link ReviewSummaryWriter}.
	 *
	 * @param thesis     the thesis whose uploaded document is reviewed
	 * @param reviewType whether to review the proposal or the thesis document
	 * @return the assessment, summary, and editable drafts
	 */
	public AIPreviewResponseDTO previewReview(Thesis thesis, ReviewType reviewType) {
		Reviewed reviewed = execute(thesis, reviewType);
		ReviewResult result = reviewed.result();

		return new AIPreviewResponseDTO(
				result.assessment(),
				result.normalizedScore(),
				result.summary(),
				reviewed.drafts().stream().sorted(BY_SEVERITY).toList());
	}

	/**
	 * Suggests a category and severity for a single feedback line an instructor typed by hand, so
	 * the two dropdowns they would otherwise fill in themselves come pre-selected.
	 *
	 * <p>Nothing is persisted: the suggestion is applied to the instructor's unsaved draft row and
	 * can be changed or cleared before the batch is saved. Either field may come back {@code null}
	 * when the model did not answer with a usable value — a missing suggestion leaves that dropdown
	 * to the instructor rather than guessing on their behalf.
	 *
	 * @param thesis   the thesis the feedback is written for; used to resolve the research group's
	 *                 AI opt-in
	 * @param feedback the feedback line to classify
	 * @return the suggested category and severity, either of which may be {@code null}
	 */
	public FeedbackClassificationDTO classifyFeedbackLine(Thesis thesis, String feedback) {
		// Same per-group gate as every other AI feature: a group whose lead has not set up (valid)
		// guidelines has not opted in, so no LLM call is made on its behalf. The guidelines
		// themselves do not influence the classification.
		guidelinesGate.requireReady(thesis.getResearchGroup());

		String line = feedback == null ? "" : feedback.strip();
		if (line.isEmpty()) {
			throw new ResourceInvalidParametersException("Cannot classify an empty feedback line.");
		}
		if (line.length() > MAX_CLASSIFICATION_CHARS) {
			line = line.substring(0, MAX_CLASSIFICATION_CHARS);
		}

		FeedbackClassificationResult result = feedbackClassificationService.classify(line);
		if (result == null) {
			log.warn("Feedback classification returned no result for thesis {}", thesis.getId());
			return new FeedbackClassificationDTO(null, null);
		}

		return new FeedbackClassificationDTO(
				FeedbackMapper.toCategory(result.category()),
				FeedbackMapper.toSeverity(result.severity()));
	}

	/**
	 * Throws if the thesis does not have an uploaded document for the given review type. Public
	 * because the controller wants to check before doing the work.
	 *
	 * @param thesis     the thesis whose uploaded document is required
	 * @param reviewType whether the proposal or the thesis document is required
	 */
	public void assertHasDocument(Thesis thesis, ReviewType reviewType) {
		documents.load(thesis, reviewType);
	}

	/** One completed review: the raw result, its drafts, and the revision it ran against. */
	private record Reviewed(ReviewResult result, List<AIFeedbackDraftDTO> drafts, UUID documentVersionId) {
	}

	/**
	 * The shared path both flows take. Gates on guidelines before touching the document, so a group
	 * that has not set up the feature gets that explanation rather than a complaint about a missing
	 * upload.
	 */
	private Reviewed execute(Thesis thesis, ReviewType reviewType) {
		StructuredGuidelines guidelines = guidelinesGate.requireReady(thesis.getResearchGroup());
		ReviewDocument document = documents.load(thesis, reviewType);

		ReviewResult result = reviewer.review(new ReviewRequest(reviewType, guidelines, document.resource()));
		log.debug("Review of thesis {} ({}) by {} produced {} findings",
				thesis.getId(), reviewType, reviewer.strategy(), result.findings().size());

		return new Reviewed(
				result,
				result.findings().stream().map(FeedbackMapper::toDraft).toList(),
				document.versionId());
	}

	private static ThesisFeedbackType toFeedbackType(ReviewType reviewType) {
		return switch (reviewType) {
			case PROPOSAL -> ThesisFeedbackType.PROPOSAL;
			case THESIS -> ThesisFeedbackType.THESIS;
		};
	}
}
