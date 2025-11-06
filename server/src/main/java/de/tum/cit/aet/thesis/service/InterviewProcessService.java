package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.EmailTemplate;
import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.InterviewProcessRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.util.UUID;

@Service
public class InterviewProcessService {
    private final TopicService topicService;
    private final InterviewProcessRepository interviewProcessRepository;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

    @Autowired
    public InterviewProcessService(TopicService topicService, InterviewProcessRepository interviewProcessRepository, ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
        this.topicService = topicService;
        this.interviewProcessRepository = interviewProcessRepository;
        this.currentUserProviderProvider = currentUserProviderProvider;
    }

    private CurrentUserProvider currentUserProvider() {
        return currentUserProviderProvider.getObject();
    }

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
                HibernateHelper.getColumnName(InterviewProcess.class, sortBy)
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

    public InterviewProcess createInterviewProcess(UUID topicId) {

        if (existsByTopicId(topicId)) {
            throw new IllegalStateException("An interview process for the given topic already exists.");
        }

        Topic topic = topicService.findById(topicId);

        if (topic == null) {
            throw new IllegalArgumentException("Topic with the given ID does not exist.");
        }

        currentUserProvider().assertCanAccessResearchGroup(topic.getResearchGroup());

        if (topic.getClosedAt() != null) {
            throw new IllegalStateException("Cannot create interview process for a closed topic.");
        }

        InterviewProcess interviewProcess = new InterviewProcess();
        interviewProcess.setTopic(topic);

        interviewProcessRepository.save(interviewProcess);

        return interviewProcess;
    }

    public InterviewProcess findById(UUID interviewProcessId) {
        return interviewProcessRepository.findById(interviewProcessId).orElseThrow(() -> new ResourceNotFoundException(String.format("InterviewProcess with id %s not found.", interviewProcessId)));
    }

    public boolean existsByTopicId(UUID topicId) {
        return interviewProcessRepository.existsByTopicId(topicId);
    }
}
