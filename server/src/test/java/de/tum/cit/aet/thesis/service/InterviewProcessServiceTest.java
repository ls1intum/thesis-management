package de.tum.cit.aet.thesis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.dto.InterviewSlotDto;
import de.tum.cit.aet.thesis.dto.UpcomingInterviewDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.InterviewProcessRepository;
import de.tum.cit.aet.thesis.repository.IntervieweeRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.mail.internet.InternetAddress;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InterviewProcessServiceTest {

	@Mock
	private TopicService topicService;
	@Mock
	private InterviewProcessRepository interviewProcessRepository;
	@Mock
	private ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	@Mock
	private CurrentUserProvider currentUserProvider;
	@Mock
	private ApplicationService applicationService;
	@Mock
	private IntervieweeRepository intervieweeRepository;
	@Mock
	private MailingService mailingService;
	@Mock
	private CalendarService calendarService;

	private InterviewProcessService interviewProcessService;

	@BeforeEach
	void setUp() throws Exception {
		interviewProcessService = new InterviewProcessService(
				topicService,
				interviewProcessRepository,
				currentUserProviderProvider,
				applicationService,
				intervieweeRepository,
				mailingService,
				calendarService,
				new InternetAddress("noreply@example.com")
		);

		lenient().when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
	}

	@Test
	void findAllMyProcesses_WhenUserHasNoResearchGroupAndIsNotAdmin_Throws() {
		User user = new User();
		when(currentUserProvider.getUser()).thenReturn(user);
		when(currentUserProvider.isAdmin()).thenReturn(false);

		assertThrows(IllegalStateException.class, () ->
				interviewProcessService.findAllMyProcesses("q", 0, 20, "completed", "asc", false)
		);
	}

	@Test
	void findAllMyProcesses_WithValidArguments_CallsRepositoryWithExpectedFilterAndPageable() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setResearchGroup(new ResearchGroup());
		when(currentUserProvider.getUser()).thenReturn(user);
		when(currentUserProvider.isAdmin()).thenReturn(false);

		InterviewProcess process = new InterviewProcess();
		process.setTopic(new Topic());
		Page<InterviewProcess> expectedPage = new PageImpl<>(List.of(process));
		when(interviewProcessRepository.searchMyInterviewProcesses(
				eq(user.getId()),
				eq("%thesis%"),
				eq(true),
				any(Pageable.class)
		)).thenReturn(expectedPage);

		Page<InterviewProcess> result = interviewProcessService.findAllMyProcesses("thesis", 1, 5, "completed", "asc", true);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());

		ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
		ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Boolean> excludeCaptor = ArgumentCaptor.forClass(Boolean.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		verify(interviewProcessRepository).searchMyInterviewProcesses(
				userIdCaptor.capture(),
				searchCaptor.capture(),
				excludeCaptor.capture(),
				pageableCaptor.capture()
		);

		assertEquals(user.getId(), userIdCaptor.getValue());
		assertEquals("%thesis%", searchCaptor.getValue());
		assertTrue(excludeCaptor.getValue());
		assertEquals(1, pageableCaptor.getValue().getPageNumber());
		assertEquals(5, pageableCaptor.getValue().getPageSize());
		assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("completed").getDirection());
	}

	@Test
	void createInterviewProcess_WithValidInput_CreatesIntervieweesAndUpdatesApplicationState() {
		UUID topicId = UUID.randomUUID();
		UUID applicationId = UUID.randomUUID();

		ResearchGroup group = new ResearchGroup();
		group.setId(UUID.randomUUID());
		Topic topic = new Topic();
		topic.setId(topicId);
		topic.setResearchGroup(group);
		topic.setClosedAt(null);

		Application application = new Application();
		application.setId(applicationId);
		application.setTopic(topic);
		application.setState(ApplicationState.NOT_ASSESSED);

		when(topicService.findById(topicId)).thenReturn(topic);
		when(interviewProcessRepository.existsByTopicId(topicId)).thenReturn(false);
		when(interviewProcessRepository.findByTopicId(topicId)).thenReturn(null);
		when(applicationService.findById(applicationId)).thenReturn(application);
		when(interviewProcessRepository.save(any(InterviewProcess.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InterviewProcess created = interviewProcessService.createInterviewProcess(topicId, List.of(applicationId));

		assertNotNull(created);
		assertEquals(topic, created.getTopic());
		assertEquals(1, created.getInterviewees().size());
		assertEquals(ApplicationState.INTERVIEWING, application.getState());
		verify(interviewProcessRepository).save(created);
	}

	@Test
	void createInterviewProcess_WithClosedTopic_Throws() {
		UUID topicId = UUID.randomUUID();
		Topic topic = new Topic();
		topic.setId(topicId);
		topic.setResearchGroup(new ResearchGroup());
		topic.setClosedAt(Instant.now());
		when(topicService.findById(topicId)).thenReturn(topic);

		assertThrows(IllegalStateException.class, () ->
				interviewProcessService.createInterviewProcess(topicId, List.of(UUID.randomUUID()))
		);
		verify(interviewProcessRepository, never()).save(any());
	}

	@Test
	void addInterviewSlotsToProcess_WithOverlappingSlots_Throws() {
		User user = new User();
		user.setResearchGroup(new ResearchGroup());
		when(currentUserProvider.getUser()).thenReturn(user);

		Instant start = Instant.now().plusSeconds(3600);
		List<InterviewSlotDto> slots = List.of(
				new InterviewSlotDto(null, start, start.plusSeconds(3600), null, "Room 1", null),
				new InterviewSlotDto(null, start.plusSeconds(1800), start.plusSeconds(5400), null, "Room 2", null)
		);

		assertThrows(IllegalStateException.class, () ->
				interviewProcessService.addInterviewSlotsToProcess(UUID.randomUUID(), slots)
		);
		verify(interviewProcessRepository, never()).findById(any());
	}

	@Test
	void bookInterviewSlot_WithValidData_BooksSlotAndSendsMail() {
		UUID processId = UUID.randomUUID();
		UUID slotId = UUID.randomUUID();
		UUID intervieweeUserId = UUID.randomUUID();

		when(currentUserProvider.isAdmin()).thenReturn(true);
		when(currentUserProvider.getUser()).thenReturn(new User());

		User intervieweeUser = new User();
		intervieweeUser.setId(intervieweeUserId);
		Application application = new Application();
		application.setUser(intervieweeUser);

		Interviewee interviewee = new Interviewee();
		interviewee.setApplication(application);

		InterviewSlot slot = new InterviewSlot();
		slot.setId(slotId);
		slot.setInterviewee(null);

		Topic topic = new Topic();
		topic.setResearchGroup(new ResearchGroup());
		InterviewProcess process = new InterviewProcess();
		process.setId(processId);
		process.setTopic(topic);
		process.setSlots(List.of(slot));
		process.setInterviewees(List.of(interviewee));

		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));
		when(interviewProcessRepository.save(any(InterviewProcess.class))).thenReturn(process);

		InterviewSlot bookedSlot = interviewProcessService.bookInterviewSlot(processId, slotId, intervieweeUserId);

		assertNotNull(bookedSlot);
		assertEquals(interviewee, bookedSlot.getInterviewee());
		verify(interviewProcessRepository).save(process);
		verify(mailingService).sendInterviewSlotConfirmationEmail(slot, "BOOK");
	}

	@Test
	void getMyBookedSlot_WhenCurrentUserHasNoBooking_ThrowsResourceNotFound() {
		UUID processId = UUID.randomUUID();
		UUID currentUserId = UUID.randomUUID();

		User currentUser = new User();
		currentUser.setId(currentUserId);
		when(currentUserProvider.getUser()).thenReturn(currentUser);

		User otherUser = new User();
		otherUser.setId(UUID.randomUUID());
		Application application = new Application();
		application.setUser(otherUser);

		Interviewee interviewee = new Interviewee();
		interviewee.setApplication(application);

		InterviewSlot bookedByOther = new InterviewSlot();
		bookedByOther.setInterviewee(interviewee);

		InterviewProcess process = new InterviewProcess();
		process.setSlots(List.of(bookedByOther));

		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		assertThrows(ResourceNotFoundException.class, () -> interviewProcessService.getMyBookedSlot(processId));
	}

	@Test
	void findById_WhenExists_ReturnsEntity() {
		UUID id = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		process.setId(id);
		when(interviewProcessRepository.findById(id)).thenReturn(Optional.of(process));

		InterviewProcess result = interviewProcessService.findById(id);

		assertEquals(id, result.getId());
	}

	@Test
	void findById_WhenMissing_ThrowsResourceNotFound() {
		UUID id = UUID.randomUUID();
		when(interviewProcessRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> interviewProcessService.findById(id));
	}

	@Test
	void findByTopicId_WhenProcessExists_ReturnsExisting() {
		UUID topicId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		process.setId(UUID.randomUUID());
		when(interviewProcessRepository.findByTopicId(topicId)).thenReturn(process);

		InterviewProcess result = interviewProcessService.findByTopicId(topicId);

		assertEquals(process, result);
	}

	@Test
	void findByTopicId_WhenMissing_ReturnsNewProcess() {
		UUID topicId = UUID.randomUUID();
		when(interviewProcessRepository.findByTopicId(topicId)).thenReturn(null);

		InterviewProcess result = interviewProcessService.findByTopicId(topicId);

		assertNotNull(result);
		assertNotNull(result.getInterviewees());
		assertNotNull(result.getSlots());
	}

	@Test
	void existsByTopicId_DelegatesToRepository() {
		UUID topicId = UUID.randomUUID();
		when(interviewProcessRepository.existsByTopicId(topicId)).thenReturn(true);

		assertTrue(interviewProcessService.existsByTopicId(topicId));
	}

	@Test
	void getUpcomingInterviewsForCurrentUser_FiltersOnlyFutureBookedSlots() {
		User currentUser = createUser("Current");
		currentUser.setResearchGroup(new ResearchGroup());
		when(currentUserProvider.getUser()).thenReturn(currentUser);
		when(currentUserProvider.isAdmin()).thenReturn(false);

		InterviewProcess process = new InterviewProcess();
		process.setId(UUID.randomUUID());
		Topic topic = new Topic();
		topic.setTitle("Topic A");
		process.setTopic(topic);

		Interviewee interviewee = createInterviewee(UUID.randomUUID(), UUID.randomUUID());
		InterviewSlot futureBooked = createSlot(process, Instant.now().plusSeconds(7200), Instant.now().plusSeconds(9000));
		futureBooked.setInterviewee(interviewee);
		InterviewSlot pastBooked = createSlot(process, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
		pastBooked.setInterviewee(interviewee);
		InterviewSlot futureUnbooked = createSlot(process, Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400));
		process.setSlots(List.of(futureBooked, pastBooked, futureUnbooked));
		process.setInterviewees(List.of(interviewee));

		when(interviewProcessRepository.searchMyInterviewProcesses(
				eq(currentUser.getId()),
				eq(null),
				eq(false),
				any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(process)));

		List<UpcomingInterviewDto> result = interviewProcessService.getUpcomingInterviewsForCurrentUser();

		assertEquals(1, result.size());
		assertEquals(process.getId(), result.getFirst().interviewProcessId());
	}

	@Test
	void getInterviewProcessInterviewees_WithBlankFilters_PassesNullFilters() {
		User currentUser = createUser("Advisor");
		ResearchGroup group = new ResearchGroup();
		group.setId(UUID.randomUUID());
		currentUser.setResearchGroup(group);
		when(currentUserProvider.getUser()).thenReturn(currentUser);

		UUID processId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		Topic topic = new Topic();
		topic.setResearchGroup(group);
		process.setTopic(topic);
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));
		when(intervieweeRepository.findAllInterviewees(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

		interviewProcessService.getInterviewProcessInterviewees(processId, " ", 0, 0, "lastInvited", "desc", "");

		ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(intervieweeRepository).findAllInterviewees(eq(processId), searchCaptor.capture(), stateCaptor.capture(), pageableCaptor.capture());
		assertEquals(null, searchCaptor.getValue());
		assertEquals(null, stateCaptor.getValue());
		assertEquals(Integer.MAX_VALUE, pageableCaptor.getValue().getPageSize());
	}

	@Test
	void addInterviewSlotsToProcess_WithNullIncoming_ThrowsNullPointerException() {
		User currentUser = createUser("Advisor");
		currentUser.setResearchGroup(new ResearchGroup());
		when(currentUserProvider.getUser()).thenReturn(currentUser);

		assertThrows(NullPointerException.class, () ->
				interviewProcessService.addInterviewSlotsToProcess(UUID.randomUUID(), null)
		);
		verify(interviewProcessRepository, never()).findById(any());
	}

	@Test
	void addInterviewSlotsToProcess_UpdatesAddsAndDeletesUnbookedSlots() {
		User currentUser = createUser("Advisor");
		currentUser.setResearchGroup(new ResearchGroup());
		when(currentUserProvider.getUser()).thenReturn(currentUser);

		UUID processId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		Topic topic = new Topic();
		topic.setResearchGroup(new ResearchGroup());
		process.setTopic(topic);
		process.setSlots(new ArrayList<>());

		InterviewSlot updatable = createSlot(process, Instant.now().plusSeconds(1000), Instant.now().plusSeconds(2000));
		updatable.setId(UUID.randomUUID());
		InterviewSlot deletable = createSlot(process, Instant.now().plusSeconds(3000), Instant.now().plusSeconds(4000));
		deletable.setId(UUID.randomUUID());
		InterviewSlot bookedKeep = createSlot(process, Instant.now().plusSeconds(5000), Instant.now().plusSeconds(6000));
		bookedKeep.setId(UUID.randomUUID());
		bookedKeep.setInterviewee(createInterviewee(UUID.randomUUID(), UUID.randomUUID()));
		process.getSlots().addAll(List.of(updatable, deletable, bookedKeep));

		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		InterviewSlotDto updatedDto = new InterviewSlotDto(
				updatable.getId(),
				Instant.now().plusSeconds(7000),
				Instant.now().plusSeconds(8000),
				null,
				"Updated Room",
				"meet.example.com/new"
		);
		InterviewSlotDto newDto = new InterviewSlotDto(
				null,
				Instant.now().plusSeconds(9000),
				Instant.now().plusSeconds(10000),
				null,
				"New Room",
				"https://meet.example.com/newer"
		);

		List<InterviewSlot> result = interviewProcessService.addInterviewSlotsToProcess(processId, List.of(updatedDto, newDto));

		assertEquals(3, result.size());
		assertFalse(result.stream().anyMatch(s -> deletable.getId().equals(s.getId())));
		assertTrue(result.stream().anyMatch(s -> "Updated Room".equals(s.getLocation())));
		assertTrue(result.stream().anyMatch(s -> "New Room".equals(s.getLocation())));
		verify(interviewProcessRepository).save(process);
	}

	@Test
	void getInterviewProcessInterviewSlots_WhenExcludeBooked_ReturnsOnlyFutureUnbooked() {
		UUID processId = UUID.randomUUID();
		User currentUser = createUser("Advisor");
		currentUser.setResearchGroup(new ResearchGroup());
		when(currentUserProvider.getUser()).thenReturn(currentUser);

		InterviewProcess process = new InterviewProcess();
		Topic topic = new Topic();
		topic.setResearchGroup(new ResearchGroup());
		process.setTopic(topic);

		InterviewSlot futureUnbooked = createSlot(process, Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400));
		InterviewSlot futureBooked = createSlot(process, Instant.now().plusSeconds(7200), Instant.now().plusSeconds(9000));
		futureBooked.setInterviewee(createInterviewee(UUID.randomUUID(), UUID.randomUUID()));
		InterviewSlot pastUnbooked = createSlot(process, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
		process.setSlots(List.of(futureUnbooked, futureBooked, pastUnbooked));

		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		List<InterviewSlot> result = interviewProcessService.getInterviewProcessInterviewSlots(processId, true);
		assertEquals(1, result.size());
		assertEquals(futureUnbooked, result.getFirst());
	}

	@Test
	void getInterviewee_WhenExists_ReturnsInterviewee() {
		UUID intervieweeId = UUID.randomUUID();
		Interviewee interviewee = createInterviewee(UUID.randomUUID(), UUID.randomUUID());
		when(intervieweeRepository.findById(intervieweeId)).thenReturn(Optional.of(interviewee));

		Interviewee result = interviewProcessService.getInterviewee(intervieweeId);

		assertEquals(interviewee, result);
		verify(currentUserProvider).assertCanAccessResearchGroup(interviewee.getApplication().getResearchGroup());
	}

	@Test
	void updateIntervieweeAssessment_WithNegativeScore_ClearsScoreAndCreatesAssessment() {
		Interviewee interviewee = createInterviewee(UUID.randomUUID(), UUID.randomUUID());
		when(intervieweeRepository.save(any(Interviewee.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Interviewee result = interviewProcessService.updateIntervieweeAssessment(interviewee, "Strong candidate", -1);

		assertEquals(null, result.getScore());
		assertEquals(1, result.getAssessments().size());
		assertEquals("Strong candidate", result.getAssessments().getFirst().getInterviewNote());
	}

	@Test
	void inviteInterviewees_WithEmptyIds_Throws() {
		assertThrows(IllegalStateException.class, () -> interviewProcessService.inviteInterviewees(UUID.randomUUID(), List.of()));
	}

	@Test
	void inviteInterviewees_WithValidIds_SendsEmailsAndUpdatesTimestamps() {
		UUID processId = UUID.randomUUID();
		UUID intervieweeAId = UUID.randomUUID();
		UUID intervieweeBId = UUID.randomUUID();

		InterviewProcess process = new InterviewProcess();
		Topic topic = new Topic();
		topic.setResearchGroup(new ResearchGroup());
		process.setTopic(topic);
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		Interviewee first = createInterviewee(intervieweeAId, UUID.randomUUID());
		first.setLastInvited(null);
		Interviewee second = createInterviewee(intervieweeBId, UUID.randomUUID());
		second.setLastInvited(Instant.now().minusSeconds(1000));
		when(intervieweeRepository.findAllById(List.of(intervieweeAId, intervieweeBId))).thenReturn(List.of(first, second));

		List<Interviewee> result = interviewProcessService.inviteInterviewees(processId, List.of(intervieweeAId, intervieweeBId));

		assertEquals(2, result.size());
		assertNotNull(first.getLastInvited());
		assertNotNull(second.getLastInvited());
		verify(mailingService).sendInterviewInvitationEmail(first, true);
		verify(mailingService).sendInterviewInvitationEmail(second, false);
		verify(intervieweeRepository, times(2)).save(any(Interviewee.class));
	}

	@Test
	void bookInterviewSlot_WithNullIntervieweeUserId_Throws() {
		assertThrows(IllegalStateException.class, () ->
				interviewProcessService.bookInterviewSlot(UUID.randomUUID(), UUID.randomUUID(), null)
		);
	}

	@Test
	void bookInterviewSlot_WhenSlotNotPartOfProcess_Throws() {
		UUID processId = UUID.randomUUID();
		UUID slotId = UUID.randomUUID();
		UUID intervieweeUserId = UUID.randomUUID();
		when(currentUserProvider.isAdmin()).thenReturn(true);
		when(currentUserProvider.getUser()).thenReturn(createUser("Admin"));

		InterviewProcess process = new InterviewProcess();
		process.setSlots(List.of());
		process.setInterviewees(List.of());
		process.setTopic(new Topic());
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		assertThrows(ResourceNotFoundException.class, () ->
				interviewProcessService.bookInterviewSlot(processId, slotId, intervieweeUserId)
		);
	}

	@Test
	void cancelInterviewSlotBooking_WhenSlotNotBooked_Throws() {
		UUID processId = UUID.randomUUID();
		UUID slotId = UUID.randomUUID();

		InterviewProcess process = new InterviewProcess();
		process.setTopic(new Topic());
		InterviewSlot slot = new InterviewSlot();
		slot.setId(slotId);
		slot.setInterviewee(null);
		process.setSlots(List.of(slot));
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		assertThrows(IllegalStateException.class, () ->
				interviewProcessService.cancelInterviewSlotBooking(processId, slotId)
		);
	}

	@Test
	void cancelInterviewSlotBooking_WithOwner_CancelsAndSendsMail() {
		UUID processId = UUID.randomUUID();
		UUID slotId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		User currentUser = createUser("Student");
		currentUser.setId(userId);
		when(currentUserProvider.getUser()).thenReturn(currentUser);

		Interviewee interviewee = createInterviewee(UUID.randomUUID(), userId);
		InterviewSlot slot = new InterviewSlot();
		slot.setId(slotId);
		slot.setInterviewee(interviewee);

		InterviewProcess process = new InterviewProcess();
		Topic topic = new Topic();
		topic.setResearchGroup(new ResearchGroup());
		process.setTopic(topic);
		process.setSlots(new ArrayList<>(List.of(slot)));
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		InterviewSlot result = interviewProcessService.cancelInterviewSlotBooking(processId, slotId);

		assertEquals(null, result.getInterviewee());
		verify(mailingService).sendInterviewSlotConfirmationEmail(slot, "CANCEL");
		verify(interviewProcessRepository).save(process);
	}

	@Test
	void getPossibleApplicationsForProcess_UsesNotAssessedStateAndTopicFilter() {
		UUID processId = UUID.randomUUID();
		UUID topicId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		Topic topic = new Topic();
		topic.setId(topicId);
		topic.setResearchGroup(new ResearchGroup());
		process.setTopic(topic);
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));
		when(applicationService.getAll(any(), any(), any(), any(), any(), any(), any(), eq(false), eq(0), eq(50), eq("createdAt"), eq("desc")))
				.thenReturn(new PageImpl<>(List.of()));

		Page<Application> result = interviewProcessService.getPossibleApplicationsForProcess(processId, 0, 50);

		assertNotNull(result);
		ArgumentCaptor<String[]> topicFilterCaptor = ArgumentCaptor.forClass(String[].class);
		verify(applicationService).getAll(
				eq(null),
				eq(null),
				eq(null),
				any(),
				eq(null),
				topicFilterCaptor.capture(),
				eq(null),
				eq(false),
				eq(0),
				eq(50),
				eq("createdAt"),
				eq("desc")
		);
		assertEquals(1, topicFilterCaptor.getValue().length);
		assertEquals(topicId.toString(), topicFilterCaptor.getValue()[0]);
	}

	@Test
	void getInterviewProcessTopic_ReturnsTopic() {
		UUID processId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		Topic topic = new Topic();
		topic.setTitle("Topic");
		process.setTopic(topic);
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		Topic result = interviewProcessService.getInterviewProcessTopic(processId);

		assertEquals(topic, result);
	}

	@Test
	void getMyBookedSlot_WhenCurrentUserBookedSlot_ReturnsSlot() {
		UUID processId = UUID.randomUUID();
		UUID currentUserId = UUID.randomUUID();
		User currentUser = createUser("Student");
		currentUser.setId(currentUserId);
		when(currentUserProvider.getUser()).thenReturn(currentUser);

		Interviewee interviewee = createInterviewee(UUID.randomUUID(), currentUserId);
		InterviewSlot slot = new InterviewSlot();
		slot.setInterviewee(interviewee);

		InterviewProcess process = new InterviewProcess();
		process.setSlots(List.of(slot));
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		InterviewSlot result = interviewProcessService.getMyBookedSlot(processId);
		assertEquals(slot, result);
	}

	@Test
	void isInterviewProcessCompleted_ReturnsFlag() {
		UUID processId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		process.setCompleted(true);
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		assertTrue(interviewProcessService.isInterviewProcessCompleted(processId));
	}

	@Test
	void getInterviewProcess_ReturnsProcessAndChecksAccess() {
		UUID processId = UUID.randomUUID();
		ResearchGroup group = new ResearchGroup();
		InterviewProcess process = new InterviewProcess();
		Topic topic = new Topic();
		topic.setResearchGroup(group);
		process.setTopic(topic);
		when(interviewProcessRepository.findById(processId)).thenReturn(Optional.of(process));

		InterviewProcess result = interviewProcessService.getInterviewProcess(processId);

		assertEquals(process, result);
		verify(currentUserProvider).assertCanAccessResearchGroup(group);
	}

	private User createUser(String firstName) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstName(firstName);
		user.setLastName("Last");
		user.setGroups(new HashSet<>());
		return user;
	}

	private Interviewee createInterviewee(UUID intervieweeId, UUID userId) {
		ResearchGroup group = new ResearchGroup();
		group.setId(UUID.randomUUID());
		User user = createUser("Candidate");
		user.setId(userId);
		user.setResearchGroup(group);

		Application application = new Application();
		application.setId(UUID.randomUUID());
		application.setUser(user);
		application.setResearchGroup(group);
		application.setMotivation("motivation");
		application.setComment("");
		application.setState(ApplicationState.NOT_ASSESSED);
		application.setDesiredStartDate(Instant.now().plusSeconds(1000));

		Interviewee interviewee = new Interviewee();
		interviewee.setIntervieweeId(intervieweeId);
		interviewee.setApplication(application);
		interviewee.setAssessments(new ArrayList<>());
		interviewee.setSlots(new ArrayList<>());

		return interviewee;
	}

	private InterviewSlot createSlot(InterviewProcess process, Instant start, Instant end) {
		InterviewSlot slot = new InterviewSlot();
		slot.setId(UUID.randomUUID());
		slot.setInterviewProcess(process);
		slot.setStartDate(start);
		slot.setEndDate(end);
		return slot;
	}
}
