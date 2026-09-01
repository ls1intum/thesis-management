package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.core.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.repository.ResearchGroupGuidelinesRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * The per-research-group gate on the AI review features: members of a group whose lead has not
 * successfully uploaded guidelines cannot run a review. Separate from {@link GuidelinesService},
 * which is the lead's CRUD path and asserts the caller manages the group — here the caller is
 * typically the student, who may read their group's guidelines only through this narrow door.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class GuidelinesGate {

	/**
	 * Shown when the thesis's research group has never configured AI review guidelines. Students
	 * cannot fix this themselves, so the message names who can and where.
	 */
	private static final String MISSING =
			"AI review is not set up for your research group yet. Please ask your examiner or your research "
					+ "group lead to add the group's writing guidelines under Research Group Settings → AI Review "
					+ "Guidelines. Once they are saved, AI feedback becomes available for everyone in the group.";

	/**
	 * Shown when guidelines exist but preprocessing rejected them as too vague (or they are still
	 * being processed) — a different fix than the missing case, so it gets its own wording.
	 */
	private static final String NOT_READY =
			"AI review is not available yet because your research group's writing guidelines could not be turned "
					+ "into review rules. Please ask your examiner or your research group lead to revise them under "
					+ "Research Group Settings → AI Review Guidelines.";

	private static final String NO_GROUP =
			"AI review is not available for this thesis because it is not assigned to a research group. "
					+ "Please ask your examiner or supervisor to assign the thesis to a research group.";

	private final ResearchGroupGuidelinesRepository guidelinesRepository;

	/**
	 * Creates the gate.
	 *
	 * @param guidelinesRepository repository for the stored per-group guidelines
	 */
	public GuidelinesGate(ResearchGroupGuidelinesRepository guidelinesRepository) {
		this.guidelinesRepository = guidelinesRepository;
	}

	/**
	 * Resolves the group's guidelines and requires them to be ready before any review may run.
	 *
	 * @param researchGroup the research group the reviewed thesis belongs to; may be {@code null}
	 * @return the group's structured guidelines
	 * @throws AccessDeniedException if the thesis has no group, or the group has no ready guidelines
	 */
	public StructuredGuidelines requireReady(ResearchGroup researchGroup) {
		if (researchGroup == null || researchGroup.getId() == null) {
			throw new AccessDeniedException(NO_GROUP);
		}

		ResearchGroupGuidelines guidelines = guidelinesRepository.findById(researchGroup.getId())
				.orElseThrow(() -> new AccessDeniedException(MISSING));
		if (!guidelines.isReady() || guidelines.getStructuredGuidelines() == null) {
			throw new AccessDeniedException(NOT_READY);
		}

		return guidelines.getStructuredGuidelines();
	}
}
