package de.tum.cit.aet.thesis.feedback.controller;

import de.tum.cit.aet.thesis.core.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.AIReviewRequestDTO;
import de.tum.cit.aet.thesis.feedback.service.AIFeedbackService;
import de.tum.cit.aet.thesis.thesis.dto.ThesisDto;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.service.ThesisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
	 */
	@PostMapping("auto")
	public ResponseEntity<ThesisDto> autoReview(@Valid @RequestBody AIReviewRequestDTO request) {
		User currentUser = currentUserProvider().getUser();
		Thesis thesis = thesisService.findById(request.thesisId());

		// Both the student on the thesis and any supervisor may trigger the auto flow.
		if (!thesis.hasStudentAccess(currentUser) && !thesis.hasSupervisorAccess(currentUser)) {
			throw new AccessDeniedException(
					"You must be a student or supervisor on the thesis to run an AI review.");
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
	 */
	@PostMapping("preview")
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

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}
}
