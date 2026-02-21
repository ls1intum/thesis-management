package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.StringLimits;
import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationState;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.controller.payload.AddThesisGradePayload;
import de.tum.cit.aet.thesis.controller.payload.CreateAssessmentPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.controller.payload.PostThesisCommentPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplacePresentationPayload;
import de.tum.cit.aet.thesis.controller.payload.RequestChangesPayload;
import de.tum.cit.aet.thesis.controller.payload.SchedulePresentationPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateNotePayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateThesisCreditsPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateThesisInfoPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateThesisPayload;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.ThesisCommentDto;
import de.tum.cit.aet.thesis.dto.ThesisDto;
import de.tum.cit.aet.thesis.dto.ThesisOverviewDto;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisComment;
import de.tum.cit.aet.thesis.entity.ThesisFile;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.entity.ThesisProposal;
import de.tum.cit.aet.thesis.entity.User;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** REST controller for managing theses and their associated resources. */
@Slf4j
@RestController
@RequestMapping("/v2/theses")
public class ThesisController {
	private final ThesisService thesisService;
	private final ThesisCommentService thesisCommentService;
	private final ThesisPresentationService thesisPresentationService;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	/**
	 * Injects the thesis service, comment service, presentation service, and current user provider.
	 *
	 * @param thesisService the service for thesis operations
	 * @param thesisCommentService the service for thesis comment operations
	 * @param thesisPresentationService the service for thesis presentation operations
	 * @param currentUserProviderProvider the provider for the current authenticated user
	 */
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

	/**
	 * Retrieves a paginated list of theses filtered by search, state, type, and research group.
	 *
	 * @param search the search query to filter theses
	 * @param state the thesis states to filter by
	 * @param type the thesis types to filter by
	 * @param fetchAll whether to fetch all theses regardless of user access
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @param sortBy the field to sort results by
	 * @param sortOrder the sort direction (asc or desc)
	 * @param researchGroupIds the research group IDs to filter by
	 * @return the paginated list of theses
	 */
	@GetMapping
	public ResponseEntity<PaginationDto<ThesisOverviewDto>> getTheses(
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
				theses.map(ThesisOverviewDto::fromThesisEntity)
		));
	}

	/**
	 * Retrieves a single thesis by its identifier.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @return the thesis data
	 */
	@GetMapping("/{thesisId}")
	public ResponseEntity<ThesisDto> getThesis(@PathVariable UUID thesisId) {
		User currentUser = currentUserProvider().getUser();
		Thesis thesis = thesisService.findById(thesisId);

		if (!thesis.hasReadAccess(currentUser)) {
			throw new AccessDeniedException("You do not have the required permissions to view this thesis");
		}

		return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
	}

	/**
	 * Creates a new thesis with the specified title, type, language, and assigned roles.
	 *
	 * @param payload the thesis creation data
	 * @return the created thesis
	 */
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

	/**
	 * Updates the configuration of an existing thesis including title, type, dates, and assigned roles.
	 *
	 * @param thesisId the unique identifier of the thesis to update
	 * @param payload the updated thesis configuration data
	 * @return the updated thesis
	 */
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

	/**
	 * Closes a thesis, preventing further modifications.
	 *
	 * @param thesisId the unique identifier of the thesis to close
	 * @return the closed thesis
	 */
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

	/**
	 * Updates the abstract, info text, and titles of a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis to update
	 * @param payload the updated thesis info data
	 * @return the updated thesis
	 */
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
				RequestValidator.validateStringMaxLengthAllowNull(payload.abstractText(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateStringMaxLengthAllowNull(payload.infoText(), StringLimits.UNLIMITED_TEXT.getLimit())
		);

		thesis = thesisService.updateThesisTitles(
				thesis,
				RequestValidator.validateStringMaxLengthAllowNull(payload.primaryTitle(), StringLimits.THESIS_TITLE.getLimit()),
				payload.secondaryTitles()
		);

		return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
	}

	/**
	 * Updates the credit points assigned to a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis to update
	 * @param payload the updated thesis credits data
	 * @return the updated thesis
	 */
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
	/**
	 * Marks a feedback item as complete or incomplete for a given thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param feedbackId the unique identifier of the feedback item
	 * @param action the action to perform (complete or incomplete)
	 * @return the updated thesis
	 */
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

	/**
	 * Deletes a feedback item from a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param feedbackId the unique identifier of the feedback item to delete
	 * @return the updated thesis
	 */
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

	/**
	 * Creates a new feedback item requesting changes on a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param payload the requested changes data
	 * @return the updated thesis
	 */
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

	/**
	 * Downloads the PDF file of a thesis proposal.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param proposalId the unique identifier of the proposal
	 * @return the proposal PDF file as a resource
	 */
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

	/**
	 * Deletes a proposal from a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param proposalId the unique identifier of the proposal to delete
	 * @return the updated thesis
	 */
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

	/**
	 * Uploads a new proposal file for a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param proposalFile the proposal PDF file to upload
	 * @return the updated thesis
	 */
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

	/**
	 * Accepts the current proposal of a thesis and advances the thesis state.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @return the updated thesis
	 */
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

	/**
	 * Submits the final version of a thesis for assessment.
	 *
	 * @param thesisId the unique identifier of the thesis to submit
	 * @return the updated thesis
	 */
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

	/**
	 * Uploads a file attachment to a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param type the type of file being uploaded
	 * @param file the file to upload
	 * @return the updated thesis
	 */
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

	/**
	 * Downloads a file attachment from a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param fileId the unique identifier of the file to download
	 * @return the file as a resource
	 */
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

	/**
	 * Deletes a file attachment from a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param fileId the unique identifier of the file to delete
	 * @return the updated thesis
	 */
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

	/**
	 * Creates a new presentation draft for a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param payload the presentation data
	 * @return the updated thesis
	 */
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

	/**
	 * Updates an existing presentation for a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param presentationId the unique identifier of the presentation to update
	 * @param payload the updated presentation data
	 * @return the updated thesis
	 */
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

	/**
	 * Updates the note of a scheduled thesis presentation.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param presentationId the unique identifier of the presentation
	 * @param payload the updated note data
	 * @return the updated thesis
	 */
	@PutMapping("/{thesisId}/presentations/{presentationId}/note")
	public ResponseEntity<ThesisDto> updateNote(
			@PathVariable UUID thesisId,
			@PathVariable UUID presentationId,
			@RequestBody UpdateNotePayload payload
	) {
		User currentUser = currentUserProvider().getUser();
		ThesisPresentation presentation = thesisPresentationService.findById(thesisId, presentationId);
		Thesis thesis = presentation.getThesis();

		if (!thesis.hasStudentAccess(currentUser)) {
			throw new AccessDeniedException("You need to be a student of this thesis to update the presentation note");
		}

		if (presentation.getState() == ThesisPresentationState.DRAFTED) {
			throw new AccessDeniedException("The Presentation note can only be updated for scheduled presentations");
		}

		thesis = thesisPresentationService.updatePresentationNote(
				presentation,
				RequestValidator.validateStringMaxLength(payload.note(), StringLimits.UNLIMITED_TEXT.getLimit())
		);

		return ResponseEntity.ok(ThesisDto.fromThesisEntity(thesis, thesis.hasAdvisorAccess(currentUser), thesis.hasStudentAccess(currentUser)));
	}

	/**
	 * Schedules a drafted presentation and sends invitations to attendees.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param presentationId the unique identifier of the presentation to schedule
	 * @param payload the scheduling data including attendees
	 * @return the updated thesis
	 */
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

	/**
	 * Deletes a presentation from a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param presentationId the unique identifier of the presentation to delete
	 * @return the updated thesis
	 */
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

	/**
	 * Retrieves a paginated list of comments for a thesis, filtered by comment type.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param commentType the type of comments to retrieve
	 * @param page the page number for pagination
	 * @param limit the maximum number of comments per page
	 * @return the paginated list of thesis comments
	 */
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

	/**
	 * Creates a new comment on a thesis with an optional file attachment.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param payload the comment data
	 * @param file the optional file attachment
	 * @return the created comment
	 */
	@PostMapping("/{thesisId}/comments")
	public ResponseEntity<ThesisCommentDto> createComment(
			@PathVariable UUID thesisId,
			@RequestPart("data") PostThesisCommentPayload payload,
			@RequestPart(value = "file", required = false) MultipartFile file
	) {
		User currentUser = currentUserProvider().getUser();
		Thesis thesis = thesisService.findById(thesisId);

		System.out.println("Message: " + payload.message() + " , Comment: " + payload.commentType() + " , File: " + (file != null ? file.getOriginalFilename() : "No File"));

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

	/**
	 * Downloads the file attached to a thesis comment.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param commentId the unique identifier of the comment
	 * @return the comment file as a resource
	 */
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

	/**
	 * Deletes a comment from a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param commentId the unique identifier of the comment to delete
	 * @return the deleted comment
	 */
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

	/**
	 * Downloads the assessment PDF file for a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @return the assessment PDF file as a resource
	 */
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

	/**
	 * Submits an assessment with summary, positives, negatives, and a grade suggestion.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param payload the assessment data
	 * @return the updated thesis
	 */
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

	/**
	 * Adds a final grade and feedback to a thesis.
	 *
	 * @param thesisId the unique identifier of the thesis
	 * @param payload the grade and feedback data
	 * @return the updated thesis
	 */
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

	/**
	 * Marks a thesis as completed after grading.
	 *
	 * @param thesisId the unique identifier of the thesis to complete
	 * @return the completed thesis
	 */
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
