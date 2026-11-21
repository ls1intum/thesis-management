package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.core.group.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.core.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.GuidelinesPreprocessingResult;
import de.tum.cit.aet.thesis.feedback.entity.GuidelinesStatus;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.repository.ResearchGroupGuidelinesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

	private static final String NO_USABLE_RULES_REASON =
			"The guidelines could not be distilled into specific, actionable review rules. "
					+ "Please provide concrete, actionable guidance.";

	private final ResearchGroupGuidelinesRepository guidelinesRepository;
	private final ResearchGroupRepository researchGroupRepository;
	private final GuidelinesPreprocessor preprocessor;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	/**
	 * Creates the service.
	 *
	 * @param guidelinesRepository           repository for the stored per-group guidelines
	 * @param researchGroupRepository        repository used to resolve the target research group
	 * @param preprocessor                   distills raw guideline text into structured rules
	 * @param currentUserProviderProvider    lazy provider for the request-scoped current user
	 */
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

		// The model's output is untrusted: it can contain unknown or null category slugs, blank
		// rules, or several entries for the same category. Canonicalize it first so validation and
		// persistence see the same normalized form -- storing the raw output would leave duplicate
		// entries whose rules StructuredGuidelines#rulesForCategory silently drops.
		List<CategoryGuidelines> sanitized = sanitizeCategories(
				result != null ? result.categories() : null);

		// The model can also return specific=true while producing an empty category array, unknown
		// category slugs, or only blank rules. Persisting that as READY would unlock the AI
		// features for the group with no actual group-specific guidance, defeating the gate. Only
		// mark READY when at least one recognized category carries a nonblank rule.
		if (result != null && result.specific() && !sanitized.isEmpty()) {
			entity.setStatus(GuidelinesStatus.READY);
			entity.setStructuredGuidelines(new StructuredGuidelines(result.overview(), sanitized));
			entity.setFailureReason(null);
			entity.setProcessedAt(Instant.now());
			log.info("Stored READY guidelines for research group {}", researchGroupId);
		} else {
			entity.setStatus(GuidelinesStatus.FAILED);
			entity.setStructuredGuidelines(null);
			entity.setFailureReason(resolveFailureReason(result));
			entity.setProcessedAt(null);
			log.info("Stored FAILED guidelines for research group {}: {}", researchGroupId, entity.getFailureReason());
		}

		return guidelinesRepository.save(entity);
	}

	/**
	 * Manually replaces a research group's structured guidelines without re-running the
	 * preprocessor. This is the post-processing path: after the automatic distribution has produced
	 * a first draft, the lead can tweak wording, add rules for a new convention, or drop ones that
	 * no longer apply. The raw text is left untouched.
	 *
	 * <p>Edits that would leave no usable, category-specific guidance are rejected (the existing
	 * {@link GuidelinesStatus#READY} state is preserved) rather than silently downgrading the group
	 * out of the AI features. On success the record is (re)marked {@link GuidelinesStatus#READY}.
	 *
	 * @param researchGroupId the research group id
	 * @param overview        the edited category-independent overview (blank clears it)
	 * @param categories      the edited per-category rules
	 * @return the persisted guidelines
	 * @throws ResourceNotFoundException          if the group has no guidelines to edit yet
	 * @throws ResourceInvalidParametersException if the edit leaves no recognized category with a
	 *                                            nonblank rule
	 */
	public ResearchGroupGuidelines updateStructuredGuidelines(
			UUID researchGroupId, String overview, List<CategoryGuidelines> categories) {
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
				.orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		ResearchGroupGuidelines entity = guidelinesRepository.findById(researchGroupId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"This research group has no guidelines to edit yet. Generate them first."));

		List<CategoryGuidelines> sanitized = sanitizeCategories(categories);
		if (sanitized.isEmpty()) {
			throw new ResourceInvalidParametersException(
					"The guidelines must keep at least one rule for a recognized review category.");
		}

		String cleanedOverview = overview != null && !overview.isBlank() ? overview.strip() : null;
		entity.setStructuredGuidelines(new StructuredGuidelines(cleanedOverview, sanitized));
		entity.setStatus(GuidelinesStatus.READY);
		entity.setFailureReason(null);
		entity.setProcessedAt(Instant.now());
		entity.setUpdatedBy(currentUserProvider().getUser());
		log.info("Stored manually edited guidelines for research group {} ({} categories)",
				researchGroupId, sanitized.size());

		return guidelinesRepository.save(entity);
	}

	/**
	 * Normalizes categories from either the preprocessor or a manual edit: keeps only recognized
	 * {@link ReviewCategory} slugs, strips and drops blank rules, merges repeated entries for the
	 * same category into one, drops exact duplicate rules, and drops categories left with no rules.
	 * Guards the stored representation against unknown or null slugs, whitespace-only rules and
	 * duplicate entries regardless of what the client or the model sends.
	 *
	 * <p>Merging matters because {@link StructuredGuidelines#rulesForCategory} resolves a slug to
	 * the first matching entry, so a second entry for the same category would never be read.
	 *
	 * @param categories the raw categories, may be {@code null} or contain {@code null} entries
	 * @return the cleaned categories, one entry per non-empty recognized category
	 */
	private static List<CategoryGuidelines> sanitizeCategories(List<CategoryGuidelines> categories) {
		if (categories == null) {
			return List.of();
		}
		// Null slugs must be screened out before the lookup: ReviewCategory.SLUGS is an immutable
		// set, whose contains(null) throws rather than returning false.
		Map<String, Set<String>> rulesBySlug = new LinkedHashMap<>();
		for (CategoryGuidelines category : categories) {
			if (category == null
					|| category.category() == null
					|| !ReviewCategory.SLUGS.contains(category.category())
					|| category.rules() == null) {
				continue;
			}
			for (String rule : category.rules()) {
				if (rule != null && !rule.isBlank()) {
					rulesBySlug.computeIfAbsent(category.category(), slug -> new LinkedHashSet<>())
							.add(rule.strip());
				}
			}
		}
		return rulesBySlug.entrySet().stream()
				.map(entry -> new CategoryGuidelines(entry.getKey(), List.copyOf(entry.getValue())))
				.toList();
	}

	private static String resolveFailureReason(GuidelinesPreprocessingResult result) {
		if (result != null && result.reason() != null && !result.reason().isBlank()) {
			return result.reason().strip();
		}
		// specific=true but no usable rules survived validation: give a distinct, actionable reason.
		if (result != null && result.specific()) {
			return NO_USABLE_RULES_REASON;
		}
		return "The guidelines are too vague to build specific review rules. "
				+ "Please provide concrete, actionable guidance.";
	}
}
