package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.constants.ApplicationReviewReason;
import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.entity.key.ApplicationReviewerId;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.mock.EntityMockFactory;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.TopicRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private MailingService mailingService;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private ThesisService thesisService;
    @Mock
    private TopicService topicService;
    @Mock
    private ApplicationReviewerRepository applicationReviewerRepository;
    @Mock
    private ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private ResearchGroupRepository researchGroupRepository;

    private ApplicationService applicationService;
    private User testUser;
    private ResearchGroup testResearchGroup;
    private Topic testTopic;
    private Application testApplication;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                applicationRepository,
                mailingService,
                topicRepository,
                thesisService,
                topicService,
                applicationReviewerRepository,
                currentUserProviderProvider,
                researchGroupRepository
        );

        testUser = EntityMockFactory.createUser("Test User");
        testResearchGroup = EntityMockFactory.createResearchGroup("Test Research Group");
        testUser.setResearchGroup(testResearchGroup);
        testTopic = EntityMockFactory.createTopic("Test Topic", testResearchGroup);
        testApplication = EntityMockFactory.createApplication(testResearchGroup);
    }

    @Test
    void getAll_WithValidParameters_ReturnsPageOfApplications() {
        Page<Application> expectedPage = new PageImpl<>(List.of(testApplication));
        when(applicationRepository.searchApplications(
                any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(PageRequest.class)
        )).thenReturn(expectedPage);
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);

        Page<Application> result = applicationService.getAll(
                null,
                null,
                null,
                new ApplicationState[]{ApplicationState.NOT_ASSESSED},
                null,
                null,
                null,
                true,
                0,
                10,
                "createdAt",
                "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(applicationRepository).searchApplications(
                any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(PageRequest.class)
        );
    }

    @Test
    void createApplication_WithValidData_CreatesApplication() {
        when(topicService.findById(testTopic.getId())).thenReturn(testTopic);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.createApplication(
                testUser,
                testResearchGroup.getId(),
                testTopic.getId(),
                "Test Thesis",
                "Bachelor",
                Instant.now(),
                "Test motivation"
        );

        assertNotNull(result);
        verify(applicationRepository).save(any(Application.class));
        verify(mailingService).sendApplicationCreatedEmail(any(Application.class));
    }

    @Test
    void createApplication_WithClosedTopic_ThrowsException() {
        testTopic.setClosedAt(Instant.now());
        when(topicService.findById(testTopic.getId())).thenReturn(testTopic);

        assertThrows(ResourceInvalidParametersException.class, () ->
                applicationService.createApplication(
                        testUser,
                        testResearchGroup.getId(),
                        testTopic.getId(),
                        "Test Thesis",
                        "Bachelor",
                        Instant.now(),
                        "Test motivation"
                )
        );
    }

    @Test
    void accept_WithValidData_AcceptsApplicationAndCreatesThesis() {
        User reviewer = new User();
        reviewer.setId(UUID.randomUUID());
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(thesisService.createThesis(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(EntityMockFactory.createThesis("Test Thesis", testResearchGroup));
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);

        List<Application> results = applicationService.accept(
                reviewer,
                testApplication,
                "Test Thesis",
                "Bachelor",
                "ENGLISH",
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()),
                true,
                false
        );

        assertFalse(results.isEmpty());
        assertEquals(ApplicationState.ACCEPTED, results.getFirst().getState());
        verify(thesisService).createThesis(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        verify(mailingService).sendApplicationAcceptanceEmail(any(), any());
    }

    @Test
    void reject_WithValidData_RejectsApplication() {
        User reviewer = EntityMockFactory.createUser("Reviewer");
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);

        List<Application> results = applicationService.reject(
                reviewer,
                testApplication,
                ApplicationRejectReason.TOPIC_FILLED,
                true
        );

        assertFalse(results.isEmpty());
        assertEquals(ApplicationState.REJECTED, results.getFirst().getState());
        verify(mailingService).sendApplicationRejectionEmail(any(), any());
    }

    @Test
    void reviewApplication_WithNewReviewer_CreatesReview() {
        User reviewer = new User();
        reviewer.setId(UUID.randomUUID());
        ApplicationReviewer applicationReviewer = new ApplicationReviewer();
        applicationReviewer.setId(new ApplicationReviewerId());
        when(applicationReviewerRepository.save(any(ApplicationReviewer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);

        Application result = applicationService.reviewApplication(
                testApplication,
                reviewer,
                ApplicationReviewReason.INTERESTED
        );

        assertNotNull(result);
        verify(applicationReviewerRepository).save(any(ApplicationReviewer.class));
    }

    @Test
    void closeTopic_WithValidData_ClosesTopicAndRejectsApplications() {
        User closer = new User();
        closer.setId(UUID.randomUUID());
        List<Application> apllicationList = Arrays.asList(testApplication);
        when(applicationRepository.findAllByTopic(testTopic)).thenReturn(apllicationList);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(topicRepository.save(any(Topic.class))).thenReturn(testTopic);
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
        when(currentUserProvider.getUser()).thenReturn(closer);

        Topic result = applicationService.closeTopic(
                testTopic,
                ApplicationRejectReason.TOPIC_FILLED,
                true
        );

        assertNotNull(result);
        assertNotNull(result.getClosedAt());
        verify(topicRepository).save(testTopic);
        verify(applicationRepository).findAllByTopic(testTopic);
    }

    @Test
    void applicationExists_WithExistingApplication_ReturnsTrue() {
        UUID topicId = UUID.randomUUID();
        when(applicationRepository.existsPendingApplication(testUser.getId(), topicId)).thenReturn(true);

        boolean result = applicationService.applicationExists(testUser, topicId);

        assertTrue(result);
    }

    @Test
    void findById_WithValidId_ReturnsApplication() {
        when(applicationRepository.findById(testApplication.getId())).thenReturn(Optional.of(testApplication));
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);

        Application result = applicationService.findById(testApplication.getId());

        assertNotNull(result);
        assertEquals(testApplication, result);
    }

    @Test
    void findById_WithInvalidId_ThrowsException() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.findById(applicationId)
        );
    }

    @Test
    void updateApplication_WithValidData_UpdatesApplication() {
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(topicService.findById(any())).thenReturn(testTopic);
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);

        Application result = applicationService.updateApplication(
                testApplication,
                UUID.randomUUID(),
                "Updated Title",
                "Master",
                Instant.now(),
                "Updated motivation"
        );

        assertNotNull(result);
        assertEquals("Updated Title", result.getThesisTitle());
        verify(applicationRepository).save(any(Application.class));
    }
}