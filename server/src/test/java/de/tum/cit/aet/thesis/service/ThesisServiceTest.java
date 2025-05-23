package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.mock.EntityMockFactory;
import de.tum.cit.aet.thesis.repository.*;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Mock private ThesisPresentationService thesisPresentationService;
    @Mock private ThesisFeedbackRepository thesisFeedbackRepository;
    @Mock private ThesisFileRepository thesisFileRepository;
    @Mock private ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    @Mock
    private ResearchGroupRepository researchGroupRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

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
                thesisPresentationService, thesisFeedbackRepository, thesisFileRepository,
                currentUserProviderProvider, researchGroupRepository
        );
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);

        testUser = EntityMockFactory.createUser("Test");
        testResearchGroup = EntityMockFactory.createResearchGroup("Test Research Group");
        testUser.setResearchGroup(testResearchGroup);
        testThesis = EntityMockFactory.createThesis("Test Thesis", testResearchGroup);
        EntityMockFactory.setupThesisRole(testThesis, testUser, ThesisRoleName.SUPERVISOR);
    }

    @Test
    void createThesis_WithValidData_CreatesThesis() {
        User supervisor = EntityMockFactory.createUserWithGroup("Supervisor", "supervisor");
        User advisor = EntityMockFactory.createUserWithGroup("Advisor", "advisor");
        User student = EntityMockFactory.createUserWithGroup("Student", "student");

        List<UUID> supervisorIds = new ArrayList<>(List.of(supervisor.getId()));
        List<UUID> advisorIds = new ArrayList<>(List.of(advisor.getId()));
        List<UUID> studentIds = new ArrayList<>(List.of(student.getId()));
        UUID researchGroupId = testResearchGroup.getId();

        when(userRepository.findAllById(supervisorIds)).thenReturn(new ArrayList<>(List.of(supervisor)));
        when(userRepository.findAllById(advisorIds)).thenReturn(new ArrayList<>(List.of(advisor)));
        when(userRepository.findAllById(studentIds)).thenReturn(new ArrayList<>(List.of(student)));
        when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProvider.getUser()).thenReturn(testUser);
        when(researchGroupRepository.findById(researchGroupId)).thenReturn(Optional.ofNullable(testResearchGroup));

        Thesis result = thesisService.createThesis(
                "Test Thesis",
                "Bachelor",
                "ENGLISH",
                supervisorIds,
                advisorIds,
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
                "A"
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