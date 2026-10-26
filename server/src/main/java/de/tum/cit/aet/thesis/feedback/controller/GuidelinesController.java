package de.tum.cit.aet.thesis.feedback.controller;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.controller.payload.UpdateGuidelinesPayload;
import de.tum.cit.aet.thesis.feedback.controller.payload.UpdateStructuredGuidelinesPayload;
import de.tum.cit.aet.thesis.feedback.dto.GuidelinesDTO;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.service.GuidelinesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller letting a research group lead read and set the group's custom AI review
 * guidelines. Role is checked coarsely via {@code @PreAuthorize}; the service additionally
 * enforces that the caller manages the target research group.
 */
@Slf4j
@RestController
@RequestMapping("/v2/ai-review/guidelines")
@Conditional(AIFeaturesEnabled.class)
public class GuidelinesController {
	private final GuidelinesService guidelinesService;

	/**
	 * Creates the controller.
	 *
	 * @param guidelinesService service handling guideline reads and updates
	 */
	public GuidelinesController(GuidelinesService guidelinesService) {
		this.guidelinesService = guidelinesService;
	}

	/**
	 * Returns the current guidelines for a research group, or an empty object when the lead has
	 * not set any yet.
	 *
	 * @param researchGroupId the research group id
	 * @return the guidelines, or an empty representation
	 */
	@GetMapping("/{researchGroupId}")
	@PreAuthorize("hasAnyRole('admin', 'group-admin')")
	public ResponseEntity<GuidelinesDTO> getGuidelines(@PathVariable UUID researchGroupId) {
		Optional<ResearchGroupGuidelines> guidelines = guidelinesService.getByResearchGroupId(researchGroupId);
		return ResponseEntity.ok(guidelines.map(GuidelinesDTO::fromEntity).orElseGet(GuidelinesDTO::empty));
	}

	/**
	 * Sets or replaces the guidelines for a research group. The raw text is preprocessed into the
	 * fixed review categories; the response carries the resulting status (ready or failed) so the
	 * client can surface either the distilled rules or the reason the input was rejected.
	 *
	 * @param researchGroupId the research group id
	 * @param payload         the raw guidelines to store
	 * @return the persisted guidelines
	 */
	@PutMapping("/{researchGroupId}")
	@PreAuthorize("hasAnyRole('admin', 'group-admin')")
	public ResponseEntity<GuidelinesDTO> updateGuidelines(
			@PathVariable UUID researchGroupId,
			@Valid @RequestBody UpdateGuidelinesPayload payload) {
		ResearchGroupGuidelines saved = guidelinesService.updateGuidelines(researchGroupId, payload.rawGuidelines());
		return ResponseEntity.ok(GuidelinesDTO.fromEntity(saved));
	}

	/**
	 * Manually refines the already-generated per-category rules without re-running the LLM
	 * preprocessor — the lead's post-processing path for tweaking wording or adding a new
	 * convention. Requires guidelines to already exist for the group; an edit that leaves no usable
	 * rule for any recognized category is rejected with {@code 400}.
	 *
	 * @param researchGroupId the research group id
	 * @param payload         the edited overview and per-category rules
	 * @return the persisted guidelines
	 */
	@PutMapping("/{researchGroupId}/rules")
	@PreAuthorize("hasAnyRole('admin', 'group-admin')")
	public ResponseEntity<GuidelinesDTO> updateStructuredGuidelines(
			@PathVariable UUID researchGroupId,
			@Valid @RequestBody UpdateStructuredGuidelinesPayload payload) {
		List<CategoryGuidelines> categories = payload.categories().stream()
				.map(category -> new CategoryGuidelines(category.category(), category.rules()))
				.toList();
		ResearchGroupGuidelines saved = guidelinesService.updateStructuredGuidelines(
				researchGroupId, payload.overview(), categories);
		return ResponseEntity.ok(GuidelinesDTO.fromEntity(saved));
	}
}
