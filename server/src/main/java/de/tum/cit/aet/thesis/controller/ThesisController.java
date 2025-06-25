package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.StringLimits;
import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationState;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.controller.payload.*;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.ThesisCommentDto;
import de.tum.cit.aet.thesis.dto.ThesisDto;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.service.ThesisCommentService;
import de.tum.cit.aet.thesis.service.ThesisPresentationService;
import de.tum.cit.aet.thesis.service.ThesisService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/v2/theses")
public class ThesisController {
    private final ThesisService thesisService;
    private final ThesisCommentService thesisCommentService;
    private final ThesisPresentationService thesisPresentationService;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

    @Autowired
    public ThesisController(ThesisService thesisService, ThesisCommentService thesisCommentService, ThesisPresentationService thesisPresentationService,
        ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
        this.thesisService = thesisService;
        this.thesisCommentService = thesisCommentService;
        this.thesisPresentationService = thesisPresentationService;
        this.currentUserProviderProvider = currentUserProviderProvider;
    }

    private CurrentUserProvider currentUserProvider() {
        return currentUserProviderProvider.getObject();
    }

    @GetMapping
    public ResponseEntity<PaginationDto<ThesisDto>> getTheses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ThesisState[] state,
            @RequestParam(required = false) String[] type,
            @RequestParam(required = false, defaultValue = "false") boolean fetchAll,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false, defaultValue = "") UUID[] researchGroupIds
    ) {
        User currentUser = currentUserProvider().getUser();

        Page<Thesis> theses = thesisService.getAll(
                currentUser.getId(),
                fetchAll,
                search,
                state,
                type,
                page,
                limit,
                sortBy,
                sortOrder,
                researchGroupIds
        );

        return ResponseEntity.ok(PaginationDto.fromSpringPage(
                theses.map(thesis -> ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)))
        ));
    }

    @GetMapping("/{thesisId}")
    public ResponseEntity<ThesisDto> getThesis(@PathVariable UUID thesisId) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasReadAccess(currentUser)) {
            throw new AccessDeniedException("You do not have the required permissions to view this thesis");
        }

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<ThesisDto> createThesis(
            @RequestBody CreateThesisPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.createThesis(
                RequestValidator.validateStringMaxLength(payload.thesisTitle(), StringLimits.THESIS_TITLE.getLimit()),
                RequestValidator.validateStringMaxLength(payload.thesisType(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.language(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateNotNull(payload.supervisorIds()),
                RequestValidator.validateNotNull(payload.advisorIds()),
                RequestValidator.validateNotNull(payload.studentIds()),
                null,
                true,
                RequestValidator.validateNotNull(payload.researchGroupId())
        );
        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PutMapping("/{thesisId}")
    public ResponseEntity<ThesisDto> updateThesisConfig(
            @PathVariable UUID thesisId,
            @RequestBody UpdateThesisPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be an advisor of this thesis to update the thesis");
        }

        thesis = thesisService.updateThesis(
                thesis,
                RequestValidator.validateStringMaxLength(payload.thesisTitle(), StringLimits.THESIS_TITLE.getLimit()),
                RequestValidator.validateStringMaxLength(payload.thesisType(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.language(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateNotNull(payload.visibility()),
                RequestValidator.validateNotNull(payload.keywords()),
                payload.startDate(),
                payload.endDate(),
                RequestValidator.validateNotNull(payload.studentIds()),
                RequestValidator.validateNotNull(payload.advisorIds()),
                RequestValidator.validateNotNull(payload.supervisorIds()),
                RequestValidator.validateNotNull(payload.states()),
                RequestValidator.validateNotNull(payload.researchGroupId())
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @DeleteMapping("/{thesisId}")
    public ResponseEntity<ThesisDto> closeThesis(
            @PathVariable UUID thesisId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You do not have the required permissions to view this thesis");
        }

        thesis = thesisService.closeThesis(thesis);

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PutMapping("/{thesisId}/info")
    public ResponseEntity<ThesisDto> updateThesisInfo(
            @PathVariable UUID thesisId,
            @RequestBody UpdateThesisInfoPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasStudentAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a student of this thesis to update the abstract and info");
        }

        thesis = thesisService.updateThesisInfo(
                thesis,
                RequestValidator.validateStringMaxLength(payload.abstractText(), StringLimits.UNLIMITED_TEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.infoText(), StringLimits.UNLIMITED_TEXT.getLimit())
        );

        thesis = thesisService.updateThesisTitles(
                thesis,
                RequestValidator.validateStringMaxLength(payload.primaryTitle(), StringLimits.THESIS_TITLE.getLimit()),
                RequestValidator.validateNotNull(payload.secondaryTitles())
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PutMapping("/{thesisId}/credits")
    public ResponseEntity<ThesisDto> updateThesisCredits(
            @PathVariable UUID thesisId,
            @RequestBody UpdateThesisCreditsPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be an advisor of this thesis to update the student credits");
        }

        thesis = thesisService.updateThesisCredits(
                thesis,
                payload.credits()
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    /* FEEDBACK ENDPOINTS */
    @PutMapping("/{thesisId}/feedback/{feedbackId}/{action}")
    public ResponseEntity<ThesisDto> completeFeedback(
            @PathVariable UUID thesisId,
            @PathVariable UUID feedbackId,
            @PathVariable String action
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasStudentAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a student of this thesis to complete a feedback item");
        }

        thesis = thesisService.completeFeedback(thesis, feedbackId, action.equals("complete"));

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @DeleteMapping("/{thesisId}/feedback/{feedbackId}")
    public ResponseEntity<ThesisDto> deleteFeedback(
            @PathVariable UUID thesisId,
            @PathVariable UUID feedbackId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a advisor of this thesis to delete a feedback item");
        }

        thesis = thesisService.deleteFeedback(thesis, feedbackId);

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PostMapping("/{thesisId}/feedback")
    public ResponseEntity<ThesisDto> requestChanges(
            @PathVariable UUID thesisId,
            @RequestBody RequestChangesPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be an advisor of this thesis to request changes");
        }

        thesis = thesisService.requestChanges(
                thesis,
                RequestValidator.validateNotNull(payload.type()),
                RequestValidator.validateNotNull(payload.requestedChanges())
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    /* PROPOSAL ENDPOINTS */

    @GetMapping("/{thesisId}/proposal/{proposalId}")
    public ResponseEntity<Resource> getProposalFile(
            @PathVariable UUID thesisId,
            @PathVariable UUID proposalId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasReadAccess(currentUser)) {
            throw new AccessDeniedException("You do not have the required permissions to view this thesis");
        }

        ThesisProposal proposal = thesis.getProposalById(proposalId).orElseThrow();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=proposal_%s.pdf", thesisId))
                .body(thesisService.getProposalFile(proposal));
    }

    @DeleteMapping("/{thesisId}/proposal/{proposalId}")
    public ResponseEntity<ThesisDto> deleteProposal(
            @PathVariable UUID thesisId,
            @PathVariable UUID proposalId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You do not have the required permissions to delete this proposal");
        }

        thesis = thesisService.deleteProposal(thesis, proposalId);

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PostMapping("/{thesisId}/proposal")
    public ResponseEntity<ThesisDto> uploadProposal(
            @PathVariable UUID thesisId,
            @RequestPart("proposal") MultipartFile proposalFile
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasStudentAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a student of this thesis to add a proposal");
        }

        if (thesis.getState() != ThesisState.PROPOSAL && !thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("Only advisors can upload a new proposal if thesis state is not PROPOSAL");
        }

        thesis = thesisService.uploadProposal(thesis, RequestValidator.validateNotNull(proposalFile));

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PutMapping("/{thesisId}/proposal/accept")
    public ResponseEntity<ThesisDto> acceptProposal(
            @PathVariable UUID thesisId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be an advisor of this thesis to accept a proposal");
        }

        thesis = thesisService.acceptProposal(thesis);

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    /* WRITING ENDPOINTS */

    @PutMapping("/{thesisId}/thesis/final-submission")
    public ResponseEntity<ThesisDto> submitThesis(
            @PathVariable UUID thesisId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasStudentAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a student of the thesis to do the final submission");
        }

        thesis = thesisService.submitThesis(thesis);

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PostMapping("/{thesisId}/files")
    public ResponseEntity<ThesisDto> uploadThesisFile(
            @PathVariable UUID thesisId,
            @RequestPart("type") String type,
            @RequestPart("file") MultipartFile file
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasStudentAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a student of this thesis to upload thesis files");
        }

        if (thesis.getState() != ThesisState.WRITING && !thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("Only advisors can upload a new file if thesis state is not WRITING");
        }

        thesis = thesisService.uploadThesisFile(thesis, type, RequestValidator.validateNotNull(file));

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @GetMapping("/{thesisId}/files/{fileId}")
    public ResponseEntity<Resource> getThesisFile(
            @PathVariable UUID thesisId,
            @PathVariable UUID fileId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasReadAccess(currentUser)) {
            throw new AccessDeniedException("You do not have the required permissions to view this thesis");
        }

        ThesisFile file = thesis.getFileById(fileId).orElseThrow();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=" + file.getFilename(), thesisId))
                .body(thesisService.getThesisFile(file));
    }

    @DeleteMapping("/{thesisId}/files/{fileId}")
    public ResponseEntity<ThesisDto> deleteThesisFile(
            @PathVariable UUID thesisId,
            @PathVariable UUID fileId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You do not have the required permissions to delete this file");
        }

        thesis = thesisService.deleteThesisFile(thesis, fileId);

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PostMapping("/{thesisId}/presentations")
    public ResponseEntity<ThesisDto> createPresentation(
            @PathVariable UUID thesisId,
            @RequestBody ReplacePresentationPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasStudentAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a student of this thesis to create a presentation");
        }

        thesis = thesisPresentationService.createPresentation(
                thesis,
                RequestValidator.validateNotNull(payload.type()),
                RequestValidator.validateNotNull(payload.visibility()),
                RequestValidator.validateStringMaxLength(payload.location(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.streamUrl(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.language(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateNotNull(payload.date())
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PutMapping("/{thesisId}/presentations/{presentationId}")
    public ResponseEntity<ThesisDto> updatePresentation(
            @PathVariable UUID thesisId,
            @PathVariable UUID presentationId,
            @RequestBody ReplacePresentationPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        ThesisPresentation presentation = thesisPresentationService.findById(thesisId, presentationId);
        Thesis thesis = presentation.getThesis();

        if (!thesis.hasStudentAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a student of this thesis to update a presentation");
        }

        if (presentation.getState() == ThesisPresentationState.SCHEDULED && !thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be an advisor of this thesis to update a scheduled presentation");
        }

        thesis = thesisPresentationService.updatePresentation(
                presentation,
                RequestValidator.validateNotNull(payload.type()),
                RequestValidator.validateNotNull(payload.visibility()),
                RequestValidator.validateStringMaxLength(payload.location(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.streamUrl(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.language(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateNotNull(payload.date())
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PostMapping("/{thesisId}/presentations/{presentationId}/schedule")
    public ResponseEntity<ThesisDto> schedulePresentation(
            @PathVariable UUID thesisId,
            @PathVariable UUID presentationId,
            @RequestBody SchedulePresentationPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        ThesisPresentation presentation = thesisPresentationService.findById(thesisId, presentationId);
        Thesis thesis = presentation.getThesis();

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be an advisor of this thesis to schedule a presentation");
        }

        thesis = thesisPresentationService.schedulePresentation(
                presentation,
                RequestValidator.validateNotNull(payload.inviteChairMembers()),
                RequestValidator.validateNotNull(payload.inviteThesisStudents()),
                RequestValidator.validateNotNull(payload.optionalAttendees())
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @DeleteMapping("/{thesisId}/presentations/{presentationId}")
    public ResponseEntity<ThesisDto> deletePresentation(
            @PathVariable UUID thesisId,
            @PathVariable UUID presentationId
    ) {
        User currentUser = currentUserProvider().getUser();
        ThesisPresentation presentation = thesisPresentationService.findById(thesisId, presentationId);

        if (!presentation.hasManagementAccess(currentUser)) {
            throw new AccessDeniedException("You are not allowed to delete this presentation");
        }

        Thesis thesis = thesisPresentationService.deletePresentation(presentation);

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @GetMapping("/{thesisId}/comments")
    public ResponseEntity<PaginationDto<ThesisCommentDto>> getComments(
            @PathVariable UUID thesisId,
            @RequestParam(required = false, defaultValue = "THESIS") ThesisCommentType commentType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (commentType == ThesisCommentType.ADVISOR && !thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be an advisor of this thesis to view advisor comments");
        }

        if (!thesis.hasReadAccess(currentUser)) {
            throw new AccessDeniedException("You do not have the required permissions to view comments on this thesis");
        }

        Page<ThesisComment> comments = thesisCommentService.getComments(thesis, commentType, page, limit);

        return ResponseEntity.ok(PaginationDto.fromSpringPage(comments.map(ThesisCommentDto::fromCommentEntity)));
    }

    @PostMapping("/{thesisId}/comments")
    public ResponseEntity<ThesisCommentDto> createComment(
            @PathVariable UUID thesisId,
            @RequestPart("data") PostThesisCommentPayload payload,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (payload.commentType() == ThesisCommentType.ADVISOR && !thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be an advisor of this thesis to add an advisor comment");
        }

        if (!thesis.hasStudentAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a student of this thesis to add a comment");
        }

        ThesisComment comment = thesisCommentService.postComment(
                thesis,
                RequestValidator.validateNotNull(payload.commentType()),
                RequestValidator.validateStringMaxLength(payload.message(), StringLimits.LONGTEXT.getLimit()),
                file
        );

        return ResponseEntity.ok(ThesisCommentDto.fromCommentEntity(comment));
    }

    @GetMapping("/{thesisId}/comments/{commentId}/file")
    public ResponseEntity<Resource> getCommentFile(
            @PathVariable UUID thesisId,
            @PathVariable UUID commentId
    ) {
        User currentUser = currentUserProvider().getUser();
        ThesisComment comment = thesisCommentService.findById(thesisId, commentId);

        if (comment.getType() == ThesisCommentType.ADVISOR && !comment.getThesis().hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a advisor of this thesis to view an advisor file");
        }

        if (!comment.getThesis().hasReadAccess(currentUser)) {
            throw new AccessDeniedException("You do not have the required permissions to view this comment");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=" + comment.getFilename(), commentId))
                .body(thesisCommentService.getCommentFile(comment));
    }

    @DeleteMapping("/{thesisId}/comments/{commentId}")
    public ResponseEntity<ThesisCommentDto> deleteComment(
            @PathVariable UUID thesisId,
            @PathVariable UUID commentId
    ) {
        User currentUser = currentUserProvider().getUser();
        ThesisComment comment = thesisCommentService.findById(thesisId, commentId);

        if (!comment.hasManagementAccess(currentUser)) {
            throw new AccessDeniedException("You are not allowed to delete this comment");
        }

        comment = thesisCommentService.deleteComment(comment);

        return ResponseEntity.ok(ThesisCommentDto.fromCommentEntity(comment));
    }

    /* ASSESSMENT ENDPOINTS */

    @GetMapping("/{thesisId}/assessment")
    public ResponseEntity<Resource> getAssessmentFile(
            @PathVariable UUID thesisId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a advisor of this thesis to add an assessment");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=assessment.pdf")
                .body(thesisService.getAssessmentFile(thesis));
    }

    @PostMapping("/{thesisId}/assessment")
    public ResponseEntity<ThesisDto> createAssessment(
            @PathVariable UUID thesisId,
            @RequestBody CreateAssessmentPayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasAdvisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a advisor of this thesis to add an assessment");
        }

        thesis = thesisService.submitAssessment(
                thesis,
                RequestValidator.validateStringMaxLength(payload.summary(), StringLimits.UNLIMITED_TEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.positives(), StringLimits.UNLIMITED_TEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.negatives(), StringLimits.UNLIMITED_TEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.gradeSuggestion(), StringLimits.THESIS_GRADE.getLimit())
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    /* GRADE ENDPOINTS */

    @PostMapping("/{thesisId}/grade")
    public ResponseEntity<ThesisDto> addGrade(
            @PathVariable UUID thesisId,
            @RequestBody AddThesisGradePayload payload
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasSupervisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a supervisor of this thesis to add a final grade");
        }

        thesis = thesisService.gradeThesis(
                thesis,
                RequestValidator.validateStringMaxLength(payload.finalGrade(), StringLimits.THESIS_GRADE.getLimit()),
                RequestValidator.validateStringMaxLength(payload.finalFeedback(), StringLimits.UNLIMITED_TEXT.getLimit()),
                RequestValidator.validateNotNull(payload.visibility())
        );

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }

    @PostMapping("/{thesisId}/complete")
    public ResponseEntity<ThesisDto> completeThesis(
            @PathVariable UUID thesisId
    ) {
        User currentUser = currentUserProvider().getUser();
        Thesis thesis = thesisService.findById(thesisId);

        if (!thesis.hasSupervisorAccess(currentUser)) {
            throw new AccessDeniedException("You need to be a supervisor of this thesis to close the thesis");
        }

        thesis = thesisService.completeThesis(thesis);

        return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
    }
}
