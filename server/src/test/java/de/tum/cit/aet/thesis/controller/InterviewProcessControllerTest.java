package de.tum.cit.aet.thesis.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.controller.payload.AddIntervieweesPayload;
import de.tum.cit.aet.thesis.controller.payload.BookInterviewSlotPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateInterviewProcessPayload;
import de.tum.cit.aet.thesis.controller.payload.InviteIntervieweesPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateIntervieweeAssessmentPayload;
import de.tum.cit.aet.thesis.dto.InterviewProcessDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.UpcomingInterviewDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.InterviewProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InterviewProcessControllerTest {

	@Mock
	private InterviewProcessService interviewProcessService;

	private InterviewProcessController interviewProcessController;

	@BeforeEach
	void setUp() {
		interviewProcessController = new InterviewProcessController(interviewProcessService);
	}

	@Test
	void getMyBookedSlot_WhenSlotExists_ReturnsOk() {
		UUID processId = UUID.randomUUID();
		InterviewSlot slot = new InterviewSlot();
		slot.setId(UUID.randomUUID());
		slot.setStartDate(Instant.now());
		slot.setEndDate(Instant.now().plusSeconds(1800));

		when(interviewProcessService.getMyBookedSlot(processId)).thenReturn(slot);

		ResponseEntity<?> response = interviewProcessController.getMyBookedSlot(processId);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		verify(interviewProcessService).getMyBookedSlot(processId);
	}

	@Test
	void getMyInterviewProcesses_ReturnsMappedPaginationDto() {
		InterviewProcess process = new InterviewProcess();
		process.setId(UUID.randomUUID());
		Topic topic = new Topic();
		topic.setTitle("Interview Topic");
		process.setTopic(topic);
		process.setInterviewees(List.of());

		Page<InterviewProcess> page = new PageImpl<>(List.of(process));
		when(interviewProcessService.findAllMyProcesses("abc", 1, 10, "completed", "desc", true)).thenReturn(page);

		ResponseEntity<PaginationDto<InterviewProcessDto>> response = interviewProcessController.getMyInterviewProcesses(
				"abc",
				1,
				10,
				"completed",
				"desc",
				true
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(1, response.getBody().content().size());
		assertEquals(process.getId(), response.getBody().content().getFirst().interviewProcessId());
		assertEquals("Interview Topic", response.getBody().content().getFirst().topicTitle());
		verify(interviewProcessService).findAllMyProcesses("abc", 1, 10, "completed", "desc", true);
	}

	@Test
	void getMyBookedSlot_WhenSlotIsNull_ReturnsNoContent() {
		UUID processId = UUID.randomUUID();
		when(interviewProcessService.getMyBookedSlot(processId)).thenReturn(null);

		ResponseEntity<?> response = interviewProcessController.getMyBookedSlot(processId);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		assertNull(response.getBody());
		verify(interviewProcessService).getMyBookedSlot(processId);
	}

	@Test
	void createInterviewProcess_WithValidPayload_ReturnsDto() {
		UUID topicId = UUID.randomUUID();
		UUID applicationId = UUID.randomUUID();
		Topic topic = new Topic();
		topic.setTitle("Topic");

		InterviewProcess process = new InterviewProcess();
		process.setId(UUID.randomUUID());
		process.setTopic(topic);
		process.setInterviewees(List.of());

		when(interviewProcessService.createInterviewProcess(topicId, List.of(applicationId))).thenReturn(process);

		CreateInterviewProcessPayload payload = new CreateInterviewProcessPayload(topicId, List.of(applicationId));
		ResponseEntity<InterviewProcessDto> response = interviewProcessController.createInterviewProcess(payload);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(process.getId(), response.getBody().interviewProcessId());
		assertEquals("Topic", response.getBody().topicTitle());
		verify(interviewProcessService).createInterviewProcess(topicId, List.of(applicationId));
	}

	@Test
	void getInterviewee_WhenIntervieweeBelongsToDifferentProcess_ReturnsNotFound() {
		UUID processId = UUID.randomUUID();
		UUID intervieweeId = UUID.randomUUID();

		InterviewProcess anotherProcess = new InterviewProcess();
		anotherProcess.setId(UUID.randomUUID());
		Interviewee interviewee = new Interviewee();
		interviewee.setInterviewProcess(anotherProcess);

		when(interviewProcessService.getInterviewee(intervieweeId)).thenReturn(interviewee);

		ResponseEntity<?> response = interviewProcessController.getInterviewee(processId, intervieweeId);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		verify(interviewProcessService).getInterviewee(intervieweeId);
	}

	@Test
	void getInterviewee_WhenIntervieweeBelongsToProcess_ReturnsOk() {
		UUID processId = UUID.randomUUID();
		UUID intervieweeId = UUID.randomUUID();

		InterviewProcess process = new InterviewProcess();
		process.setId(processId);
		Interviewee interviewee = createInterviewee(intervieweeId, process);
		when(interviewProcessService.getInterviewee(intervieweeId)).thenReturn(interviewee);

		ResponseEntity<?> response = interviewProcessController.getInterviewee(processId, intervieweeId);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
	}

	@Test
	void getUpcomingInterviews_ReturnsOkWithList() {
		InterviewProcess process = new InterviewProcess();
		process.setId(UUID.randomUUID());
		Topic topic = new Topic();
		topic.setTitle("Topic");
		process.setTopic(topic);
		InterviewSlot slot = new InterviewSlot();
		slot.setId(UUID.randomUUID());
		slot.setStartDate(Instant.now().plusSeconds(1000));
		slot.setEndDate(Instant.now().plusSeconds(2000));

		when(interviewProcessService.getUpcomingInterviewsForCurrentUser())
				.thenReturn(List.of(UpcomingInterviewDto.fromInterviewSlot(process, slot)));

		ResponseEntity<List<UpcomingInterviewDto>> response = interviewProcessController.getUpcomingInterviews();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, response.getBody().size());
	}

	@Test
	void getInterviewProcessTopic_ReturnsTopicDto() {
		UUID processId = UUID.randomUUID();
		Topic topic = new Topic();
		topic.setId(UUID.randomUUID());
		topic.setTitle("Interview Topic");
		topic.setRoles(new ArrayList<>());
		topic.setResearchGroup(new ResearchGroup());
		when(interviewProcessService.getInterviewProcessTopic(processId)).thenReturn(topic);

		ResponseEntity<?> response = interviewProcessController.getInterviewProcessTopic(processId);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
	}

	@Test
	void getInterviewProcess_ReturnsProcessDto() {
		UUID processId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		process.setId(processId);
		Topic topic = new Topic();
		topic.setTitle("Topic");
		process.setTopic(topic);
		process.setInterviewees(List.of());
		when(interviewProcessService.getInterviewProcess(processId)).thenReturn(process);

		ResponseEntity<InterviewProcessDto> response = interviewProcessController.getInterviewProcess(processId);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(processId, response.getBody().interviewProcessId());
	}

	@Test
	void isInterviewProcessCompleted_ReturnsBoolean() {
		UUID processId = UUID.randomUUID();
		when(interviewProcessService.isInterviewProcessCompleted(processId)).thenReturn(true);

		ResponseEntity<Boolean> response = interviewProcessController.isInterviewProcessCompleted(processId);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(true, response.getBody());
	}

	@Test
	void getInterviewProcessInterviewSlots_ReturnsMappedDtos() {
		UUID processId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		InterviewSlot slot = new InterviewSlot();
		slot.setId(UUID.randomUUID());
		slot.setInterviewProcess(process);
		slot.setStartDate(Instant.now().plusSeconds(1000));
		slot.setEndDate(Instant.now().plusSeconds(2000));
		slot.setStreamLink("meet.example.com");

		when(interviewProcessService.getInterviewProcessInterviewSlots(processId, true)).thenReturn(List.of(slot));

		ResponseEntity<?> response = interviewProcessController.getInterviewProcessInterviewSlots(processId, true);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		verify(interviewProcessService).getInterviewProcessInterviewSlots(processId, true);
	}

	@Test
	void bookInterviewSlot_ReturnsBookedDto() {
		UUID processId = UUID.randomUUID();
		UUID slotId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		InterviewSlot slot = new InterviewSlot();
		slot.setId(slotId);
		slot.setInterviewProcess(new InterviewProcess());
		slot.setStartDate(Instant.now().plusSeconds(1000));
		slot.setEndDate(Instant.now().plusSeconds(2000));

		when(interviewProcessService.bookInterviewSlot(processId, slotId, userId)).thenReturn(slot);

		ResponseEntity<?> response = interviewProcessController.bookInterviewSlot(processId, slotId, new BookInterviewSlotPayload(userId));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
	}

	@Test
	void cancelInterviewSlotBooking_ReturnsUpdatedSlot() {
		UUID processId = UUID.randomUUID();
		UUID slotId = UUID.randomUUID();

		InterviewSlot slot = new InterviewSlot();
		slot.setId(slotId);
		slot.setInterviewProcess(new InterviewProcess());
		slot.setStartDate(Instant.now().plusSeconds(1000));
		slot.setEndDate(Instant.now().plusSeconds(2000));

		when(interviewProcessService.cancelInterviewSlotBooking(processId, slotId)).thenReturn(slot);

		ResponseEntity<?> response = interviewProcessController.cancelInterviewSlotBooking(processId, slotId);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
	}

	@Test
	void updateInterviewee_WhenDifferentProcess_ReturnsNotFoundAndDoesNotUpdate() {
		UUID processId = UUID.randomUUID();
		UUID intervieweeId = UUID.randomUUID();

		InterviewProcess anotherProcess = new InterviewProcess();
		anotherProcess.setId(UUID.randomUUID());
		Interviewee interviewee = createInterviewee(intervieweeId, anotherProcess);
		when(interviewProcessService.getInterviewee(intervieweeId)).thenReturn(interviewee);

		ResponseEntity<?> response = interviewProcessController.updateInterviewee(
				processId,
				intervieweeId,
				new UpdateIntervieweeAssessmentPayload("note", 10)
		);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		verify(interviewProcessService, never()).updateIntervieweeAssessment(any(), any(), anyInt());
	}

	@Test
	void updateInterviewee_WhenSameProcess_ReturnsUpdatedIntervieweeDto() {
		UUID processId = UUID.randomUUID();
		UUID intervieweeId = UUID.randomUUID();

		InterviewProcess process = new InterviewProcess();
		process.setId(processId);
		Interviewee interviewee = createInterviewee(intervieweeId, process);
		Interviewee updated = createInterviewee(intervieweeId, process);
		updated.setScore(9);

		when(interviewProcessService.getInterviewee(intervieweeId)).thenReturn(interviewee);
		when(interviewProcessService.updateIntervieweeAssessment(interviewee, "strong", 9)).thenReturn(updated);

		ResponseEntity<?> response = interviewProcessController.updateInterviewee(
				processId,
				intervieweeId,
				new UpdateIntervieweeAssessmentPayload("strong", 9)
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		verify(interviewProcessService).updateIntervieweeAssessment(interviewee, "strong", 9);
	}

	@Test
	void inviteInterviewees_ReturnsMappedIntervieweeDtos() {
		UUID processId = UUID.randomUUID();
		UUID intervieweeAId = UUID.randomUUID();
		UUID intervieweeBId = UUID.randomUUID();
		InterviewProcess process = new InterviewProcess();
		process.setId(processId);

		Interviewee first = createInterviewee(intervieweeAId, process);
		Interviewee second = createInterviewee(intervieweeBId, process);
		first.setLastInvited(Instant.now());
		second.setLastInvited(Instant.now());

		when(interviewProcessService.inviteInterviewees(processId, List.of(intervieweeAId, intervieweeBId)))
				.thenReturn(List.of(first, second));

		ResponseEntity<?> response = interviewProcessController.inviteInterviewees(
				processId,
				new InviteIntervieweesPayload(List.of(intervieweeAId, intervieweeBId))
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		verify(interviewProcessService).inviteInterviewees(processId, List.of(intervieweeAId, intervieweeBId));
	}

	@Test
	void addInterviewees_ReturnsUpdatedProcessDto() {
		UUID processId = UUID.randomUUID();
		UUID topicId = UUID.randomUUID();
		UUID applicationId = UUID.randomUUID();

		Topic topic = new Topic();
		topic.setId(topicId);
		InterviewProcess existing = new InterviewProcess();
		existing.setId(processId);
		existing.setTopic(topic);
		existing.setInterviewees(List.of());
		when(interviewProcessService.findById(processId)).thenReturn(existing);

		InterviewProcess updated = new InterviewProcess();
		updated.setId(processId);
		updated.setTopic(topic);
		updated.setInterviewees(List.of());
		when(interviewProcessService.createInterviewProcess(topicId, List.of(applicationId))).thenReturn(updated);

		ResponseEntity<InterviewProcessDto> response = interviewProcessController.addInterviewees(
				processId,
				new AddIntervieweesPayload(List.of(applicationId))
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(processId, response.getBody().interviewProcessId());
	}

	@Test
	void getInterviewApplications_ReturnsPaginationResponse() {
		UUID processId = UUID.randomUUID();
		Application application = new Application();
		application.setId(UUID.randomUUID());
		User user = createUser("Ada", "Lovelace");
		application.setUser(user);
		application.setState(ApplicationState.NOT_ASSESSED);
		when(interviewProcessService.getPossibleApplicationsForProcess(processId, 0, 50))
				.thenReturn(new PageImpl<>(List.of(application)));

		ResponseEntity<?> response = interviewProcessController.getInterviewApplications(processId, 0, 50);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
	}

	private Interviewee createInterviewee(UUID intervieweeId, InterviewProcess process) {
		Interviewee interviewee = new Interviewee();
		interviewee.setIntervieweeId(intervieweeId);
		interviewee.setInterviewProcess(process);
		interviewee.setApplication(createApplication());
		interviewee.setAssessments(new ArrayList<>());
		interviewee.setSlots(new ArrayList<>());
		return interviewee;
	}

	private Application createApplication() {
		Application application = new Application();
		application.setId(UUID.randomUUID());
		application.setUser(createUser("Grace", "Hopper"));
		application.setState(ApplicationState.NOT_ASSESSED);
		application.setMotivation("m");
		application.setComment("");
		application.setDesiredStartDate(Instant.now().plusSeconds(1000));
		return application;
	}

	private User createUser(String firstName, String lastName) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setGroups(new HashSet<>());
		return user;
	}
}
