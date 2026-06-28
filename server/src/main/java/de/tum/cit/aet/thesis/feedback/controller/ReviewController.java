package de.tum.cit.aet.thesis.feedback.controller;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.ProviderCategory;
import de.tum.cit.aet.thesis.feedback.dto.ReviewRequestDTO;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** REST controller for AI generated feedback. */
@Slf4j
@RestController
@RequestMapping("/v2/ai-review")
@Conditional(AIFeaturesEnabled.class)
public class ReviewController {
	private final ReviewService reviewService;
	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PostMapping(value = "review-proposal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<ReviewResultDTO> reviewProposal(@ModelAttribute ReviewRequestDTO request) {
		// TODO: Use already uploaded file from the thesis service instead of uploading it again

		if (request.providerCategory().equals(ProviderCategory.AZURE)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Azure provider is not supported yet.");
		}

		ReviewResultDTO reviewResult = reviewService.review(request);
		return ResponseEntity.ok().body(reviewResult);
	}
}
