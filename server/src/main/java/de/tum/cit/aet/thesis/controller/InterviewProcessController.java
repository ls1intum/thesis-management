package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.AddIntervieweesPayload;
import de.tum.cit.aet.thesis.controller.payload.BookInterviewSlotPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateInterviewProcessPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateInterviewSlotsPayload;
import de.tum.cit.aet.thesis.controller.payload.InviteIntervieweesPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateIntervieweeAssessmentPayload;
import de.tum.cit.aet.thesis.dto.ApplicationInterviewProcessDto;
import de.tum.cit.aet.thesis.dto.InterviewProcessDto;
import de.tum.cit.aet.thesis.dto.InterviewSlotDto;
import de.tum.cit.aet.thesis.dto.IntervieweeDTO;
import de.tum.cit.aet.thesis.dto.IntervieweeLightWithNextSlotDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.TopicDto;
import de.tum.cit.aet.thesis.dto.UpcomingInterviewDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.service.InterviewProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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

/** REST controller for managing interview processes including slots, interviewees, and bookings. */
@RestController
@RequestMapping("/v2/interview-process")
public class InterviewProcessController {
	private final InterviewProcessService interviewProcessService;

	/**
	 * Injects the interview process service dependency.
	 *
	 * @param interviewProcessService the interview process service
	 */
	@Autowired
	public InterviewProcessController(InterviewProcessService interviewProcessService) {
		this.interviewProcessService = interviewProcessService;
	}

	/**
	 * Retrieves a paginated list of interview processes for the current user.
	 *
	 * @param searchQuery the search query to filter processes
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction (asc or desc)
	 * @param excludeSupervised whether to exclude supervised processes
	 * @return the paginated list of interview processes
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<PaginationDto<InterviewProcessDto>> getMyInterviewProcesses(
			@RequestParam(required = false) String searchQuery,
			@RequestParam(required = false, defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "20") int limit,
			@RequestParam(required = false, defaultValue = "completed") String sortBy,
			@RequestParam(required = false, defaultValue = "asc") String sortOrder,
			@RequestParam(required = false, defaultValue = "false") boolean excludeSupervised
	) {

		Page<InterviewProcess> result = interviewProcessService.findAllMyProcesses(
				searchQuery,
				page,
				limit,
				sortBy,
				sortOrder,
				excludeSupervised
		);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(result.map(InterviewProcessDto::fromInterviewProcessEntity)));
	}

	/**
	 * Retrieves the current user's booked interview slot for the specified process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return the booked interview slot or no content if none exists
	 */
	@GetMapping("/{interviewProcessId}/my-booking")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<InterviewSlotDto> getMyBookedSlot(
			@PathVariable("interviewProcessId") UUID interviewProcessId
	) {
		InterviewSlot slot = interviewProcessService.getMyBookedSlot(interviewProcessId);
		if (slot == null) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(InterviewSlotDto.fromInterviewSlot(slot));
	}

	/**
	 * Creates a new interview process for a topic with the specified interviewee applications.
	 *
	 * @param payload the payload containing the topic ID and interviewee application IDs
	 * @return the created interview process
	 */
	@PostMapping
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<InterviewProcessDto> createInterviewProcess(@RequestBody CreateInterviewProcessPayload payload) {
		InterviewProcessDto interviewProcessDto = InterviewProcessDto.fromInterviewProcessEntity(
				interviewProcessService.createInterviewProcess(payload.topicId(), payload.intervieweeApplicationIds())
		);
		return ResponseEntity.ok(interviewProcessDto);
	}

	/**
	 * Retrieves all upcoming interviews with booked slots for the current user.
	 *
	 * @return the list of upcoming interviews
	 */
	@GetMapping("/upcoming-interviews")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<List<UpcomingInterviewDto>> getUpcomingInterviews() {
		List<UpcomingInterviewDto> upcomingInterviews = interviewProcessService.getUpcomingInterviewsForCurrentUser();
		return ResponseEntity.ok(upcomingInterviews);
	}

	/**
	 * Retrieves a paginated list of interviewees for the specified interview process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param searchQuery the search query to filter interviewees
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction (asc or desc)
	 * @param state the interviewee state to filter by
	 * @return the paginated list of interviewees
	 */
	@GetMapping("/{interviewProcessId}/interviewees")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<PaginationDto<IntervieweeLightWithNextSlotDto>> getInterviewProcessInterviewees(
			@PathVariable("interviewProcessId") UUID interviewProcessId,
			@RequestParam(required = false) String searchQuery,
			@RequestParam(required = false, defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "50") int limit,
			@RequestParam(required = false, defaultValue = "lastInvited") String sortBy,
			@RequestParam(required = false, defaultValue = "asc") String sortOrder,
			@RequestParam(required = false, defaultValue = "") String state
	) {

		Page<Interviewee> interviewProcessDto = interviewProcessService.getInterviewProcessInterviewees(interviewProcessId, searchQuery, page, limit, sortBy, sortOrder, state);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(interviewProcessDto.map(IntervieweeLightWithNextSlotDto::fromIntervieweeEntity)));
	}

	/**
	 * Retrieves the topic associated with the specified interview process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return the topic of the interview process
	 */
	@GetMapping("/{interviewProcessId}/topic")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<TopicDto> getInterviewProcessTopic(
			@PathVariable("interviewProcessId") UUID interviewProcessId
	) {
		Topic topic = interviewProcessService.getInterviewProcessTopic(interviewProcessId);

		return ResponseEntity.ok(TopicDto.fromTopicEntity(topic));
	}

	/**
	 * Retrieves the details of a specific interview process by its ID.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return the interview process details
	 */
	@GetMapping("/{interviewProcessId}")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<InterviewProcessDto> getInterviewProcess(
			@PathVariable("interviewProcessId") UUID interviewProcessId
	) {
		InterviewProcess interviewProcess = interviewProcessService.getInterviewProcess(interviewProcessId);

		return ResponseEntity.ok(InterviewProcessDto.fromInterviewProcessEntity(interviewProcess));
	}

	/**
	 * Adds, updates, or removes interview time slots for an interview process.
	 *
	 * @param payload the payload containing the interview process ID and slot data
	 * @return the updated list of interview slots
	 */
	@PostMapping("/interview-slots")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<List<InterviewSlotDto>> addInterviewProcessSlots(@RequestBody CreateInterviewSlotsPayload payload) {
		List<InterviewSlot> adaptedInterviewSlots = interviewProcessService.addInterviewSlotsToProcess(
				payload.interviewProcessId(),
				payload.interviewSlots()
		);

		List<InterviewSlotDto> interviewSlotDtos = adaptedInterviewSlots.stream().map((InterviewSlotDto::fromInterviewSlot)).toList();

		return ResponseEntity.ok(interviewSlotDtos );
	}

	/**
	 * Checks whether the specified interview process has been completed.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return true if the interview process is completed, false otherwise
	 */
	@GetMapping("/{interviewProcessId}/completed")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Boolean> isInterviewProcessCompleted(
			@PathVariable("interviewProcessId") UUID interviewProcessId
	) {
		boolean isCompleted = interviewProcessService.isInterviewProcessCompleted(interviewProcessId);
		return ResponseEntity.ok(isCompleted);
	}

	/**
	 * Retrieves interview slots for a process, optionally excluding already booked slots.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param excludeBooked whether to exclude already booked slots
	 * @return the list of interview slots
	 */
	@GetMapping("/{interviewProcessId}/interview-slots")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<InterviewSlotDto>> getInterviewProcessInterviewSlots(
			@PathVariable("interviewProcessId") UUID interviewProcessId,
			@RequestParam(required = false, defaultValue = "false") boolean excludeBooked
	) {
		List<InterviewSlot> interviewSlots = interviewProcessService.getInterviewProcessInterviewSlots(interviewProcessId, excludeBooked);
		List<InterviewSlotDto> interviewSlotDtos = interviewSlots.stream().map((InterviewSlotDto::fromInterviewSlot)).toList();
		return ResponseEntity.ok(interviewSlotDtos);
	}

	/**
	 * Retrieves the details of a specific interviewee within an interview process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param intervieweeId the ID of the interviewee
	 * @return the interviewee details or not found if the interviewee does not belong to the process
	 */
	@GetMapping("/{interviewProcessId}/interviewee/{intervieweeId}")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<IntervieweeDTO> getInterviewee(
			@PathVariable("interviewProcessId") UUID interviewProcessId,
			@PathVariable("intervieweeId") UUID intervieweeId
	) {
		Interviewee interviewee = interviewProcessService.getInterviewee(intervieweeId);
		if (!interviewee.getInterviewProcess().getId().equals(interviewProcessId)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(IntervieweeDTO.fromIntervieweeEntity(interviewee));
	}

	/**
	 * Updates the assessment note and score for a specific interviewee.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param intervieweeId the ID of the interviewee
	 * @param payload the payload containing the assessment note and score
	 * @return the updated interviewee details
	 */
	@PostMapping("/{interviewProcessId}/interviewee/{intervieweeId}")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<IntervieweeDTO> updateInterviewee(
			@PathVariable("interviewProcessId") UUID interviewProcessId,
			@PathVariable("intervieweeId") UUID intervieweeId,
			@RequestBody UpdateIntervieweeAssessmentPayload payload
	) {
		Interviewee interviewee = interviewProcessService.getInterviewee(intervieweeId);
		if (!interviewee.getInterviewProcess().getId().equals(interviewProcessId)) {
			return ResponseEntity.notFound().build();
		}

		Interviewee updatedInterviewee = interviewProcessService.updateIntervieweeAssessment(interviewee, payload.intervieweeNote(), payload.score());
		return ResponseEntity.ok(IntervieweeDTO.fromIntervieweeEntity(updatedInterviewee));
	}

	/**
	 * Sends interview invitations to the specified interviewees.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param payload the payload containing the interviewee IDs to invite
	 * @return the list of invited interviewees
	 */
	@PostMapping("/{interviewProcessId}/invite")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<List<IntervieweeLightWithNextSlotDto>> inviteInterviewees(
			@PathVariable("interviewProcessId") UUID interviewProcessId,
			@RequestBody InviteIntervieweesPayload payload
	) {
		List<Interviewee> interviewee = interviewProcessService.inviteInterviewees(interviewProcessId, payload.intervieweeIds());
		return ResponseEntity.ok(interviewee.stream().map(IntervieweeLightWithNextSlotDto::fromIntervieweeEntity).toList());
	}

	/**
	 * Books an interview slot for the specified interviewee.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param slotId the ID of the interview slot to book
	 * @param payload the payload containing the interviewee user ID
	 * @return the booked interview slot
	 */
	@PutMapping("/{interviewProcessId}/slot/{slotId}/book")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<InterviewSlotDto> bookInterviewSlot(
			@PathVariable("interviewProcessId") UUID interviewProcessId,
			@PathVariable("slotId") UUID slotId,
			@RequestBody BookInterviewSlotPayload payload
			) {
		InterviewSlot interviewSlot = interviewProcessService.bookInterviewSlot(interviewProcessId, slotId, payload.intervieweeUserId());
		return ResponseEntity.ok(InterviewSlotDto.fromInterviewSlot(interviewSlot));
	}

	/**
	 * Cancels an existing interview slot booking.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param slotId the ID of the interview slot to cancel
	 * @return the updated interview slot after cancellation
	 */
	@PutMapping("/{interviewProcessId}/slot/{slotId}/cancel")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<InterviewSlotDto> cancelInterviewSlotBooking(
			@PathVariable("interviewProcessId") UUID interviewProcessId,
			@PathVariable("slotId") UUID slotId
	) {
		InterviewSlot interviewSlot = interviewProcessService.cancelInterviewSlotBooking(interviewProcessId, slotId);
		return ResponseEntity.ok(InterviewSlotDto.fromInterviewSlot(interviewSlot));
	}

	/**
	 * Retrieves paginated applications that can be added to the specified interview process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @return the paginated list of available applications
	 */
	@GetMapping("/{interviewProcessId}/interview-applications")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<PaginationDto<ApplicationInterviewProcessDto>> getInterviewApplications(
			@PathVariable("interviewProcessId") UUID interviewProcessId,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit
	) {

		Page<Application> applications = interviewProcessService.getPossibleApplicationsForProcess(interviewProcessId, page, limit);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(
				applications.map(ApplicationInterviewProcessDto::from)
		));

	}

	/**
	 * Adds new interviewees from applications to an existing interview process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param payload the payload containing the application IDs of new interviewees
	 * @return the updated interview process
	 */
	@PostMapping("/{interviewProcessId}/interviewees")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<InterviewProcessDto> addInterviewees(
			@PathVariable UUID interviewProcessId,
			@RequestBody AddIntervieweesPayload payload
	) {
		Topic topic = interviewProcessService.findById(interviewProcessId).getTopic();

		InterviewProcessDto interviewProcessDto = InterviewProcessDto.fromInterviewProcessEntity(
				interviewProcessService.createInterviewProcess(topic.getId(), payload.intervieweeApplicationIds())
		);
		return ResponseEntity.ok(interviewProcessDto);
	}
}
