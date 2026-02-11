package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.dto.InterviewSlotDto;
import de.tum.cit.aet.thesis.dto.UpcomingInterviewDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.InterviewAssessment;
import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.InterviewProcessRepository;
import de.tum.cit.aet.thesis.repository.IntervieweeRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.property.immutable.ImmutableMethod;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Service for managing interview processes, slots, interviewees, and related business logic. */
@Service
public class InterviewProcessService {
	private final TopicService topicService;
	private final InterviewProcessRepository interviewProcessRepository;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	private final ApplicationService applicationService;
	private final IntervieweeRepository intervieweeRepository;
	private final MailingService mailingService;
	private final CalendarService calendarService;

	private final InternetAddress applicationMail;

	/**
	 * Injects required services, repositories, and the application mail sender address.
	 *
	 * @param topicService the topic service
	 * @param interviewProcessRepository the interview process repository
	 * @param currentUserProviderProvider the provider for the current user context
	 * @param applicationService the application service
	 * @param intervieweeRepository the interviewee repository
	 * @param mailingService the mailing service
	 * @param calendarService the calendar service
	 * @param applicationMail the application mail sender address
	 */
	@Autowired
	public InterviewProcessService(
			TopicService topicService,
			InterviewProcessRepository interviewProcessRepository,
			ObjectProvider<CurrentUserProvider> currentUserProviderProvider,
			ApplicationService applicationService,
			IntervieweeRepository intervieweeRepository,
			MailingService mailingService,
			CalendarService calendarService,
			@Value("${thesis-management.mail.sender}") InternetAddress applicationMail) {
		this.topicService = topicService;
		this.interviewProcessRepository = interviewProcessRepository;
		this.currentUserProviderProvider = currentUserProviderProvider;
		this.applicationService = applicationService;
		this.intervieweeRepository = intervieweeRepository;
		this.mailingService = mailingService;
		this.calendarService = calendarService;
		this.applicationMail = applicationMail;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Finds all interview processes accessible to the current user with pagination and filtering.
	 *
	 * @param searchQuery the search query to filter processes
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction (asc or desc)
	 * @param excludeSupervised whether to exclude supervised processes
	 * @return the paginated list of interview processes
	 */
	public Page<InterviewProcess> findAllMyProcesses(
			String searchQuery,
			int page,
			int limit,
			String sortBy,
			String sortOrder,
			boolean excludeSupervised
	) {
		if (currentUserProvider().getUser().getResearchGroup() == null && !currentUserProvider().isAdmin()) {
			throw new IllegalStateException("Current user is not assigned to any research group.");
		}

		UUID userId = currentUserProvider().isAdmin() ?
				null : currentUserProvider().getUser().getId();
		String searchQueryFilter = (searchQuery == null || searchQuery.isBlank())
				? null
				: "%" + searchQuery.toLowerCase() + "%";

		Sort.Order order = new Sort.Order(
				sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
				sortBy
		);

		Pageable pageable = limit == -1
				? PageRequest.of(0, Integer.MAX_VALUE, Sort.by(order))
				: PageRequest.of(page, limit, Sort.by(order));

		return interviewProcessRepository.searchMyInterviewProcesses(
				userId,
				searchQueryFilter,
				excludeSupervised,
				pageable
		);
	}

	/**
	 * Creates or updates an interview process for a topic and adds interviewees from the given applications.
	 *
	 * @param topicId the ID of the topic for the interview process
	 * @param intervieweeApplicationIds the list of application IDs to add as interviewees
	 * @return the created or updated interview process
	 */
	public InterviewProcess createInterviewProcess(UUID topicId, List<UUID> intervieweeApplicationIds) {

		Topic topic = topicService.findById(topicId);

		if (topic == null) {
			throw new IllegalArgumentException("Topic with the given ID does not exist.");
		}

		currentUserProvider().assertCanAccessResearchGroup(topic.getResearchGroup());

		if (topic.getClosedAt() != null) {
			throw new IllegalStateException("Cannot create interview process for a closed topic.");
		}

		boolean processExists = interviewProcessRepository.existsByTopicId(topicId);

		InterviewProcess interviewProcess = findByTopicId(topicId);
		if (!processExists) {
			interviewProcess.setTopic(topic);
		}

		if (intervieweeApplicationIds != null && !intervieweeApplicationIds.isEmpty()) {
			List<Interviewee> interviewees = interviewProcess.getInterviewees();
			for (UUID intervieweeApplicationId : intervieweeApplicationIds) {
				Application application = applicationService.findById(intervieweeApplicationId);
				if (application == null) {
					throw new IllegalArgumentException("Application with ID " + intervieweeApplicationId + " does not exist.");
				}
				if (!application.getTopic().getId().equals(topicId)) {
					throw new IllegalArgumentException("Application with ID " + intervieweeApplicationId + " does not belong to the specified topic.");
				}

				if (application.getState() != ApplicationState.NOT_ASSESSED) {
					throw new IllegalStateException("Application with ID " + intervieweeApplicationId + " is already in interviewing or assessed.");
				}
				application.setState(ApplicationState.INTERVIEWING);

				Interviewee interviewee = new Interviewee();
				interviewee.setApplication(application);
				interviewee.setInterviewProcess(interviewProcess);

				interviewees.add(interviewee);
			}
		}

		interviewProcessRepository.save(interviewProcess);

		return interviewProcess;
	}

	/**
	 * Finds an interview process by its ID or throws a ResourceNotFoundException.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return the interview process
	 */
	public InterviewProcess findById(UUID interviewProcessId) {
		return interviewProcessRepository.findById(interviewProcessId)
				.orElseThrow(() -> new ResourceNotFoundException(
						String.format("InterviewProcess with id %s not found.", interviewProcessId)));
	}

	/**
	 * Finds an interview process by topic ID, or returns a new empty process if none exists.
	 *
	 * @param topicId the ID of the topic
	 * @return the interview process for the topic, or a new empty process
	 */
	public InterviewProcess findByTopicId(UUID topicId) {
		InterviewProcess interviewProcess = interviewProcessRepository.findByTopicId(topicId);
		if (interviewProcess == null) {
			interviewProcess = new InterviewProcess();
		}
		return interviewProcess;
	}

	/**
	 * Checks whether an interview process exists for the given topic ID.
	 *
	 * @param topicId the ID of the topic
	 * @return true if an interview process exists for the topic, false otherwise
	 */
	public boolean existsByTopicId(UUID topicId) {
		return interviewProcessRepository.existsByTopicId(topicId);
	}

	/**
	 * Retrieves all future booked interview slots across the current user's interview processes.
	 *
	 * @return the list of upcoming interviews for the current user
	 */
	public List<UpcomingInterviewDto> getUpcomingInterviewsForCurrentUser() {
		if (currentUserProvider().getUser().getResearchGroup() == null && !currentUserProvider().isAdmin()) {
			throw new IllegalStateException("Current user is not assigned to any research group.");
		}

		List<InterviewProcess> myProcesses = findAllMyProcesses("", 0, Integer.MAX_VALUE, "completed", "asc", false).stream().toList();

		Instant now = Instant.now();

		List<UpcomingInterviewDto> upcomingInterviewDtos = new ArrayList<>();

		for (InterviewProcess interviewProcess : myProcesses) {
			for (InterviewSlot slot : interviewProcess.getSlots()) {
				if (slot.getStartDate().isAfter(now) && slot.getInterviewee() != null) {
					UpcomingInterviewDto dto = UpcomingInterviewDto.fromInterviewSlot(interviewProcess, slot);
					upcomingInterviewDtos.add(dto);
				}
			}
		}

		return upcomingInterviewDtos;
	}

	/**
	 * Retrieves a paginated and filtered list of interviewees for a specific interview process.
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
	public Page<Interviewee> getInterviewProcessInterviewees(
			UUID interviewProcessId,
			String searchQuery,
			int page,
			int limit,
			String sortBy,
			String sortOrder,
			String state
	) {
		if (currentUserProvider().getUser().getResearchGroup() == null && !currentUserProvider().isAdmin()) {
			throw new IllegalStateException("Current user is not assigned to any research group.");
		}

		InterviewProcess interviewProcess = findById(interviewProcessId);

		currentUserProvider().assertCanAccessResearchGroup(interviewProcess.getTopic().getResearchGroup());

		String searchQueryFilter = (searchQuery == null || searchQuery.isBlank())
				? null
				: "%" + searchQuery.toLowerCase() + "%";

		String stateFilter = (state == null || state.isBlank()) ? null : state;

		Sort.Order order = new Sort.Order(
				sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
				sortBy
		);

	return intervieweeRepository.findAllInterviewees( interviewProcessId, searchQueryFilter, stateFilter, PageRequest.of(page, limit <= 0 ? Integer.MAX_VALUE : limit, Sort.by(order)) );
	}

	/**
	 * Synchronizes interview slots for a process by adding, updating, or removing slots as needed.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param interviewSlots the list of interview slot DTOs to synchronize
	 * @return the updated list of interview slots
	 */
	public List<InterviewSlot> addInterviewSlotsToProcess(UUID interviewProcessId, List<InterviewSlotDto> interviewSlots) {
		if (currentUserProvider().getUser().getResearchGroup() == null && !currentUserProvider().isAdmin()) {
			throw new IllegalStateException("Current user is not assigned to any research group.");
		}

		if (assertNoOverlappingSlots(interviewSlots)) {
			throw new IllegalStateException("Interview slots cannot overlap.");
		}

		InterviewProcess interviewProcess = findById(interviewProcessId);

		currentUserProvider().assertCanAccessResearchGroup(interviewProcess.getTopic().getResearchGroup());


		if (interviewProcess.getSlots() == null) {
			interviewProcess.setSlots(new ArrayList<>());
		}

		List<InterviewSlot> existing = interviewProcess.getSlots();

		// Delete all existing slots if incoming is null (modify in-place)
		if (interviewSlots == null) {
			existing.clear();
			interviewProcessRepository.save(interviewProcess);
			return existing;
		}

		Map<UUID, InterviewSlot> existingById = existing.stream()
				.filter(s -> s.getId() != null)
				.collect(Collectors.toMap(InterviewSlot::getId, Function.identity()));

		Set<UUID> incomingIds = interviewSlots.stream()
				.map(InterviewSlotDto::getSlotId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// Delete no longer existing slots (only if not booked) — modify the persistent collection via iterator
		Iterator<InterviewSlot> it = existing.iterator();
		while (it.hasNext()) {
			InterviewSlot s = it.next();
			if (s.getId() != null && !incomingIds.contains(s.getId()) && s.getInterviewee() == null) {
				it.remove();
			}
		}

		// Update existing or add new slots (operate on the same collection)
		for (InterviewSlotDto incoming : interviewSlots) {
			if (incoming.getSlotId() != null && existingById.containsKey(incoming.getSlotId())) {
				InterviewSlot toUpdate = existingById.get(incoming.getSlotId());

				// Only allow updating unbooked slots
				if (toUpdate.getInterviewee() == null) {
					toUpdate.setStartDate(incoming.startDate());
					toUpdate.setEndDate(incoming.endDate());
					toUpdate.setLocation(incoming.location());
					toUpdate.setStreamLink(incoming.streamUrl());
				} else {
					//TODO: INSTEAD ALLOW UPDATING BOOKED SLOTS WITH RE-SENDING EMAILS
					toUpdate.setLocation(incoming.location());
					toUpdate.setStreamLink(incoming.streamUrl());
				}
			} else {
				// Create new slot and add to the persistent collection
				InterviewSlot newSlot = new InterviewSlot();
				newSlot.setInterviewProcess(interviewProcess);
				newSlot.setStartDate(incoming.startDate());
				newSlot.setEndDate(incoming.endDate());
				newSlot.setLocation(incoming.location());
				newSlot.setStreamLink(incoming.streamUrl());

				existing.add(newSlot);
			}
		}

		interviewProcessRepository.save(interviewProcess);

		return interviewProcess.getSlots();
	}

	private boolean assertNoOverlappingSlots(List<InterviewSlotDto> slots) {
		List<InterviewSlotDto> sortedSlots = slots.stream()
				.sorted(Comparator.comparing(InterviewSlotDto::getStartDate))
				.toList();

		for (int i = 0; i < sortedSlots.size() - 1; i++) {
			InterviewSlotDto current = sortedSlots.get(i);
			InterviewSlotDto next = sortedSlots.get(i + 1);

			if (current.getEndDate().compareTo(next.getStartDate()) <= 0) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Retrieves interview slots for a process, optionally filtering out booked and past slots.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param excludeBooked whether to exclude already booked and past slots
	 * @return the list of interview slots
	 */
	public List<InterviewSlot> getInterviewProcessInterviewSlots(
			UUID interviewProcessId,
			boolean excludeBooked
	) {
		InterviewProcess interviewProcess = findById(interviewProcessId);

		if (currentUserProvider().getUser().getResearchGroup() == null && !currentUserProvider().isAdmin() && !userIsInvitedToInterviewee(interviewProcess)) {
			throw new IllegalStateException("Current user is not allowed to access the interview slots of this interview process.");
		}

		currentUserProvider().assertCanAccessResearchGroup(interviewProcess.getTopic().getResearchGroup());

		List<InterviewSlot> slots =  interviewProcess.getSlots() == null
				? new ArrayList<>()
				: interviewProcess.getSlots();

		Instant now = Instant.now();

		return excludeBooked ? slots.stream()
				.filter(slot -> slot.getInterviewee() == null)
				.filter(slot -> slot.getStartDate() != null && slot.getStartDate().isAfter(now))
				.toList() : slots;
	}

	private Boolean userIsInvitedToInterviewee(InterviewProcess interviewProcess) {
		UUID currentUserId = currentUserProvider().getUser().getId();

		for (Interviewee interviewee : interviewProcess.getInterviewees()) {
			if (interviewee.getApplication().getUser().getId().equals(currentUserId) && interviewee.getLastInvited() != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds an interviewee by ID and verifies the current user has access to their research group.
	 *
	 * @param intervieweeId the ID of the interviewee
	 * @return the interviewee
	 */
	public Interviewee getInterviewee(UUID intervieweeId) {
		Interviewee interviewee = intervieweeRepository.findById(intervieweeId)
				.orElseThrow(() -> new ResourceNotFoundException(
						String.format("Interviewee with id %s not found.", intervieweeId)));
		currentUserProvider().assertCanAccessResearchGroup(interviewee.getApplication().getResearchGroup());
		return interviewee;
	}

	/**
	 * Updates the interview assessment note and score for an interviewee.
	 *
	 * @param interviewee the interviewee to update
	 * @param intervieweeNote the assessment note
	 * @param score the assessment score
	 * @return the updated interviewee
	 */
	public Interviewee updateIntervieweeAssessment(Interviewee interviewee, String intervieweeNote, int score) {
		currentUserProvider().assertCanAccessResearchGroup(interviewee.getApplication().getResearchGroup());

		if (score >= 0) {
			interviewee.setScore(score);
		} else {
			interviewee.setScore(null);
		}

		if (intervieweeNote != null) {
		InterviewAssessment assesment = interviewee.getAssessments().isEmpty() ? null : interviewee.getAssessments().getFirst();
		if (assesment == null) {
			assesment = new InterviewAssessment();
			assesment.setInterviewee(interviewee);
			interviewee.getAssessments().add(assesment);
		}
		assesment.setInterviewNote(intervieweeNote);
		}

		return intervieweeRepository.save(interviewee);
	}

	/**
	 * Sends interview invitation emails to the specified interviewees and updates their invitation timestamps.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param intervieweeIds the list of interviewee IDs to invite
	 * @return the list of invited interviewees
	 */
	public List<Interviewee> inviteInterviewees(UUID interviewProcessId, List<UUID> intervieweeIds) {
		if (intervieweeIds == null || intervieweeIds.isEmpty()) {
			throw new IllegalStateException("Interviewee Ids cannot be null or empty.");
		}

		InterviewProcess interviewProcess = findById(interviewProcessId);
		currentUserProvider().assertCanAccessResearchGroup(interviewProcess.getTopic().getResearchGroup());

		List<Interviewee> interviewees = intervieweeRepository.findAllById(intervieweeIds);
		for (Interviewee interviewee : interviewees) {
			if (intervieweeIds.contains(interviewee.getIntervieweeId())) {
				Boolean firstInvitation = interviewee.getLastInvited() == null;
				interviewee.setLastInvited(new Date().toInstant());
				intervieweeRepository.save(interviewee);

				mailingService.sendInterviewInvitationEmail(interviewee, firstInvitation);
			}
		}

		return interviewees;
	}

	/**
	 * Books an available interview slot for the specified interviewee and sends a confirmation email.
	 *
	 * @param processID the ID of the interview process
	 * @param slotId the ID of the interview slot to book
	 * @param intervieweeUserId the user ID of the interviewee booking the slot
	 * @return the booked interview slot
	 */
	public InterviewSlot bookInterviewSlot(UUID processID, UUID slotId,UUID intervieweeUserId) {
		if (intervieweeUserId == null) {
			throw new IllegalStateException("No Interviewee Id provided.");
		}
		InterviewProcess interviewProcess = findById(processID);

		if (currentUserProvider().getUser().getResearchGroup() == null && !currentUserProvider().isAdmin() && !userIsInvitedToInterviewee(interviewProcess)) {
			throw new IllegalStateException("Current user is not allowed to access the interview slots of this interview process.");
		}

		if (!interviewProcess.getSlots().stream().map(InterviewSlot::getId).toList().contains(slotId)) {
			throw new ResourceNotFoundException(String.format("Slot with id %s does not belong to the provided process.", slotId));
		}

		if (interviewProcess.getSlots().stream().anyMatch((slot) -> slot.getInterviewee() != null && slot.getInterviewee().getApplication().getUser().getId().equals(intervieweeUserId))) {
			throw new IllegalStateException("Interviewee has already booked a slot for this interview process.");
		}

		Interviewee interviewee = interviewProcess.getInterviewees().stream()
				.filter(ie -> ie.getApplication().getUser().getId().equals(intervieweeUserId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(String.format(
						"Interviewee with user id %s not found in the provided process.",
						intervieweeUserId)));
		InterviewSlot slot = interviewProcess.getSlots().stream()
				.filter(s -> s.getId().equals(slotId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(String.format("Slot with id %s not found.", slotId)));

		if (slot.getInterviewee() != null) {
			throw new IllegalStateException("Slot is already booked.");
		}

		slot.setInterviewee(interviewee);
		interviewProcessRepository.save(interviewProcess);

		mailingService.sendInterviewSlotConfirmationEmail(slot, "BOOK");

		return slot;
	}

	/**
	 * Cancels an interview slot booking and sends a cancellation notification email.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param slotId the ID of the interview slot to cancel
	 * @return the updated interview slot after cancellation
	 */
	public InterviewSlot cancelInterviewSlotBooking(UUID interviewProcessId, UUID slotId) {
		InterviewProcess interviewProcess = findById(interviewProcessId);

		InterviewSlot slot = interviewProcess.getSlots().stream()
				.filter(s -> s.getId().equals(slotId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(String.format("Slot with id %s not found.", slotId)));

		if (slot.getInterviewee() == null) {
			throw new IllegalStateException("Slot is not booked.");
		}

		if (slot.getInterviewee().getApplication().getUser().getId() != currentUserProvider().getUser().getId()) {
			currentUserProvider().assertCanAccessResearchGroup(interviewProcess.getTopic().getResearchGroup());
		}

		if (!interviewProcess.getSlots().stream().map(InterviewSlot::getId).toList().contains(slotId)) {
			throw new ResourceNotFoundException(String.format("Slot with id %s does not belong to the provided process.", slotId));
		}

		mailingService.sendInterviewSlotConfirmationEmail(slot, "CANCEL");
		slot.setInterviewee(null);
		interviewProcessRepository.save(interviewProcess);

		return slot;
	}

	/**
	 * Retrieves paginated unassessed applications that can be added to the specified interview process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @return the paginated list of available applications
	 */
	public Page<Application> getPossibleApplicationsForProcess(UUID interviewProcessId, int page, int limit) {
		InterviewProcess interviewProcess = findById(interviewProcessId);
		currentUserProvider().assertCanAccessResearchGroup(interviewProcess.getTopic().getResearchGroup());

		return applicationService.getAll(
				null,
				null,
				null,
				new ApplicationState[]{ApplicationState.NOT_ASSESSED},
				null,
				new String[]{interviewProcess.getTopic().getId().toString()},
				null,
				false,
				page,
				limit <= 0 ? Integer.MAX_VALUE : limit,
				"createdAt",
				"desc"
		);
	}

	/**
	 * Generates an iCal calendar containing all interview slots for the specified user.
	 *
	 * @param userId the ID of the user
	 * @return the iCal calendar with interview slot events
	 */
	public Calendar getInterviewCalendarForUser(UUID userId) {
		String calendarProdId = "-//Thesis Management//Thesis Interviews//EN";
		Calendar calendar = calendarService.createEmptyCalendar(calendarProdId);

		calendar.add(ImmutableMethod.PUBLISH);

		List<InterviewSlot> slots = interviewProcessRepository.findAllMyInterviewSlots(userId);

		for (InterviewSlot slot : slots) {
			calendar.add(calendarService.createVEvent(slot.getId().toString(), createInterviewSlotCalendarEvent(slot)));
		}

		return calendar;
	}

	private CalendarService.CalendarEvent createInterviewSlotCalendarEvent(InterviewSlot slot) {
		String thesisTitle = slot.getInterviewProcess().getTopic().getTitle();
		String title = "Interview Slot \"" + thesisTitle + "\"";
		String location = slot.getLocation();
		String streamUrl = slot.getStreamLink();
		String intervieweeName = slot.getInterviewee() == null
				? "Not booked"
				: slot.getInterviewee().getApplication().getUser().getFirstName()
						+ " " + slot.getInterviewee().getApplication().getUser().getLastName();

		return new CalendarService.CalendarEvent(
				title,
				location == null || location.isBlank() ? streamUrl : location,
				"Title: " + thesisTitle + "\n" +
						(streamUrl != null && !streamUrl.isBlank() ? "Stream URL: " + streamUrl + "\n" : "") + "\n" +
						"Interviewee: " + intervieweeName + "\n\n",
				slot.getStartDate(),
				slot.getEndDate(),
				this.applicationMail,
				slot.getInterviewProcess().getTopic().getRoles().stream().map(role -> role.getUser().getEmail()).toList(),
				new ArrayList<>()
		);
	}

	/**
	 * Retrieves the topic associated with the specified interview process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return the topic of the interview process
	 */
	public Topic getInterviewProcessTopic(UUID interviewProcessId) {
		InterviewProcess interviewProcess = findById(interviewProcessId);
		return interviewProcess.getTopic();
	}

	/**
	 * Retrieves the interview slot booked by the current user in the specified process.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return the interview slot booked by the current user
	 */
	public InterviewSlot getMyBookedSlot(UUID interviewProcessId) {
		InterviewProcess interviewProcess = findById(interviewProcessId);

		UUID currentUserId = currentUserProvider().getUser().getId();

		for (InterviewSlot slot : interviewProcess.getSlots()) {
			if (slot.getInterviewee() != null && slot.getInterviewee().getApplication().getUser().getId().equals(currentUserId)) {
				return slot;
			}
		}

		throw new ResourceNotFoundException("No booked slot found for the current user in the specified interview process.");
	}

	/**
	 * Checks whether the specified interview process has been marked as completed.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return true if the interview process is completed, false otherwise
	 */
	public Boolean isInterviewProcessCompleted(UUID interviewProcessId) {
		InterviewProcess interviewProcess = findById(interviewProcessId);

		return interviewProcess.isCompleted();
	}

	/**
	 * Retrieves an interview process by ID and verifies the current user has research group access.
	 *
	 * @param interviewProcessId the ID of the interview process
	 * @return the interview process
	 */
	public InterviewProcess getInterviewProcess(UUID interviewProcessId) {
		InterviewProcess interviewProcess = findById(interviewProcessId);
		currentUserProvider().assertCanAccessResearchGroup(interviewProcess.getTopic().getResearchGroup());

		return interviewProcess;
	}
}
