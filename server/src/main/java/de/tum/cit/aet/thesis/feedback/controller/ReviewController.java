package de.tum.cit.aet.thesis.feedback.controller;

import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.AIReviewRequestDTO;
import de.tum.cit.aet.thesis.feedback.service.AIFeedbackService;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import de.tum.cit.aet.thesis.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.thesis.dto.ThesisDto;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.service.ThesisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** REST controller for AI generated feedback. */
@Slf4j
@RestController
@RequestMapping("/v2/ai-review")
@Conditional(AIFeaturesEnabled.class)
public class ReviewController {
	private final AIFeedbackService aiFeedbackService;
	private final ThesisService thesisService;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	/**
	 * Creates the AI review controller.
	 *
	 * @param aiFeedbackService the service that runs the AI review pipeline and persists findings
	 * @param thesisService the service used to load the target thesis
	 * @param currentUserProviderProvider the provider for the current authenticated user
	 */
	public ReviewController(
			AIFeedbackService aiFeedbackService,
			ThesisService thesisService,
			ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
		this.aiFeedbackService = aiFeedbackService;
		this.thesisService = thesisService;
		this.currentUserProviderProvider = currentUserProviderProvider;
	}

	/**
	 * Student-facing auto endpoint: runs the AI review pipeline against the thesis's already
	 * uploaded proposal or thesis PDF and persists each finding as a {@code ThesisFeedback}
	 * row with {@code generationSource = AI}. Returns the refreshed thesis so the client can
	 * re-render the feedback list.
	 *
	 * @param request the thesis id and review type to run
	 * @return the refreshed thesis with the newly persisted AI feedback
	 */
	@PostMapping("auto")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ThesisDto> autoReview(@Valid @RequestBody AIReviewRequestDTO request) {
		User currentUser = currentUserProvider().getUser();
		Thesis thesis = thesisService.findById(request.thesisId());

		// Both the student on the thesis and any supervisor may trigger the auto flow.
		boolean supervisorAccess = thesis.hasSupervisorAccess(currentUser);
		if (!thesis.hasStudentAccess(currentUser) && !supervisorAccess) {
			throw new AccessDeniedException(
					"You must be a student or supervisor on the thesis to run an AI review.");
		}

		// Students may only run the review that matches the thesis's current phase: a PROPOSAL
		// review during the proposal phase, a THESIS review during writing. Supervisors keep an
		// explicit override so they can, e.g., re-run a proposal review after writing has begun.
		if (!supervisorAccess) {
			assertReviewTypeMatchesPhase(thesis, request.reviewType());
		}

		aiFeedbackService.assertHasDocument(thesis, request.reviewType());
		Thesis updated = aiFeedbackService.autoReviewAndSave(thesis, request.reviewType());

		return ResponseEntity.ok(ThesisDto.fromThesisEntity(
				updated,
				updated.hasSupervisorAccess(currentUser),
				updated.hasStudentAccess(currentUser)));
	}

	/**
	 * Instructor-facing preview endpoint: runs the AI review pipeline and returns editable
	 * drafts without saving anything. The instructor UI appends these to its unsaved batch so
	 * the instructor can edit, delete, or accept each item before committing.
	 *
	 * @param request the thesis id and review type to run
	 * @return the assessment, summary, and editable drafts
	 */
	@PostMapping("preview")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<AIPreviewResponseDTO> previewReview(@Valid @RequestBody AIReviewRequestDTO request) {
		User currentUser = currentUserProvider().getUser();
		Thesis thesis = thesisService.findById(request.thesisId());

		if (!thesis.hasSupervisorAccess(currentUser)) {
			throw new AccessDeniedException(
					"You must be a supervisor on the thesis to preview AI feedback.");
		}

		AIPreviewResponseDTO response = aiFeedbackService.previewReview(thesis, request.reviewType());
		return ResponseEntity.ok(response);
	}

	/**
	 * Rejects a student-triggered review whose type does not match the thesis's current phase.
	 * The proposal phase only accepts {@link ReviewType#PROPOSAL} reviews and the writing phase
	 * only {@link ReviewType#THESIS} reviews; every other state has no student review at all.
	 */
	private static void assertReviewTypeMatchesPhase(Thesis thesis, ReviewType reviewType) {
		ThesisState state = thesis.getState();
		ReviewType allowed = switch (state) {
			case PROPOSAL -> ReviewType.PROPOSAL;
			case WRITING -> ReviewType.THESIS;
			default -> null;
		};

		if (allowed == null) {
			throw new ResourceInvalidParametersException(
					"AI reviews can only be run while the thesis is in the proposal or writing phase.");
		}

		if (reviewType != allowed) {
			throw new ResourceInvalidParametersException(
					"A " + reviewType + " review cannot be run while the thesis is in the "
							+ state + " phase.");
		}
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}
}
