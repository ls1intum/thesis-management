package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.TopicRole;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.key.TopicRoleId;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.TopicRepository;
import de.tum.cit.aet.thesis.repository.TopicRoleRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TopicService {
    private final TopicRepository topicRepository;
    private final TopicRoleRepository topicRoleRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    private final ResearchGroupRepository researchGroupRepository;

    @Autowired
    public TopicService(TopicRepository topicRepository, TopicRoleRepository topicRoleRepository, UserRepository userRepository,
                        ObjectProvider<CurrentUserProvider> currentUserProviderProvider, ResearchGroupRepository researchGroupRepository) {
        this.topicRepository = topicRepository;
        this.topicRoleRepository = topicRoleRepository;
        this.userRepository = userRepository;
       this.currentUserProviderProvider = currentUserProviderProvider;
        this.researchGroupRepository = researchGroupRepository;
    }

    private CurrentUserProvider currentUserProvider() {
        return currentUserProviderProvider.getObject();
    }

    public Page<Topic> getAll(
            boolean onlyOwnResearchGroup,
            String[] types,
            boolean includeClosed,
            String searchQuery,
            int page,
            int limit,
            String sortBy,
            String sortOrder
    ) {
        Sort.Order order = new Sort.Order(
                sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                HibernateHelper.getColumnName(Topic.class, sortBy)
        );

        ResearchGroup researchGroup = onlyOwnResearchGroup ?
            currentUserProvider().getResearchGroupOrThrow() : null;
        String searchQueryFilter = searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
        String[] typesFilter = types == null || types.length == 0 ? null : types;

        return topicRepository.searchTopics(
                researchGroup == null ? null : researchGroup.getId(),
                typesFilter,
                includeClosed,
                searchQueryFilter,
                PageRequest.of(page, limit, Sort.by(order))
        );
    }

    @Transactional
    public Topic createTopic(
            String title,
            Set<String> thesisTypes,
            String problemStatement,
            String requirements,
            String goals,
            String references,
            List<UUID> supervisorIds,
            List<UUID> advisorIds,
            UUID researchGroupId
    ) {
        User creator = currentUserProvider().getUser();
        ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId).orElseThrow(
                () -> new ResourceNotFoundException("Research group not found")
        );
        Topic topic = new Topic();

        topic.setTitle(title);
        topic.setThesisTypes(thesisTypes);
        topic.setProblemStatement(problemStatement);
        topic.setRequirements(requirements);
        topic.setGoals(goals);
        topic.setReferences(references);
        topic.setUpdatedAt(Instant.now());
        topic.setCreatedAt(Instant.now());
        topic.setCreatedBy(creator);
        topic.setResearchGroup(researchGroup);

        topic = topicRepository.save(topic);

        assignTopicRoles(topic, advisorIds, supervisorIds);

        return topicRepository.save(topic);
    }

    @Transactional
    public Topic updateTopic(
            Topic topic,
            String title,
            Set<String> thesisTypes,
            String problemStatement,
            String requirements,
            String goals,
            String references,
            List<UUID> supervisorIds,
            List<UUID> advisorIds,
            UUID researchGroupId
    ) {
        ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId).orElseThrow(
                () -> new ResourceNotFoundException("Research group not found")
        );
        currentUserProvider().assertCanAccessResearchGroup(researchGroup);
        topic.setTitle(title);
        topic.setThesisTypes(thesisTypes);
        topic.setProblemStatement(problemStatement);
        topic.setRequirements(requirements);
        topic.setGoals(goals);
        topic.setReferences(references);
        topic.setUpdatedAt(Instant.now());
        topic.setResearchGroup(researchGroup);

        assignTopicRoles(topic, advisorIds, supervisorIds);

        return topicRepository.save(topic);
    }

    public Topic findById(UUID topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Topic with id %s not found.", topicId)));
    }

    private void assignTopicRoles(Topic topic, List<UUID> advisorIds, List<UUID> supervisorIds) {
        currentUserProvider().assertCanAccessResearchGroup(topic.getResearchGroup());
        List<User> supervisors = userRepository.findAllById(supervisorIds);
        List<User> advisors = userRepository.findAllById(advisorIds);

        supervisors.sort(Comparator.comparing(user -> supervisorIds.indexOf(user.getId())));
        advisors.sort(Comparator.comparing(user -> advisorIds.indexOf(user.getId())));

        if (supervisors.isEmpty() || supervisors.size() != supervisorIds.size()) {
            throw new ResourceInvalidParametersException("No supervisors selected or supervisors not found");
        }

        if (advisors.isEmpty() || advisors.size() != advisorIds.size()) {
            throw new ResourceInvalidParametersException("No advisors selected or advisors not found");
        }

        topicRoleRepository.deleteByTopicId(topic.getId());
        topic.setRoles(new ArrayList<>());

        for (int i = 0; i < supervisors.size(); i++) {
            User supervisor = supervisors.get(i);

            if (!supervisor.hasAnyGroup("supervisor")) {
                throw new ResourceInvalidParametersException("User is not a supervisor");
            }

            saveTopicRole(topic, supervisor, ThesisRoleName.SUPERVISOR, i);
        }

        for (int i = 0; i < advisors.size(); i++) {
            User advisor = advisors.get(i);

            if (!advisor.hasAnyGroup("advisor", "supervisor")) {
                throw new ResourceInvalidParametersException("User is not an advisor");
            }

            saveTopicRole(topic, advisor, ThesisRoleName.ADVISOR, i);
        }
    }

    private void saveTopicRole(Topic topic, User user, ThesisRoleName role, int position) {
        currentUserProvider().assertCanAccessResearchGroup(topic.getResearchGroup());
        User assigner = currentUserProvider().getUser();
        if (assigner == null || user == null) {
            throw new ResourceInvalidParametersException("Assigner and user must be provided.");
        }

        TopicRole topicRole = new TopicRole();
        TopicRoleId topicRoleId = new TopicRoleId();

        topicRoleId.setTopicId(topic.getId());
        topicRoleId.setUserId(user.getId());
        topicRoleId.setRole(role);

        topicRole.setId(topicRoleId);
        topicRole.setUser(user);
        topicRole.setAssignedBy(assigner);
        topicRole.setAssignedAt(Instant.now());
        topicRole.setTopic(topic);
        topicRole.setPosition(position);

        topicRoleRepository.save(topicRole);

        List<TopicRole> roles = topic.getRoles();

        roles.add(topicRole);
        roles.sort(Comparator.comparingInt(TopicRole::getPosition));

        topic.setRoles(roles);
    }
}
