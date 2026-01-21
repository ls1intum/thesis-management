package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.TopicState;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.TopicRole;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.mock.EntityMockFactory;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.TopicRepository;
import de.tum.cit.aet.thesis.repository.TopicRoleRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
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
import org.springframework.data.domain.Sort;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private TopicRoleRepository topicRoleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private ResearchGroupRepository researchGroupRepository;

    private TopicService topicService;
    private Topic testTopic;
    private User testUser;
    private ResearchGroup testResearchGroup;

    @BeforeEach
    void setUp() {
        topicService = new TopicService(
                topicRepository,
                topicRoleRepository,
                userRepository,
                currentUserProviderProvider,
                researchGroupRepository
        );

        testUser = EntityMockFactory.createUser("Test User");
        testResearchGroup = EntityMockFactory.createResearchGroup("Test Research Group");
        testUser.setResearchGroup(testResearchGroup);
        testTopic = EntityMockFactory.createTopic("Test Topic", testResearchGroup);
    }

    @Test
    void getAll_ReturnsPageOfTopics() {
        List<Topic> topics = List.of(testTopic);
        Page<Topic> expectedPage = new PageImpl<>(topics);
        when(topicRepository.searchTopics(
                any(),
                any(),
                any(),
                any(),
                any(PageRequest.class)
        )).thenReturn(expectedPage);

        Page<Topic> result = topicService.getAll(
                false,
                null,
                null,
                null,
                0,
                10,
                "title",
                "asc",
                null
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(topicRepository).searchTopics(
                eq(null),
                eq(null),
                eq(new String[] { TopicState.OPEN.name()}),
                eq(null),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")))
        );
    }

    @Test
    void createTopic_WithValidData_CreatesTopic() {
        List<UUID> supervisorIds = List.of(UUID.randomUUID());
        List<UUID> advisorIds = List.of(UUID.randomUUID());
        UUID researchGroupId = testResearchGroup.getId();

        User supervisor = EntityMockFactory.createUserWithGroup("Supervisor", "supervisor");
        User advisor = EntityMockFactory.createUserWithGroup("Advisor", "advisor");

        when(userRepository.findAllById(supervisorIds)).thenReturn(new ArrayList<>(List.of(supervisor)));
        when(userRepository.findAllById(advisorIds)).thenReturn(new ArrayList<>(List.of(advisor)));
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
        when(currentUserProvider.getUser()).thenReturn(testUser);
        when(researchGroupRepository.findById(researchGroupId)).thenReturn(Optional.ofNullable(testResearchGroup));

        Topic result = topicService.createTopic(
                "Test Topic",
                Set.of("Bachelor"),
                "Problem Statement",
                "Requirements",
                "Goals",
                "References",
                supervisorIds,
                advisorIds,
                researchGroupId,
                null,
                null,
                false
        );

        assertNotNull(result);
        verify(topicRepository, times(2)).save(any(Topic.class));
        verify(topicRoleRepository, times(2)).save(any(TopicRole.class));
    }

    @Test
    void createTopic_WithInvalidSupervisor_ThrowsException() {
        User invalidSupervisor = EntityMockFactory.createUserWithGroup("Student", "student");
        User advisor = EntityMockFactory.createUserWithGroup("Advisor", "advisor");

        List<UUID> supervisorIds = new ArrayList<>(List.of(invalidSupervisor.getId()));
        List<UUID> advisorIds = new ArrayList<>(List.of(advisor.getId()));
        UUID researchGroupId = testUser.getResearchGroup().getId();

        when(userRepository.findAllById(supervisorIds)).thenReturn(new ArrayList<>(List.of(invalidSupervisor)));
        when(userRepository.findAllById(advisorIds)).thenReturn(new ArrayList<>(List.of(advisor)));
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
        when(currentUserProvider.getUser()).thenReturn(testUser);
        when(researchGroupRepository.findById(researchGroupId)).thenReturn(Optional.ofNullable(testResearchGroup));

        assertThrows(ResourceInvalidParametersException.class, () ->
                topicService.createTopic(
                        "Test Topic",
                        Set.of("Bachelor"),
                        "Problem Statement",
                        "Requirements",
                        "Goals",
                        "References",
                        supervisorIds,
                        advisorIds,
                        researchGroupId,
                        null,
                        null,
                        false
                )
        );
    }

    @Test
    void updateTopic_WithValidData_UpdatesTopic() {
        User supervisor = EntityMockFactory.createUserWithGroup("Supervisor", "supervisor");
        User advisor = EntityMockFactory.createUserWithGroup("Advisor", "advisor");

        List<UUID> supervisorIds = new ArrayList<>(List.of(supervisor.getId()));
        List<UUID> advisorIds = new ArrayList<>(List.of(advisor.getId()));
        UUID researchGroupId = testUser.getResearchGroup().getId();

        when(userRepository.findAllById(supervisorIds)).thenReturn(new ArrayList<>(List.of(supervisor)));
        when(userRepository.findAllById(advisorIds)).thenReturn(new ArrayList<>(List.of(advisor)));
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
        when(currentUserProvider.getUser()).thenReturn(testUser);
        when(researchGroupRepository.findById(researchGroupId)).thenReturn(Optional.ofNullable(testResearchGroup));

        Topic result = topicService.updateTopic(
                testTopic,
                "Updated Topic",
                Set.of("Master"),
                "Updated Problem",
                "Updated Requirements",
                "Updated Goals",
                "Updated References",
                supervisorIds,
                advisorIds,
                researchGroupId,
                null,
                null,
                false
        );

        assertNotNull(result);
        assertEquals("Updated Topic", result.getTitle());
        verify(topicRoleRepository).deleteByTopicId(testTopic.getId());
        verify(topicRepository).save(testTopic);
    }

    @Test
    void findById_WithValidId_ReturnsTopic() {
        when(topicRepository.findById(testTopic.getId())).thenReturn(Optional.of(testTopic));

        Topic result = topicService.findById(testTopic.getId());

        assertNotNull(result);
        assertEquals(testTopic, result);
    }

    @Test
    void findById_WithInvalidId_ThrowsException() {
        when(topicRepository.findById(testTopic.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                topicService.findById(testTopic.getId())
        );
    }
}
