package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.core.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.core.group.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.core.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.GuidelinesPreprocessingResult;
import de.tum.cit.aet.thesis.feedback.entity.GuidelinesStatus;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.repository.ResearchGroupGuidelinesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages a research group's custom AI review guidelines: lets the group lead read and replace
 * them, running the free-text input through {@link GuidelinesPreprocessor} to produce the
 * structured, per-category representation the review pipeline consumes.
 *
 * <p>Only {@link GuidelinesStatus#READY} guidelines unlock the AI features for the group. When
 * preprocessing decides the input is too vague, the record is stored as
 * {@link GuidelinesStatus#FAILED} (preserving the lead's text and the reason) and the group's
 * members remain gated out of AI features.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class GuidelinesService {
	private static final Logger log = LoggerFactory.getLogger(GuidelinesService.class);

	private final ResearchGroupGuidelinesRepository guidelinesRepository;
	private final ResearchGroupRepository researchGroupRepository;
	private final GuidelinesPreprocessor preprocessor;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	public GuidelinesService(
			ResearchGroupGuidelinesRepository guidelinesRepository,
			ResearchGroupRepository researchGroupRepository,
			GuidelinesPreprocessor preprocessor,
			ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
		this.guidelinesRepository = guidelinesRepository;
		this.researchGroupRepository = researchGroupRepository;
		this.preprocessor = preprocessor;
		this.currentUserProviderProvider = currentUserProviderProvider;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Returns the guidelines for a research group, verifying the current user's access.
	 *
	 * @param researchGroupId the research group id
	 * @return the guidelines, or empty if the lead has not set any yet
	 */
	public Optional<ResearchGroupGuidelines> getByResearchGroupId(UUID researchGroupId) {
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
				.orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);
		return guidelinesRepository.findById(researchGroupId);
	}

	/**
	 * Sets or replaces a research group's guidelines. Verifies the current user's access, runs the
	 * raw text through the preprocessor, and persists the result: {@link GuidelinesStatus#READY}
	 * with the distilled structured guidelines when the input is specific enough, otherwise
	 * {@link GuidelinesStatus#FAILED} with the reason (so members stay gated out of AI features).
	 *
	 * @param researchGroupId the research group id
	 * @param rawGuidelines   the lead's free-text guidelines
	 * @return the persisted guidelines
	 */
	public ResearchGroupGuidelines updateGuidelines(UUID researchGroupId, String rawGuidelines) {
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
				.orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		GuidelinesPreprocessingResult result = preprocessor.preprocess(rawGuidelines);

		ResearchGroupGuidelines entity = guidelinesRepository.findById(researchGroupId)
				.orElseGet(ResearchGroupGuidelines::new);
		entity.setResearchGroupId(researchGroupId);
		entity.setRawGuidelines(rawGuidelines);
		entity.setUpdatedBy(currentUserProvider().getUser());

		if (result != null && result.specific()) {
			entity.setStatus(GuidelinesStatus.READY);
			entity.setStructuredGuidelines(new StructuredGuidelines(
					result.overview(),
					result.categories() != null ? result.categories() : List.of()));
			entity.setFailureReason(null);
			entity.setProcessedAt(Instant.now());
			log.info("Stored READY guidelines for research group {}", researchGroupId);
		} else {
			entity.setStatus(GuidelinesStatus.FAILED);
			entity.setStructuredGuidelines(null);
			entity.setFailureReason(result != null && result.reason() != null && !result.reason().isBlank()
					? result.reason().strip()
					: "The guidelines are too vague to build specific review rules. Please provide concrete, actionable guidance.");
			entity.setProcessedAt(null);
			log.info("Stored FAILED guidelines for research group {}: {}", researchGroupId, entity.getFailureReason());
		}

		return guidelinesRepository.save(entity);
	}
}
