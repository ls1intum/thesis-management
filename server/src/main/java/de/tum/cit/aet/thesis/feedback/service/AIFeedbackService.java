package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.core.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.FindingDTO;
import de.tum.cit.aet.thesis.feedback.dto.Location;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.entity.AIReviewSummary;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.repository.AIReviewSummaryRepository;
import de.tum.cit.aet.thesis.feedback.repository.ResearchGroupGuidelinesRepository;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import de.tum.cit.aet.thesis.proposal.entity.ThesisProposal;
import de.tum.cit.aet.thesis.proposal.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSource;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.thesis.controller.payload.RequestChangesPayload;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.entity.ThesisFile;
import de.tum.cit.aet.thesis.thesis.repository.ThesisFileRepository;
import de.tum.cit.aet.thesis.thesis.service.ThesisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Runs the AI review pipeline against a thesis's existing uploaded PDF and either (a) persists
 * the findings as {@code ThesisFeedback} rows (auto flow, student-driven) or (b) shapes them
 * as editable drafts for an instructor preview (preview flow).
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class AIFeedbackService {
	private static final Logger log = LoggerFactory.getLogger(AIFeedbackService.class);

	/**
	 * Shown when the thesis's research group has never configured AI review guidelines. Students
	 * cannot fix this themselves, so the message names who can and where.
	 */
	private static final String GUIDELINES_MISSING_MESSAGE =
			"AI review is not set up for your research group yet. Please ask your examiner or your research "
					+ "group lead to add the group's writing guidelines under Research Group Settings → AI Review "
					+ "Guidelines. Once they are saved, AI feedback becomes available for everyone in the group.";

	/**
	 * Shown when guidelines exist but preprocessing rejected them as too vague (or they are still
	 * being processed) — a different fix than the missing case, so it gets its own wording.
	 */
	private static final String GUIDELINES_NOT_READY_MESSAGE =
			"AI review is not available yet because your research group's writing guidelines could not be turned "
					+ "into review rules. Please ask your examiner or your research group lead to revise them under "
					+ "Research Group Settings → AI Review Guidelines.";

	/** Severity levels sorted from most to least urgent for stable preview ordering. */
	private static final Comparator<ThesisFeedbackSeverity> SEVERITY_ORDER = Comparator.comparingInt(sev -> switch (sev) {
		case CRITICAL -> 0;
		case MAJOR -> 1;
		case MINOR -> 2;
		case SUGGESTION -> 3;
	});

	private final ReviewService reviewService;
	private final ThesisService thesisService;
	private final ResearchGroupGuidelinesRepository guidelinesRepository;
	private final AIReviewSummaryRepository reviewSummaryRepository;
	private final ThesisProposalRepository proposalRepository;
	private final ThesisFileRepository thesisFileRepository;

	/**
	 * Creates the AI feedback service.
	 *
	 * @param reviewService the pipeline that runs the LLM review over a PDF
	 * @param thesisService the service used to load documents and persist feedback
	 * @param guidelinesRepository the repository used to load and gate on per-group review guidelines
	 * @param reviewSummaryRepository the repository used to persist the latest score/assessment/summary
	 *                                per (thesis, review type)
	 * @param proposalRepository the repository used to re-check which proposal is currently the newest
	 * @param thesisFileRepository the repository used to re-check which thesis file is currently the newest
	 */
	public AIFeedbackService(
			ReviewService reviewService,
			ThesisService thesisService,
			ResearchGroupGuidelinesRepository guidelinesRepository,
			AIReviewSummaryRepository reviewSummaryRepository,
			ThesisProposalRepository proposalRepository,
			ThesisFileRepository thesisFileRepository) {
		this.reviewService = reviewService;
		this.thesisService = thesisService;
		this.guidelinesRepository = guidelinesRepository;
		this.reviewSummaryRepository = reviewSummaryRepository;
		this.proposalRepository = proposalRepository;
		this.thesisFileRepository = thesisFileRepository;
	}

	/**
	 * Runs the AI review pipeline and persists the findings on the thesis as feedback items
	 * with {@code generationSource = AI}. Access control is delegated to the caller (typically
	 * the controller, which enforces student/supervisor access based on the review type).
	 *
	 * @param thesis the thesis whose uploaded document is reviewed
	 * @param reviewType whether to review the proposal or the thesis document
	 * @return the updated thesis with the new feedback rows attached
	 */
	public Thesis autoReviewAndSave(Thesis thesis, ReviewType reviewType) {
		StructuredGuidelines guidelines = requireReadyGuidelines(thesis);
		ReviewDocument document = loadReviewDocument(thesis, reviewType);
		ReviewResultDTO result = reviewService.review(document.resource(), reviewType, guidelines);
		persistReviewSummary(thesis, reviewType, result, document.versionId());

		List<RequestChangesPayload.RequestedChange> changes = new ArrayList<>();
		for (FindingDTO finding : safeFindings(result.findings())) {
			changes.add(new RequestChangesPayload.RequestedChange(
					renderFeedbackText(finding),
					false,
					mapCategory(finding.category()),
					mapSeverity(finding.severity()),
					// Null → inherit the batch source (AI) passed to requestChanges below.
					null
			));
		}

		if (changes.isEmpty()) {
			log.info("AI auto review for thesis {} ({}) produced no actionable findings", thesis.getId(), reviewType);
			return thesis;
		}

		return thesisService.requestChanges(
				thesis,
				toFeedbackType(reviewType),
				changes,
				ThesisFeedbackSource.AI
		);
	}

	/**
	 * Runs the AI review pipeline and returns the findings as editable drafts without saving
	 * anything to the database. The instructor UI can then let the user tweak, accept, or drop
	 * individual entries before persisting them.
	 *
	 * <p>Deliberately does NOT call {@link #persistReviewSummary}: a preview is supervisor-only
	 * and provisional — the instructor may edit or discard every draft before saving anything.
	 * The persisted summary backs the "Overall Score" shown on the student-visible feedback
	 * overview, so persisting a discarded preview's score/summary there would leak content the
	 * instructor never approved.
	 *
	 * @param thesis the thesis whose uploaded document is reviewed
	 * @param reviewType whether to review the proposal or the thesis document
	 * @return the assessment, summary, and editable drafts
	 */
	public AIPreviewResponseDTO previewReview(Thesis thesis, ReviewType reviewType) {
		StructuredGuidelines guidelines = requireReadyGuidelines(thesis);
		ReviewDocument document = loadReviewDocument(thesis, reviewType);
		ReviewResultDTO result = reviewService.review(document.resource(), reviewType, guidelines);

		List<AIFeedbackDraftDTO> drafts = safeFindings(result.findings()).stream()
				.map(finding -> new AIFeedbackDraftDTO(
						renderFeedbackText(finding),
						mapCategory(finding.category()),
						mapSeverity(finding.severity())
				))
				.sorted(Comparator.comparing(
						AIFeedbackDraftDTO::severity,
						Comparator.nullsLast(SEVERITY_ORDER)
				))
				.toList();

		return new AIPreviewResponseDTO(result.category(), sanitizeScore(result.score()), result.summary(), drafts);
	}

	/**
	 * Upserts the {@code (thesis, reviewType)} row recording the AI review pipeline's latest
	 * score, assessment, and summary. Only called from {@link #autoReviewAndSave} — that flow
	 * always saves its findings as real, already-visible {@code ThesisFeedback} rows, so the
	 * summary describes content the student can already see. {@link #previewReview} must NOT call
	 * this (see its Javadoc).
	 *
	 * @param thesis the thesis that was reviewed
	 * @param reviewType whether the proposal or the thesis document was reviewed
	 * @param result the merged review result to persist
	 * @param documentVersionId the id of the document revision the review ran against, so the UI
	 *                          can tell a current summary from one describing an older upload
	 */
	private void persistReviewSummary(Thesis thesis, ReviewType reviewType, ReviewResultDTO result,
			UUID documentVersionId) {
		// A review takes long enough for the student to upload a new document while it runs. The
		// single (thesis, type) row would then be overwritten with a result about a superseded
		// revision — clobbering the summary of a review that already finished for the newer one.
		// Drop the stale result instead; the row stays on whatever revision is actually current.
		if (!isCurrentRevision(thesis, reviewType, documentVersionId)) {
			log.info("Discarding AI review summary for thesis {} ({}): reviewed revision {} is no longer current",
					thesis.getId(), reviewType, documentVersionId);
			return;
		}

		AIReviewSummary summary = reviewSummaryRepository
				.findByThesisIdAndType(thesis.getId(), reviewType)
				.orElseGet(AIReviewSummary::new);
		applyReviewResult(summary, thesis, reviewType, result, documentVersionId);

		try {
			reviewSummaryRepository.save(summary);
		} catch (DataIntegrityViolationException e) {
			// Two review runs for the same (thesis, type) raced: both found no existing row and
			// both tried to insert. Retry as an update against the row the other one just
			// created, so a concurrent request fails the review rather than this bookkeeping.
			AIReviewSummary existing = reviewSummaryRepository
					.findByThesisIdAndType(thesis.getId(), reviewType)
					.orElseThrow(() -> e);
			applyReviewResult(existing, thesis, reviewType, result, documentVersionId);
			reviewSummaryRepository.save(existing);
		}
	}

	private static void applyReviewResult(AIReviewSummary summary, Thesis thesis, ReviewType reviewType,
			ReviewResultDTO result, UUID documentVersionId) {
		summary.setThesis(thesis);
		summary.setType(reviewType);
		summary.setScore(sanitizeScore(result.score()));
		summary.setAssessment(result.category());
		summary.setSummary(result.summary());
		summary.setDocumentVersionId(documentVersionId);
	}

	/**
	 * Whether the revision a review ran against is still the thesis's newest upload.
	 *
	 * <p>Reads the current revision from the database rather than from {@code thesis}: that
	 * entity's proposal/file collections were initialised before the review started, so they
	 * cannot show an upload that landed while the (potentially minute-long) pipeline was running.
	 *
	 * @param thesis the thesis that was reviewed
	 * @param reviewType whether the proposal or the thesis document was reviewed
	 * @param documentVersionId the revision the review ran against
	 * @return {@code true} when that revision is still the newest one
	 */
	private boolean isCurrentRevision(Thesis thesis, ReviewType reviewType, UUID documentVersionId) {
		UUID currentVersionId = switch (reviewType) {
			case PROPOSAL -> proposalRepository.findFirstByThesisIdOrderByCreatedAtDesc(thesis.getId())
					.map(ThesisProposal::getId)
					.orElse(null);
			case THESIS -> thesisFileRepository
					.findFirstByThesisIdAndTypeOrderByUploadedAtDesc(thesis.getId(), "THESIS")
					.map(ThesisFile::getId)
					.orElse(null);
		};

		return currentVersionId != null && currentVersionId.equals(documentVersionId);
	}

	/**
	 * The score comes straight from the merger LLM's structured output — the prompt asks for an
	 * integer 0-100, but nothing enforces that at the schema level. Treat a missing or
	 * out-of-range value as "no score" rather than persisting or returning a bogus number.
	 *
	 * @param score the raw score reported by the review pipeline
	 * @return the score if it is a valid integer in [0, 100], otherwise {@code null}
	 */
	private static Integer sanitizeScore(Integer score) {
		if (score == null || score < 0 || score > 100) {
			return null;
		}
		return score;
	}

	/**
	 * The document a review run reads, paired with the id of the revision it came from. Reviews
	 * always run against the newest upload, and the id is what lets a persisted summary be
	 * recognised as stale once a newer proposal or thesis file replaces it.
	 *
	 * @param resource the PDF handed to the review pipeline
	 * @param versionId the {@code thesis_proposals} / {@code thesis_files} id of that revision
	 */
	private record ReviewDocument(Resource resource, UUID versionId) {
	}

	private ReviewDocument loadReviewDocument(Thesis thesis, ReviewType reviewType) {
		return switch (reviewType) {
			case PROPOSAL -> {
				List<ThesisProposal> proposals = thesis.getProposals();
				if (proposals == null || proposals.isEmpty()) {
					throw new ResourceInvalidParametersException(
							"Thesis has no uploaded proposal — cannot run an AI review.");
				}
				ThesisProposal proposal = proposals.getFirst();
				yield new ReviewDocument(thesisService.getProposalFile(proposal), proposal.getId());
			}
			case THESIS -> {
				ThesisFile thesisFile = thesis.getLatestFile("THESIS").orElseThrow(() ->
						new ResourceInvalidParametersException(
								"Thesis has no uploaded thesis document — cannot run an AI review."));
				yield new ReviewDocument(thesisService.getThesisFile(thesisFile), thesisFile.getId());
			}
		};
	}

	private static ThesisFeedbackType toFeedbackType(ReviewType reviewType) {
		return switch (reviewType) {
			case PROPOSAL -> ThesisFeedbackType.PROPOSAL;
			case THESIS -> ThesisFeedbackType.THESIS;
		};
	}

	private static List<FindingDTO> safeFindings(List<FindingDTO> findings) {
		return findings != null ? findings : List.of();
	}

	/**
	 * Collapses an AI finding into a single feedback string: title, description, then a
	 * parenthetical hint that surfaces the first location (page + section) so the student knows
	 * where to look. Additional locations are dropped — {@code ThesisFeedback.feedback} is a
	 * plain TEXT column and we don't want to explode it into JSON just for this.
	 *
	 * <p>The text is stored and rendered verbatim (the feedback overview and the request-changes
	 * dialog both show it as plain text), so no Markdown markup is added here.
	 */
	static String renderFeedbackText(FindingDTO finding) {
		StringBuilder sb = new StringBuilder();

		String title = finding.title();
		if (title != null && !title.isBlank()) {
			sb.append(title.strip());
		}

		String description = finding.description();
		if (description != null && !description.isBlank()) {
			if (!sb.isEmpty()) {
				sb.append(" — ");
			}
			sb.append(description.strip());
		}

		List<Location> locations = finding.locations();
		if (locations != null && !locations.isEmpty()) {
			Location first = locations.getFirst();
			String hint = formatLocationHint(first);
			if (!hint.isEmpty()) {
				if (!sb.isEmpty()) {
					sb.append(' ');
				}
				sb.append("(").append(hint).append(")");
			}
		}

		if (sb.isEmpty()) {
			// Defensive fallback: shouldn't happen since the LLM contract requires a title.
			return "AI feedback finding";
		}
		return sb.toString();
	}

	private static String formatLocationHint(Location location) {
		StringBuilder sb = new StringBuilder();
		if (location.page() != null) {
			sb.append("Page ").append(location.page());
		}
		if (location.section() != null && !location.section().isBlank()) {
			if (!sb.isEmpty()) {
				sb.append(", ");
			}
			sb.append(location.section().strip());
		}
		return sb.toString();
	}

	private static ThesisFeedbackCategory mapCategory(String category) {
		if (category == null || category.isBlank()) {
			return null;
		}
		try {
			return ThesisFeedbackCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			// LLM emitted a category outside our enum — record as OTHER instead of failing the request.
			log.warn("Unknown AI category '{}' — mapping to OTHER", category);
			return ThesisFeedbackCategory.OTHER;
		}
	}

	private static ThesisFeedbackSeverity mapSeverity(String severity) {
		if (severity == null || severity.isBlank()) {
			return null;
		}
		try {
			return ThesisFeedbackSeverity.valueOf(severity.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			log.warn("Unknown AI severity '{}' — mapping to MINOR", severity);
			return ThesisFeedbackSeverity.MINOR;
		}
	}

	/**
	 * Throws if the thesis does not have an uploaded document for the given review type. Kept
	 * public because the controller wants to check before doing the work.
	 *
	 * @param thesis the thesis whose uploaded document is required
	 * @param reviewType whether the proposal or the thesis document is required
	 */
	public void assertHasDocument(Thesis thesis, ReviewType reviewType) {
		loadReviewDocument(thesis, reviewType);
	}

	/**
	 * Resolves the thesis's research group guidelines and requires them to be
	 * {@code READY} before any AI review may run. This is the per-group gate: members of a
	 * research group whose lead has not (successfully) uploaded guidelines cannot use the AI
	 * features. Throws {@link AccessDeniedException} otherwise.
	 *
	 * @param thesis the thesis being reviewed
	 * @return the group's structured guidelines
	 */
	private StructuredGuidelines requireReadyGuidelines(Thesis thesis) {
		ResearchGroup researchGroup = thesis.getResearchGroup();
		if (researchGroup == null || researchGroup.getId() == null) {
			throw new AccessDeniedException(
					"AI review is not available for this thesis because it is not assigned to a research group. "
							+ "Please ask your examiner or supervisor to assign the thesis to a research group.");
		}

		ResearchGroupGuidelines guidelines = guidelinesRepository.findById(researchGroup.getId()).orElse(null);
		if (guidelines == null) {
			throw new AccessDeniedException(GUIDELINES_MISSING_MESSAGE);
		}
		if (!guidelines.isReady() || guidelines.getStructuredGuidelines() == null) {
			throw new AccessDeniedException(GUIDELINES_NOT_READY_MESSAGE);
		}

		return guidelines.getStructuredGuidelines();
	}
}
