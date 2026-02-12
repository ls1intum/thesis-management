package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.constants.StringLimits;
import de.tum.cit.aet.thesis.controller.payload.AcceptApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.RejectApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.ReviewApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateApplicationCommentPayload;
import de.tum.cit.aet.thesis.dto.ApplicationDto;
import de.tum.cit.aet.thesis.dto.ApplicationInterviewProcessDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceAlreadyExistsException;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.service.ApplicationService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing thesis applications, including creation, review, acceptance, and rejection.
 */
@Slf4j
@RestController
@RequestMapping("/v2/applications")
public class ApplicationController {
	private final ApplicationService applicationService;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	/**
	 * Injects the application service and current user provider.
	 *
	 * @param applicationService the application service
	 * @param currentUserProviderProvider the current user provider
	 */
	@Autowired
	public ApplicationController(ApplicationService applicationService,
		ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
		this.applicationService = applicationService;
		this.currentUserProviderProvider = currentUserProviderProvider;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Creates a new thesis application for the authenticated user.
	 *
	 * @param payload the payload containing the application details
	 * @return the created application
	 */
	@PostMapping
	public ResponseEntity<ApplicationDto> createApplication(@RequestBody CreateApplicationPayload payload) {
		User authenticatedUser = currentUserProvider().getUser();

		if (payload.topicId() == null && payload.thesisTitle() == null) {
			throw new ResourceInvalidParametersException("Either topic id or a thesis title must be provided");
		}

		if (applicationService.applicationExists(authenticatedUser, payload.topicId())) {
			throw new ResourceAlreadyExistsException("There is already a pending application for this topic. Please edit your application in the dashboard.");
		}

		Application application = applicationService.createApplication(authenticatedUser,
				payload.researchGroupId(),
				payload.topicId(),
				RequestValidator.validateStringMaxLengthAllowNull(payload.thesisTitle(),
						StringLimits.THESIS_TITLE.getLimit()),
				RequestValidator.validateStringMaxLength(payload.thesisType(),
						StringLimits.THESIS_TITLE.getLimit()),
				RequestValidator.validateNotNull(payload.desiredStartDate()),
				RequestValidator.validateStringMaxLength(payload.motivation(),
						StringLimits.LONGTEXT.getLimit())
		);

		return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
	}

	/**
	 * Retrieves all applications with optional filtering by state, topic, type, and pagination support.
	 *
	 * @param search the search term to filter applications
	 * @param state the application states to filter by
	 * @param topic the topic identifiers to filter by
	 * @param types the thesis types to filter by
	 * @param previous the previous application states to filter by
	 * @param includeSuggestedTopics whether to include suggested topics
	 * @param fetchAll whether to fetch all applications regardless of ownership
	 * @param page the page number for pagination
	 * @param limit the number of items per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction (asc or desc)
	 * @return the paginated list of applications
	 */
	@GetMapping
	public ResponseEntity<PaginationDto<ApplicationDto>> getApplications(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) ApplicationState[] state,
			@RequestParam(required = false) String[] topic,
			@RequestParam(required = false) String[] types,
			@RequestParam(required = false) String[] previous,
			@RequestParam(required = false, defaultValue = "true") boolean includeSuggestedTopics,
			@RequestParam(required = false, defaultValue = "false") boolean fetchAll,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "createdAt") String sortBy,
			@RequestParam(required = false, defaultValue = "desc") String sortOrder
	) {
		User authenticatedUser = currentUserProvider().getUser();

		Page<Application> applications = applicationService.getAll(
				fetchAll && authenticatedUser.hasAnyGroup("admin", "supervisor", "advisor") ? null :
					authenticatedUser.getId(),
				fetchAll && authenticatedUser.hasAnyGroup("admin", "supervisor", "advisor") ?
						authenticatedUser.getId() :
					null,
				search,
				state,
				previous,
				topic,
				types,
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

	/**
	 * Retrieves applications eligible for interview scheduling for a given topic.
	 *
	 * @param topicId the identifier of the topic to get interview applications for
	 * @param page the page number for pagination
	 * @param limit the number of items per page
	 * @return the paginated list of interview-eligible applications
	 */
	@GetMapping("/interview-applications")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<PaginationDto<ApplicationInterviewProcessDto>> getPossibleInterviewApplicationsForTopic(
			@RequestParam(required = true) String topicId,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit
	) {
		Page<Application> applications = applicationService.getAll(
				null,
				null,
				null,
				new ApplicationState[]{ApplicationState.NOT_ASSESSED, ApplicationState.INTERVIEWING},
				null,
				new String[]{topicId},
				null,
				false,
				page,
				limit <= 0 ? Integer.MAX_VALUE : limit,
				"createdAt",
				"desc"
		);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(
				applications.map(ApplicationInterviewProcessDto::from)
		));
	}

	/**
	 * Retrieves a single application by its ID.
	 *
	 * @param applicationId the unique identifier of the application
	 * @return the application details
	 */
	@GetMapping("/{applicationId}")
	public ResponseEntity<ApplicationDto> getApplication(@PathVariable UUID applicationId) {
		User authenticatedUser = currentUserProvider().getUser();
		Application application = applicationService.findById(applicationId);

		if (!application.hasReadAccess(authenticatedUser)) {
			throw new AccessDeniedException("You do not have access to this application");
		}

		return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
	}

	/**
	 * Updates an existing application that has not yet been assessed or reviewed.
	 *
	 * @param applicationId the unique identifier of the application to update
	 * @param payload the payload containing the updated application details
	 * @return the updated application
	 */
	@PutMapping("/{applicationId}")
	public ResponseEntity<ApplicationDto> updateApplication(
			@PathVariable UUID applicationId,
			@RequestBody CreateApplicationPayload payload
	) {
		User authenticatedUser = currentUserProvider().getUser();
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

	/**
	 * Updates the management comment on an application.
	 *
	 * @param applicationId the unique identifier of the application
	 * @param payload the payload containing the comment to update
	 * @return the updated application
	 */
	@PutMapping("/{applicationId}/comment")
	public ResponseEntity<ApplicationDto> updateComment(
			@PathVariable UUID applicationId,
			@RequestBody UpdateApplicationCommentPayload payload
	) {
		User authenticatedUser = currentUserProvider().getUser();
		Application application = applicationService.findById(applicationId);

		if (!application.hasManagementAccess(authenticatedUser)) {
			throw new AccessDeniedException("You do not have access to comment this application");
		}

		application = applicationService.updateComment(
				application,
				RequestValidator.validateStringMaxLength(payload.comment(), StringLimits.LONGTEXT.getLimit())
		);

		return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
	}

	/**
	 * Submits a review for an application by an authorized reviewer.
	 *
	 * @param applicationId the unique identifier of the application to review
	 * @param payload the payload containing the review details
	 * @return the reviewed application
	 */
	@PutMapping("/{applicationId}/review")
	public ResponseEntity<ApplicationDto> reviewApplication(
			@PathVariable UUID applicationId,
			@RequestBody ReviewApplicationPayload payload
	) {
		User authenticatedUser = currentUserProvider().getUser();
		Application application = applicationService.findById(applicationId);

		if (!application.hasManagementAccess(authenticatedUser)) {
			throw new AccessDeniedException("You do not have access to review this application");
		}

		application = applicationService.reviewApplication(
				application,
				authenticatedUser,
				RequestValidator.validateNotNull(payload.reason())
		);

		return ResponseEntity.ok(ApplicationDto.fromApplicationEntity(application, application.hasManagementAccess(authenticatedUser)));
	}

	/**
	 * Accepts an application and creates the corresponding thesis.
	 *
	 * @param applicationId the unique identifier of the application to accept
	 * @param payload the payload containing the acceptance details
	 * @return the list of affected applications
	 */
	@PutMapping("/{applicationId}/accept")
	public ResponseEntity<List<ApplicationDto>> acceptApplication(
			@PathVariable UUID applicationId,
			@RequestBody AcceptApplicationPayload payload
	) {
		User authenticatedUser = currentUserProvider().getUser();
		Application application = applicationService.findById(applicationId);

		if (!application.hasManagementAccess(authenticatedUser)) {
			throw new AccessDeniedException("You do not have access to accept this application");
		}

		if (application.getState().equals(ApplicationState.ACCEPTED)) {
			throw new ResourceInvalidParametersException("This application has already been accepted");
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

	/**
	 * Rejects an application with a given reason and optionally notifies the applicant.
	 *
	 * @param applicationId the unique identifier of the application to reject
	 * @param payload the payload containing the rejection reason and notification preference
	 * @return the list of affected applications
	 */
	@PutMapping("/{applicationId}/reject")
	public ResponseEntity<List<ApplicationDto>> rejectApplication(
			@PathVariable UUID applicationId,
			@RequestBody RejectApplicationPayload payload
	) {
		User authenticatedUser = currentUserProvider().getUser();
		Application application = applicationService.findById(applicationId);

		if (!application.hasManagementAccess(authenticatedUser)) {
			throw new AccessDeniedException("You do not have access to reject this application");
		}

		List<Application> applications = applicationService.reject(
				authenticatedUser,
				application,
				RequestValidator.validateNotNull(payload.reason()),
				RequestValidator.validateNotNull(payload.notifyUser()),
				true
		);

		return ResponseEntity.ok(
				applications.stream().map(item -> ApplicationDto.fromApplicationEntity(item, item.hasManagementAccess(authenticatedUser))).toList()
		);
	}
}
