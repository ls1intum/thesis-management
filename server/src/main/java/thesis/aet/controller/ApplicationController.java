package de.tum.cit.aet.thesis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.constants.StringLimits;
import de.tum.cit.aet.thesis.controller.payload.*;
import de.tum.cit.aet.thesis.dto.ApplicationDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceAlreadyExistsException;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.service.ApplicationService;
import de.tum.cit.aet.thesis.service.AuthenticationService;
import de.tum.cit.aet.thesis.utility.RequestValidator;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v2/applications")
public class ApplicationController {
    private final ApplicationService applicationService;
    private final AuthenticationService authenticationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService, AuthenticationService authenticationService) {
        this.applicationService = applicationService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
            @RequestBody CreateApplicationPayload payload,
            JwtAuthenticationToken jwt
    ) {
        User authenticatedUser = authenticationService.getAuthenticatedUser(jwt);

        if (payload.topicId() == null && payload.thesisTitle() == null) {
            throw new ResourceInvalidParametersException("Either topic id or a thesis title must be provided");
        }

        if (applicationService.applicationExists(authenticatedUser, payload.topicId())) {
            throw new ResourceAlreadyExistsException("There is already a pending application for this topic. Please edit your application in the dashboard.");
        }

        Application application = applicationService.createApplication(
                authenticatedUser,
                payload.topicId(),
                RequestValidator.validateStringMaxLengthAllowNull(payload.thesisTitle(), StringLimits.THESIS_TITLE.getLimit()),
                RequestValidator.validateStringMaxLength(payload.thesisType(), StringLimits.THESIS_TITLE.getLimit()),
                RequestValidator.validateNotNull(payload.desiredStartDate()),
                RequestValidator.validateStringMaxLength(payload.motivation(), StringLimits.LONGTEXT.getLimit())
        );

        return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
    }

    @GetMapping
    public ResponseEntity<PaginationDto<ApplicationDto>> getApplications(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationState[] state,
            @RequestParam(required = false) String[] topic,
            @RequestParam(required = false) String[] type,
            @RequestParam(required = false) String[] previous,
            @RequestParam(required = false, defaultValue = "true") Boolean includeSuggestedTopics,
            @RequestParam(required = false, defaultValue = "false") Boolean fetchAll,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            JwtAuthenticationToken jwt
    ) {
        User authenticatedUser = authenticationService.getAuthenticatedUser(jwt);

        Page<Application> applications = applicationService.getAll(
                fetchAll && authenticatedUser.hasAnyGroup("admin", "supervisor", "advisor") ? null : authenticatedUser.getId(),
                fetchAll && authenticatedUser.hasAnyGroup("admin", "supervisor", "advisor") ? authenticatedUser.getId() : null,
                search,
                state,
                previous,
                topic,
                type,
                includeSuggestedTopics,
                page,
                limit,
                sortBy,
                sortOrder
        );

        return ResponseEntity.ok(PaginationDto.fromSpringPage(
                applications.map(application -> ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)))
        ));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationDto> getApplication(@PathVariable UUID applicationId, JwtAuthenticationToken jwt) {
        User authenticatedUser = authenticationService.getAuthenticatedUser(jwt);
        Application application = applicationService.findById(applicationId);

        if (!application.hasReadAccess(authenticatedUser)) {
            throw new AccessDeniedException("You do not have access to this application");
        }

        return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
    }

    @PutMapping("/{applicationId}")
    public ResponseEntity<ApplicationDto> updateApplication(
            @PathVariable UUID applicationId,
            @RequestBody CreateApplicationPayload payload,
            JwtAuthenticationToken jwt
    ) {
        User authenticatedUser = authenticationService.getAuthenticatedUser(jwt);
        Application application = applicationService.findById(applicationId);

        if (!application.hasEditAccess(authenticatedUser)) {
            throw new AccessDeniedException("You do not have edit access to this application");
        }

        if (!application.getState().equals(ApplicationState.NOT_ASSESSED)) {
            throw new ResourceInvalidParametersException("Only applications that are not assessed can be edited");
        }

        if (!application.getReviewers().isEmpty()) {
            throw new ResourceInvalidParametersException("The review of this application has already started. You cannot edit it anymore.");
        }

        application = applicationService.updateApplication(
                application,
                payload.topicId(),
                RequestValidator.validateStringMaxLengthAllowNull(payload.thesisTitle(), StringLimits.THESIS_TITLE.getLimit()),
                RequestValidator.validateStringMaxLength(payload.thesisType(), StringLimits.THESIS_TITLE.getLimit()),
                RequestValidator.validateNotNull(payload.desiredStartDate()),
                RequestValidator.validateStringMaxLength(payload.motivation(), StringLimits.LONGTEXT.getLimit())
        );

        return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
    }

    @PutMapping("/{applicationId}/comment")
    public ResponseEntity<ApplicationDto> updateComment(
            @PathVariable UUID applicationId,
            @RequestBody UpdateApplicationCommentPayload payload,
            JwtAuthenticationToken jwt
    ) {
        User authenticatedUser = this.authenticationService.getAuthenticatedUser(jwt);
        Application application = applicationService.findById(applicationId);

        if (!application.hasManagementAccess(authenticatedUser)) {
            throw new AccessDeniedException("You do not have access to comment this application");
        }

        application =  applicationService.updateComment(
                application,
                RequestValidator.validateStringMaxLength(payload.comment(), StringLimits.LONGTEXT.getLimit())
        );

        return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
    }

    @PutMapping("/{applicationId}/review")
    public ResponseEntity<ApplicationDto> reviewApplication(
            @PathVariable UUID applicationId,
            @RequestBody ReviewApplicationPayload payload,
            JwtAuthenticationToken jwt
    ) {
        User authenticatedUser = this.authenticationService.getAuthenticatedUser(jwt);
        Application application = applicationService.findById(applicationId);

        if (!application.hasManagementAccess(authenticatedUser)) {
            throw new AccessDeniedException("You do not have access to review this application");
        }

        application =  applicationService.reviewApplication(
                application,
                authenticatedUser,
                RequestValidator.validateNotNull(payload.reason())
        );

        return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
    }

    @PutMapping("/{applicationId}/accept")
    public ResponseEntity<List<ApplicationDto>> acceptApplication(
            @PathVariable UUID applicationId,
            @RequestBody AcceptApplicationPayload payload,
            JwtAuthenticationToken jwt
    ) {
        User authenticatedUser = this.authenticationService.getAuthenticatedUser(jwt);
        Application application = applicationService.findById(applicationId);

        if (!application.hasManagementAccess(authenticatedUser)) {
            throw new AccessDeniedException("You do not have access to accept this application");
        }

        List<Application> applications = applicationService.accept(
                authenticatedUser,
                application,
                RequestValidator.validateStringMaxLength(payload.thesisTitle(), StringLimits.THESIS_TITLE.getLimit()),
                RequestValidator.validateStringMaxLength(payload.thesisType(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateStringMaxLength(payload.language(), StringLimits.SHORTTEXT.getLimit()),
                RequestValidator.validateNotNull(payload.advisorIds()),
                RequestValidator.validateNotNull(payload.supervisorIds()),
                RequestValidator.validateNotNull(payload.notifyUser()),
                RequestValidator.validateNotNull(payload.closeTopic())
        );

        return ResponseEntity.ok(
                applications.stream().map(item -> ApplicationDto.fromApplicationEntity(item, item.hasManagementAccess(authenticatedUser))).toList()
        );
    }

    @PutMapping("/{applicationId}/reject")
    public ResponseEntity<List<ApplicationDto>> rejectApplication(
            @PathVariable UUID applicationId,
            @RequestBody RejectApplicationPayload payload,
            JwtAuthenticationToken jwt
    ) {
        User authenticatedUser = this.authenticationService.getAuthenticatedUser(jwt);
        Application application = applicationService.findById(applicationId);

        if (!application.hasManagementAccess(authenticatedUser)) {
            throw new AccessDeniedException("You do not have access to reject this application");
        }

        List<Application> applications = applicationService.reject(
                authenticatedUser,
                application,
                RequestValidator.validateNotNull(payload.reason()),
                RequestValidator.validateNotNull(payload.notifyUser())
        );

        return ResponseEntity.ok(
                applications.stream().map(item -> ApplicationDto.fromApplicationEntity(item, item.hasManagementAccess(authenticatedUser))).toList()
        );
    }
}
