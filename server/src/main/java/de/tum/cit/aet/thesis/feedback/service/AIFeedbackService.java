package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.FindingDTO;
import de.tum.cit.aet.thesis.feedback.dto.Location;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import de.tum.cit.aet.thesis.proposal.entity.ThesisProposal;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSource;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.thesis.controller.payload.RequestChangesPayload;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.entity.ThesisFile;
import de.tum.cit.aet.thesis.thesis.service.ThesisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Runs the AI review pipeline against a thesis's existing uploaded PDF and either (a) persists
 * the findings as {@code ThesisFeedback} rows (auto flow, student-driven) or (b) shapes them
 * as editable drafts for an instructor preview (preview flow).
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class AIFeedbackService {
	private static final Logger log = LoggerFactory.getLogger(AIFeedbackService.class);

	/** Severity levels sorted from most to least urgent for stable preview ordering. */
	private static final Comparator<ThesisFeedbackSeverity> SEVERITY_ORDER = Comparator.comparingInt(sev -> switch (sev) {
		case CRITICAL -> 0;
		case MAJOR -> 1;
		case MINOR -> 2;
		case SUGGESTION -> 3;
	});

	private final ReviewService reviewService;
	private final ThesisService thesisService;

	/**
	 * Creates the AI feedback service.
	 *
	 * @param reviewService the pipeline that runs the LLM review over a PDF
	 * @param thesisService the service used to load documents and persist feedback
	 */
	public AIFeedbackService(ReviewService reviewService, ThesisService thesisService) {
		this.reviewService = reviewService;
		this.thesisService = thesisService;
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
		Resource pdfResource = loadPdfResource(thesis, reviewType);
		ReviewResultDTO result = reviewService.review(pdfResource, reviewType);

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
	 * @param thesis the thesis whose uploaded document is reviewed
	 * @param reviewType whether to review the proposal or the thesis document
	 * @return the assessment, summary, and editable drafts
	 */
	public AIPreviewResponseDTO previewReview(Thesis thesis, ReviewType reviewType) {
		Resource pdfResource = loadPdfResource(thesis, reviewType);
		ReviewResultDTO result = reviewService.review(pdfResource, reviewType);

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

		return new AIPreviewResponseDTO(result.category(), result.summary(), drafts);
	}

	private Resource loadPdfResource(Thesis thesis, ReviewType reviewType) {
		return switch (reviewType) {
			case PROPOSAL -> {
				List<ThesisProposal> proposals = thesis.getProposals();
				if (proposals == null || proposals.isEmpty()) {
					throw new ResourceInvalidParametersException(
							"Thesis has no uploaded proposal — cannot run an AI review.");
				}
				yield thesisService.getProposalFile(proposals.getFirst());
			}
			case THESIS -> {
				ThesisFile thesisFile = thesis.getLatestFile("THESIS").orElseThrow(() ->
						new ResourceInvalidParametersException(
								"Thesis has no uploaded thesis document — cannot run an AI review."));
				yield thesisService.getThesisFile(thesisFile);
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
		loadPdfResource(thesis, reviewType);
	}
}
