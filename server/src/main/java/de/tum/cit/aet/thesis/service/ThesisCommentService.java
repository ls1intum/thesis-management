package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisComment;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ThesisCommentRepository;

import java.time.Instant;
import java.util.UUID;

@Service
public class ThesisCommentService {
    private final ThesisCommentRepository thesisCommentRepository;
    private final UploadService uploadService;
    private final MailingService mailingService;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

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

    public Page<ThesisComment> getComments(Thesis thesis, ThesisCommentType commentType, Integer page, Integer limit) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        return thesisCommentRepository.searchComments(
                thesis.getId(),
                commentType,
                PageRequest.of(page, limit)
        );
    }

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
            comment.setFilename(uploadService.store(file, 20 * 1024 * 1024, UploadFileType.ANY));
        }

        comment = thesisCommentRepository.save(comment);

        mailingService.sendNewCommentEmail(comment);

        return comment;
    }

    public Resource getCommentFile(ThesisComment comment) {
        currentUserProvider().assertCanAccessResearchGroup(comment.getResearchGroup());
        return uploadService.load(comment.getFilename());
    }

    @Transactional
    public ThesisComment deleteComment(ThesisComment comment) {
        currentUserProvider().assertCanAccessResearchGroup(comment.getResearchGroup());
        thesisCommentRepository.deleteById(comment.getId());

        return comment;
    }

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
