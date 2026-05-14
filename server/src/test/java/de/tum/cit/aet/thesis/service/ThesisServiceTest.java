package de.tum.cit.aet.thesis.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisAssessment;
import de.tum.cit.aet.thesis.entity.ThesisFile;
import de.tum.cit.aet.thesis.entity.ThesisProposal;
import de.tum.cit.aet.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.mock.EntityMockFactory;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ThesisAssessmentRepository;
import de.tum.cit.aet.thesis.repository.ThesisFeedbackRepository;
import de.tum.cit.aet.thesis.repository.ThesisFileRepository;
import de.tum.cit.aet.thesis.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.ThesisStateChangeRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ThesisServiceTest {
	@Mock private ThesisRoleRepository thesisRoleRepository;
	@Mock private ThesisRepository thesisRepository;
	@Mock private ThesisStateChangeRepository thesisStateChangeRepository;
	@Mock private UserRepository userRepository;
	@Mock private UploadService uploadService;
	@Mock private ThesisProposalRepository thesisProposalRepository;
	@Mock private ThesisAssessmentRepository thesisAssessmentRepository;
	@Mock private MailingService mailingService;
	@Mock private AccessManagementService accessManagementService;
	@Mock private ThesisFeedbackRepository thesisFeedbackRepository;
	@Mock private ThesisFileRepository thesisFileRepository;
	@Mock private ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	@Mock
	private ResearchGroupRepository researchGroupRepository;
	@Mock
	private CurrentUserProvider currentUserProvider;
	@Mock
	private ResearchGroupSettingsService researchGroupSettingsService;

	private ThesisService thesisService;
	private Thesis testThesis;
	private User testUser;
	private ResearchGroup testResearchGroup;

	@BeforeEach
	void setUp() {
		thesisService = new ThesisService(
				thesisRoleRepository, thesisRepository, thesisStateChangeRepository,
				userRepository, thesisProposalRepository, thesisAssessmentRepository,
				uploadService, mailingService, accessManagementService,
				thesisFeedbackRepository, thesisFileRepository,
				currentUserProviderProvider, researchGroupRepository, researchGroupSettingsService
		);
		when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);

		testUser = EntityMockFactory.createUser("Test");
		testResearchGroup = EntityMockFactory.createResearchGroup("Test Research Group");
		testUser.setResearchGroup(testResearchGroup);
		testThesis = EntityMockFactory.createThesis("Test Thesis", testResearchGroup);
		EntityMockFactory.setupThesisRole(testThesis, testUser, ThesisRoleName.EXAMINER);
	}

	@Test
	void createThesis_WithValidData_CreatesThesis() {
		User examiner = EntityMockFactory.createUserWithGroup("Examiner", "supervisor");
		User supervisor = EntityMockFactory.createUserWithGroup("Supervisor", "advisor");
		User student = EntityMockFactory.createUserWithGroup("Student", "student");

		List<UUID> examinerIds = new ArrayList<>(List.of(examiner.getId()));
		List<UUID> supervisorIds = new ArrayList<>(List.of(supervisor.getId()));
		List<UUID> studentIds = new ArrayList<>(List.of(student.getId()));
		UUID researchGroupId = testResearchGroup.getId();

		when(userRepository.findAllById(examinerIds)).thenReturn(new ArrayList<>(List.of(examiner)));
		when(userRepository.findAllById(supervisorIds)).thenReturn(new ArrayList<>(List.of(supervisor)));
		when(userRepository.findAllById(studentIds)).thenReturn(new ArrayList<>(List.of(student)));
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(currentUserProvider.getUser()).thenReturn(testUser);
		when(researchGroupRepository.findById(researchGroupId)).thenReturn(Optional.ofNullable(testResearchGroup));

		Thesis result = thesisService.createThesis(
				"Test Thesis",
				"Bachelor",
				"ENGLISH",
				examinerIds,
				supervisorIds,
				studentIds,
				null,
				true,
				researchGroupId
		);

		assertNotNull(result);
		assertEquals("Test Thesis", result.getTitle());
		assertEquals("Bachelor", result.getType());
		verify(thesisRepository).save(any(Thesis.class));
		verify(mailingService).sendThesisCreatedEmail(any(), eq(result));
		verify(accessManagementService).addStudentGroup(eq(student));
	}

	@Test
	void submitThesis_WithoutFile_ThrowsException() {
		testThesis.setFiles(new ArrayList<>());

		assertThrows(ResourceInvalidParametersException.class, () ->
				thesisService.submitThesis(testThesis)
		);
	}

	@Test
	void submitThesis_WithValidFile_SubmitsThesis() {
		ThesisFile file = new ThesisFile();
		file.setType("THESIS");
		testThesis.setFiles(List.of(file));
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Thesis result = thesisService.submitThesis(testThesis);

		assertEquals(ThesisState.SUBMITTED, result.getState());
		verify(thesisRepository).save(testThesis);
		verify(mailingService).sendFinalSubmissionEmail(testThesis);
	}

	@Test
	void uploadProposal_WithValidFile_UploadsProposal() {
		MultipartFile file = new MockMultipartFile(
				"proposal",
				"proposal.pdf",
				"application/pdf",
				"test content".getBytes()
		);
		when(uploadService.store(any(), any(), any())).thenReturn("stored-file");
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Thesis result = thesisService.uploadProposal(testThesis, file);

		assertNotNull(result);
		verify(uploadService).store(eq(file), any(), eq(UploadFileType.PDF));
		verify(thesisProposalRepository).save(any(ThesisProposal.class));
		verify(mailingService).sendProposalUploadedEmail(any(ThesisProposal.class));
	}

	@Test
	void acceptProposal_WithNoProposal_ThrowsException() {
		testThesis.setProposals(new ArrayList<>());

		assertThrows(ResourceNotFoundException.class, () ->
				thesisService.acceptProposal(testThesis)
		);
	}

	@Test
	void acceptProposal_WithValidProposal_AcceptsProposal() {
		ThesisProposal proposal = new ThesisProposal();
		testThesis.setProposals(List.of(proposal));
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Thesis result = thesisService.acceptProposal(testThesis);

		assertEquals(ThesisState.WRITING, result.getState());
		verify(thesisProposalRepository).save(any(ThesisProposal.class));
		verify(mailingService).sendProposalAcceptedEmail(any(ThesisProposal.class));
	}

	@Test
	void submitAssessment_WithValidData_CreatesAssessment() {
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Thesis result = thesisService.submitAssessment(
				testThesis,
				"Summary",
				"Positives",
				"Negatives",
				"A",
				null
		);

		assertEquals(ThesisState.ASSESSED, result.getState());
		verify(thesisAssessmentRepository).save(any(ThesisAssessment.class));
		verify(mailingService).sendAssessmentAddedEmail(any(ThesisAssessment.class));
	}

	@Test
	void gradeThesis_WithValidData_GradesThesis() {
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Thesis result = thesisService.gradeThesis(
				testThesis,
				"A",
				"Great work",
				ThesisVisibility.INTERNAL
		);

		assertEquals(ThesisState.GRADED, result.getState());
		assertEquals("A", result.getFinalGrade());
		assertEquals("Great work", result.getFinalFeedback());
		assertEquals(ThesisVisibility.INTERNAL, result.getVisibility());
		verify(thesisRepository).save(testThesis);
		verify(mailingService).sendFinalGradeEmail(testThesis);
	}

	@Test
	void revertToPreviousState_WithSingleState_ThrowsException() {
		testThesis.setStates(new HashSet<>(Set.of(
				stateChange(testThesis.getId(), ThesisState.PROPOSAL, Instant.parse("2026-01-01T10:00:00Z"))
		)));

		assertThrows(ResourceInvalidParametersException.class, () ->
				thesisService.revertToPreviousState(testThesis)
		);
	}

	@Test
	void revertToPreviousState_WithMultipleStates_RevertsAndDeletesLatest() {
		ThesisStateChange proposal = stateChange(testThesis.getId(), ThesisState.PROPOSAL, Instant.parse("2026-01-01T10:00:00Z"));
		ThesisStateChange writing = stateChange(testThesis.getId(), ThesisState.WRITING, Instant.parse("2026-02-01T10:00:00Z"));
		testThesis.setState(ThesisState.WRITING);
		testThesis.setStates(new HashSet<>(Set.of(proposal, writing)));
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Thesis result = thesisService.revertToPreviousState(testThesis);

		assertEquals(ThesisState.PROPOSAL, result.getState());
		verify(thesisStateChangeRepository).deleteById(writing.getId());
		verify(thesisRepository).save(testThesis);
	}

	@Test
	void revertToPreviousState_FromFinished_RestoresStudentGroup() {
		User student = EntityMockFactory.createUserWithGroup("Student", "student");
		EntityMockFactory.setupThesisRole(testThesis, student, ThesisRoleName.STUDENT);
		ThesisStateChange graded = stateChange(testThesis.getId(), ThesisState.GRADED, Instant.parse("2026-03-01T10:00:00Z"));
		ThesisStateChange finished = stateChange(testThesis.getId(), ThesisState.FINISHED, Instant.parse("2026-04-01T10:00:00Z"));
		testThesis.setState(ThesisState.FINISHED);
		testThesis.setStates(new HashSet<>(Set.of(graded, finished)));
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Thesis result = thesisService.revertToPreviousState(testThesis);

		assertEquals(ThesisState.GRADED, result.getState());
		verify(accessManagementService).addStudentGroup(student);
	}

	@Test
	void revertToPreviousState_FromDroppedOut_RestoresStudentGroup() {
		User student = EntityMockFactory.createUserWithGroup("Student", "student");
		EntityMockFactory.setupThesisRole(testThesis, student, ThesisRoleName.STUDENT);
		ThesisStateChange writing = stateChange(testThesis.getId(), ThesisState.WRITING, Instant.parse("2026-02-01T10:00:00Z"));
		ThesisStateChange dropped = stateChange(testThesis.getId(), ThesisState.DROPPED_OUT, Instant.parse("2026-03-01T10:00:00Z"));
		testThesis.setState(ThesisState.DROPPED_OUT);
		testThesis.setStates(new HashSet<>(Set.of(writing, dropped)));
		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Thesis result = thesisService.revertToPreviousState(testThesis);

		assertEquals(ThesisState.WRITING, result.getState());
		verify(accessManagementService).addStudentGroup(student);
	}

	private ThesisStateChange stateChange(UUID thesisId, ThesisState state, Instant changedAt) {
		ThesisStateChangeId id = new ThesisStateChangeId();
		id.setThesisId(thesisId);
		id.setState(state);
		ThesisStateChange change = new ThesisStateChange();
		change.setId(id);
		change.setThesis(testThesis);
		change.setChangedAt(changedAt);
		return change;
	}

	@Test
	void completeThesis_RemovesStudentGroupIfNoOtherTheses() {
		User student = EntityMockFactory.createUserWithGroup("Student", "student");
		EntityMockFactory.setupThesisRole(testThesis, student, ThesisRoleName.STUDENT);

		when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(thesisRepository.searchTheses(
				any(), any(), any(), any(), any(), any(), any()
		)).thenReturn(new PageImpl<>(Collections.emptyList()));

		Thesis result = thesisService.completeThesis(testThesis);

		assertEquals(ThesisState.FINISHED, result.getState());
		verify(thesisRepository).save(testThesis);
		verify(accessManagementService).removeStudentGroup(student);
	}
}
