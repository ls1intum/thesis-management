package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisComment;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ThesisCommentRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

/** Manages thesis comments, including creation, retrieval, file handling, and deletion. */
@Service
public class ThesisCommentService {
	private final ThesisCommentRepository thesisCommentRepository;
	private final UploadService uploadService;
	private final MailingService mailingService;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	/**
	 * Injects the comment repository, upload service, mailing service, and current user provider.
	 *
	 * @param thesisCommentRepository the thesis comment repository
	 * @param uploadService the upload service
	 * @param mailingService the mailing service
	 * @param currentUserProviderProvider the current user provider
	 */
	public ThesisCommentService(ThesisCommentRepository thesisCommentRepository, UploadService uploadService, MailingService mailingService,
								ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
		this.thesisCommentRepository = thesisCommentRepository;
		this.uploadService = uploadService;
		this.mailingService = mailingService;
		this.currentUserProviderProvider = currentUserProviderProvider;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Returns a paginated list of comments for the given thesis filtered by comment type.
	 *
	 * @param thesis the thesis to retrieve comments for
	 * @param commentType the type of comments to filter by
	 * @param page the page number
	 * @param limit the number of comments per page
	 * @return the paginated list of thesis comments
	 */
	public Page<ThesisComment> getComments(Thesis thesis, ThesisCommentType commentType, Integer page, Integer limit) {
		currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
		return thesisCommentRepository.searchComments(
				thesis.getId(),
				commentType,
				PageRequest.of(page, limit)
		);
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public ThesisComment postComment(Thesis thesis, ThesisCommentType commentType, String message, MultipartFile file) {
		currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
		ThesisComment comment = new ThesisComment();

		comment.setType(commentType);
		comment.setThesis(thesis);
		comment.setMessage(message);
		comment.setCreatedAt(Instant.now());
		comment.setCreatedBy(currentUserProvider().getUser());

		if (file != null) {
			comment.setUploadName(file.getOriginalFilename());
			comment.setFilename(uploadService.store(file, 25 * 1024 * 1024, UploadFileType.ANY));
		}

		comment = thesisCommentRepository.save(comment);

		mailingService.sendNewCommentEmail(comment);

		return comment;
	}

	/**
	 * Loads and returns the file attachment associated with the given comment.
	 *
	 * @param comment the thesis comment containing the file
	 * @return the file resource
	 */
	public Resource getCommentFile(ThesisComment comment) {
		currentUserProvider().assertCanAccessResearchGroup(comment.getResearchGroup());
		return uploadService.load(comment.getFilename());
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public ThesisComment deleteComment(ThesisComment comment) {
		currentUserProvider().assertCanAccessResearchGroup(comment.getResearchGroup());
		thesisCommentRepository.deleteById(comment.getId());

		return comment;
	}

	/**
	 * Finds a comment by its ID and verifies it belongs to the specified thesis.
	 *
	 * @param thesisId the ID of the thesis
	 * @param commentId the ID of the comment
	 * @return the found thesis comment
	 */
	public ThesisComment findById(UUID thesisId, UUID commentId) {
		ThesisComment comment = thesisCommentRepository.findById(commentId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("Comment with id %s not found", commentId)));
		Thesis thesis = comment.getThesis();
		currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());

		if (!thesis.getId().equals(thesisId)) {
			throw new ResourceNotFoundException(String.format("Comment does not belong to thesis id %s", thesisId));
		}

		return comment;
	}
}
