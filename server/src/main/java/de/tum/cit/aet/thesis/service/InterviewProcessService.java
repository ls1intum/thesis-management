package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.InterviewProcessRepository;
import de.tum.cit.aet.thesis.repository.IntervieweeRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InterviewProcessService {
    private final TopicService topicService;
    private final InterviewProcessRepository interviewProcessRepository;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    private final ApplicationService applicationService;
    private final IntervieweeRepository intervieweeRepository;

    @Autowired
    public InterviewProcessService(TopicService topicService, InterviewProcessRepository interviewProcessRepository, ObjectProvider<CurrentUserProvider> currentUserProviderProvider, ApplicationService applicationService, IntervieweeRepository intervieweeRepository) {
        this.topicService = topicService;
        this.interviewProcessRepository = interviewProcessRepository;
        this.currentUserProviderProvider = currentUserProviderProvider;
        this.applicationService = applicationService;
        this.intervieweeRepository = intervieweeRepository;
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

    public InterviewProcess createInterviewProcess(UUID topicId, List<UUID> intervieweeApplicationIds) {

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

        if (intervieweeApplicationIds != null && !intervieweeApplicationIds.isEmpty()) {
            List<Interviewee> interviewees = new ArrayList<>();
            for (UUID intervieweeApplicationId : intervieweeApplicationIds) {
                Application application = applicationService.findById(intervieweeApplicationId);
                if (application == null) {
                    throw new IllegalArgumentException("Application with ID " + intervieweeApplicationId + " does not exist.");
                }
                if(!application.getTopic().getId().equals(topicId)) {
                    throw new IllegalArgumentException("Application with ID " + intervieweeApplicationId + " does not belong to the specified topic.");
                }

                application.setState(ApplicationState.INTERVIEWING);

                Interviewee interviewee = new Interviewee();
                interviewee.setApplication(application);
                interviewee.setInterviewProcess(interviewProcess);

                interviewees.add(interviewee);
            }

            interviewProcess.setInterviewees(interviewees);
        }

        interviewProcessRepository.save(interviewProcess);

        return interviewProcess;
    }

    public InterviewProcess findById(UUID interviewProcessId) {
        return interviewProcessRepository.findById(interviewProcessId).orElseThrow(() -> new ResourceNotFoundException(String.format("InterviewProcess with id %s not found.", interviewProcessId)));
    }

    public boolean existsByTopicId(UUID topicId) {
        return interviewProcessRepository.existsByTopicId(topicId);
    }

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
                HibernateHelper.getColumnName(Interviewee.class, sortBy)
        );

       return intervieweeRepository.findAllInterviewees( interviewProcessId, searchQueryFilter, stateFilter, PageRequest.of(page, limit <= 0 ? Integer.MAX_VALUE : limit, Sort.by(order)) );
    }
}
