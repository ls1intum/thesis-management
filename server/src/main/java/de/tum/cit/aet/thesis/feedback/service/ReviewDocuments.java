package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.proposal.entity.ThesisProposal;
import de.tum.cit.aet.thesis.proposal.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.entity.ThesisFile;
import de.tum.cit.aet.thesis.thesis.repository.ThesisFileRepository;
import de.tum.cit.aet.thesis.thesis.service.ThesisService;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Resolves which uploaded document a review runs against, and tells whether a revision is still
 * the current one. Reviews always target the newest upload of the requested type.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class ReviewDocuments {

	/** The type key under which a thesis document (as opposed to any other attachment) is stored. */
	private static final String THESIS_FILE_TYPE = "THESIS";

	private final ThesisService thesisService;
	private final ThesisProposalRepository proposalRepository;
	private final ThesisFileRepository thesisFileRepository;

	public ReviewDocuments(ThesisService thesisService, ThesisProposalRepository proposalRepository,
			ThesisFileRepository thesisFileRepository) {
		this.thesisService = thesisService;
		this.proposalRepository = proposalRepository;
		this.thesisFileRepository = thesisFileRepository;
	}

	/**
	 * The document a review run reads, paired with the id of the revision it came from. The id is
	 * what lets a persisted summary be recognised as stale once a newer upload replaces it.
	 *
	 * @param resource  the PDF handed to the reviewer
	 * @param versionId the {@code thesis_proposals} / {@code thesis_files} id of that revision
	 */
	public record ReviewDocument(Resource resource, UUID versionId) {
	}

	/**
	 * Loads the newest uploaded document of the requested type.
	 *
	 * @param thesis     the thesis whose upload is reviewed
	 * @param reviewType whether the proposal or the thesis document is wanted
	 * @return the document and its revision id
	 * @throws ResourceInvalidParametersException if no such document has been uploaded
	 */
	public ReviewDocument load(Thesis thesis, ReviewType reviewType) {
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
				ThesisFile file = thesis.getLatestFile(THESIS_FILE_TYPE).orElseThrow(() ->
						new ResourceInvalidParametersException(
								"Thesis has no uploaded thesis document — cannot run an AI review."));
				yield new ReviewDocument(thesisService.getThesisFile(file), file.getId());
			}
		};
	}

	/**
	 * Whether the revision a review ran against is still the thesis's newest upload.
	 *
	 * <p>Reads the current revision from the database rather than from {@code thesis}: that entity's
	 * proposal/file collections were initialised before the review started, so they cannot show an
	 * upload that landed while the (potentially minute-long) pipeline was running.
	 *
	 * @param thesisId          the thesis that was reviewed
	 * @param reviewType        whether the proposal or the thesis document was reviewed
	 * @param documentVersionId the revision the review ran against
	 * @return {@code true} when that revision is still the newest one
	 */
	public boolean isCurrentRevision(UUID thesisId, ReviewType reviewType, UUID documentVersionId) {
		UUID currentVersionId = switch (reviewType) {
			case PROPOSAL -> proposalRepository.findFirstByThesisIdOrderByCreatedAtDesc(thesisId)
					.map(ThesisProposal::getId)
					.orElse(null);
			case THESIS -> thesisFileRepository
					.findFirstByThesisIdAndTypeOrderByUploadedAtDesc(thesisId, THESIS_FILE_TYPE)
					.map(ThesisFile::getId)
					.orElse(null);
		};

		return currentVersionId != null && currentVersionId.equals(documentVersionId);
	}
}
