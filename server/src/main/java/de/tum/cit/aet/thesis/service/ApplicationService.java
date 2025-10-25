package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.constants.ApplicationReviewReason;
import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.cron.model.ApplicationRejectObject;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.entity.key.ApplicationReviewerId;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.TopicRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
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
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final MailingService mailingService;
    private final TopicRepository topicRepository;
    private final ThesisService thesisService;
    private final TopicService topicService;
    private final ApplicationReviewerRepository applicationReviewerRepository;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    private final ResearchGroupRepository researchGroupRepository;

    @Autowired
    public ApplicationService(
            ApplicationRepository applicationRepository,
            MailingService mailingService,
            TopicRepository topicRepository,
            ThesisService thesisService,
            TopicService topicService,
            ApplicationReviewerRepository applicationReviewerRepository,
            ObjectProvider<CurrentUserProvider> currentUserProviderProvider, ResearchGroupRepository researchGroupRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.mailingService = mailingService;
        this.topicRepository = topicRepository;
        this.thesisService = thesisService;
        this.topicService = topicService;
        this.applicationReviewerRepository = applicationReviewerRepository;
        this.currentUserProviderProvider = currentUserProviderProvider;
        this.researchGroupRepository = researchGroupRepository;
    }

    private CurrentUserProvider currentUserProvider() {
        return currentUserProviderProvider.getObject();
    }

    public Page<Application> getAll(
            UUID userId,
            UUID reviewerId,
            String searchQuery,
            ApplicationState[] states,
            String[] previous,
            String[] topics,
            String[] types,
            boolean includeSuggestedTopics,
            int page,
            int limit,
            String sortBy,
            String sortOrder
    ) {
        Sort.Order order = new Sort.Order(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);

        ResearchGroup researchGroup = currentUserProvider().getResearchGroupOrThrow();
        String searchQueryFilter = searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
        Set<ApplicationState> statesFilter = states == null || states.length == 0 ? null : new HashSet<>(Arrays.asList(states));
        Set<String> topicsFilter = topics == null || topics.length == 0 ? null : new HashSet<>(Arrays.asList(topics));
        Set<String> typesFilter = types == null || types.length == 0 ? null : new HashSet<>(Arrays.asList(types));
        Set<String> previousFilter = previous == null || previous.length == 0 ? null : new HashSet<>(Arrays.asList(previous));

        return applicationRepository.searchApplications(
                researchGroup == null ? null : researchGroup.getId(),
                userId,
                statesFilter != null && !statesFilter.contains(ApplicationState.REJECTED) ? reviewerId : null,
                searchQueryFilter,
                statesFilter,
                previousFilter,
                topicsFilter,
                typesFilter,
                includeSuggestedTopics,
                PageRequest.of(page, limit, Sort.by(order))
        );
    }

    public List<Application> getNotAssesedSuggestedOfResearchGroup(UUID researchGroupId) {
        return applicationRepository.findNotReviewedSuggestedByResearchGroup(researchGroupId);
    }

    @Transactional
    public Application createApplication(User user, UUID researchGroupId, UUID topicId, String thesisTitle,
                                         String thesisType, Instant desiredStartDate, String motivation) {
        Topic topic = topicId == null ? null : topicService.findById(topicId);

        if (topic != null && topic.getClosedAt() != null) {
            throw new ResourceInvalidParametersException("This topic is already closed. You cannot submit new applications for it.");
        }

        Application application = new Application();
        application.setUser(user);

        application.setTopic(topic);
        application.setThesisTitle(thesisTitle);
        application.setThesisType(thesisType);
        application.setMotivation(motivation);
        application.setComment("");
        application.setState(ApplicationState.NOT_ASSESSED);
        application.setDesiredStartDate(desiredStartDate);
        application.setCreatedAt(Instant.now());
        ResearchGroup researchGroup = topic != null
                ? topic.getResearchGroup()
                : researchGroupRepository.findById(researchGroupId).orElseThrow(() -> new ResourceNotFoundException(
                String.format("Research Group with id %s not found.", researchGroupId)));
        application.setResearchGroup(researchGroup);

        application = applicationRepository.save(application);

        mailingService.sendApplicationCreatedEmail(application);

        return application;
    }

    @Transactional
    public Application updateApplication(Application application, UUID topicId, String thesisTitle, String thesisType, Instant desiredStartDate, String motivation) {
        currentUserProvider().assertCanAccessResearchGroup(application.getResearchGroup());
        application.setTopic(topicId == null ? null : topicService.findById(topicId));
        application.setThesisTitle(thesisTitle);
        application.setThesisType(thesisType);
        application.setMotivation(motivation);
        application.setDesiredStartDate(desiredStartDate);

        application = applicationRepository.save(application);

        return application;
    }

    @Transactional
    public List<Application> accept(
            User reviewingUser,
            Application application,
            String thesisTitle,
            String thesisType,
            String language,
            List<UUID> advisorIds,
            List<UUID> supervisorIds,
            boolean notifyUser,
            boolean closeTopic
    ) {
        currentUserProvider().assertCanAccessResearchGroup(application.getResearchGroup());
        List<Application> result = new ArrayList<>();

        application.setState(ApplicationState.ACCEPTED);
        application.setReviewedAt(Instant.now());

        application = reviewApplication(application, reviewingUser, ApplicationReviewReason.INTERESTED);

        Thesis thesis = thesisService.createThesis(
                thesisTitle,
                thesisType,
                language,
                supervisorIds,
                advisorIds,
                List.of(application.getUser().getId()),
                application,
                false,
                application.getResearchGroup().getId()
        );

        application = applicationRepository.save(application);

        Topic topic = application.getTopic();

        if (topic != null && closeTopic) {
            topic.setClosedAt(Instant.now());

            result.addAll(rejectApplicationsForTopic(reviewingUser, topic, ApplicationRejectReason.TOPIC_FILLED, true));

            application.setTopic(topicRepository.save(topic));
        }

        if (notifyUser) {
            mailingService.sendApplicationAcceptanceEmail(application, thesis);
        }

        result.add(applicationRepository.save(application));

        return result;
    }

    @Transactional
    public List<Application> reject(User reviewingUser, Application application, ApplicationRejectReason reason, boolean notifyUser, boolean authenticated) {
        // if auto is provided and true, skip access check (used for automatic rejects)
        if (authenticated) {
            currentUserProvider().assertCanAccessResearchGroup(application.getResearchGroup());
        }
        application.setState(ApplicationState.REJECTED);
        application.setRejectReason(reason);
        application.setReviewedAt(Instant.now());

        application = authenticated ? reviewApplication(application, reviewingUser, ApplicationReviewReason.NOT_INTERESTED) : reviewApplicationWithoutAuth(application, reviewingUser, ApplicationReviewReason.NOT_INTERESTED);

        List<Application> result = new ArrayList<>();

        if (reason == ApplicationRejectReason.FAILED_STUDENT_REQUIREMENTS) {
            List<Application> applications = applicationRepository.findAllByUser(application.getUser());

            for (Application item : applications) {
                if (item.getState() == ApplicationState.NOT_ASSESSED) {
                    item.setState(ApplicationState.REJECTED);
                    item.setRejectReason(reason);
                    item.setReviewedAt(Instant.now());

                    item = authenticated ? reviewApplication(item, reviewingUser, ApplicationReviewReason.NOT_INTERESTED) : reviewApplicationWithoutAuth(item, reviewingUser, ApplicationReviewReason.NOT_INTERESTED);

                    result.add(applicationRepository.save(item));
                }
            }
        }

        if (notifyUser) {
            mailingService.sendApplicationRejectionEmail(application, reason);
        }

        result.add(applicationRepository.save(application));

        return result;
    }

    @Transactional
    public void rejectAllApplicationsAutomatically(Topic topic, int afterDuration, Instant referenceDate, UUID researchGroupId) {
        List<Application> applications = applicationRepository.findAllByTopic(topic);

        int minimalRejectDuration = 14;
        int referenceDuration = Math.max(afterDuration * 7, minimalRejectDuration);

        for (Application application : applications) {
            Instant referenceDateLocal = referenceDate == null ? application.getCreatedAt() : referenceDate;

            if (application.getState() == ApplicationState.NOT_ASSESSED &&  Instant.now().isAfter(application.getCreatedAt().plus(java.time.Duration.ofDays(minimalRejectDuration))) &&  Instant.now().isAfter(referenceDateLocal.plus(java.time.Duration.ofDays(referenceDuration)))) { //Check if the application is older than two weeks and the reference date + duration is in the past
                ResearchGroup topicGroup = researchGroupRepository.findById(researchGroupId).orElseThrow(() -> new ResourceNotFoundException("Research Group not found: " + researchGroupId));
                User reviewingUser = topicGroup.getHead();

                Optional<TopicRole> supervisor = topic.getRoles().stream().filter((role) -> role.getId().getRole() == ThesisRoleName.SUPERVISOR).findFirst();

                if (supervisor.isPresent()) {
                    reviewingUser = supervisor.orElseThrow().getUser();
                }

                reject(reviewingUser , application, ApplicationRejectReason.GENERAL, true, false);
            }
        }
    }

    public List<ApplicationRejectObject> getListOfApplicationsThatWillBeRejected(Topic topic, int afterDuration, Instant referenceDate) {
        List<Application> applications = applicationRepository.findAllByTopic(topic);
        List<ApplicationRejectObject> result = new ArrayList<>();

        int minimalRejectDuration = 14;
        int referenceDuration = Math.max(afterDuration * 7, minimalRejectDuration);
        int timeTillRejection = 7;

        for (Application application : applications) {
            Instant applicationReferenceDate = referenceDate;
            if (applicationReferenceDate == null) {
                applicationReferenceDate = application.getCreatedAt();
            }

            if (application.getState() == ApplicationState.NOT_ASSESSED) {
                Instant rejectionOption1 = application.getCreatedAt().plus(java.time.Duration.ofDays(minimalRejectDuration));
                Instant rejectionOption2 = applicationReferenceDate.plus(java.time.Duration.ofDays(referenceDuration));
                Instant dateOfRejection = rejectionOption1.isAfter(rejectionOption2) ? rejectionOption1 : rejectionOption2;

                if (Instant.now().isAfter(dateOfRejection.minus(java.time.Duration.ofDays(timeTillRejection)))) {
                    result.add(new ApplicationRejectObject(application.getUser().getFirstName() + " " + application.getUser().getLastName(), topic.getTitle(), dateOfRejection, application.getId()) );
                }
            }
        }

        return result;
    }

    @Transactional
    public void rejectListOfApplicationsIfOlderThan(List<Application> applications, int afterDuration, UUID researchGroupId) {
        ResearchGroup topicGroup = researchGroupRepository.findById(researchGroupId).orElseThrow(() -> new ResourceNotFoundException("Research Group not found: " + researchGroupId));
        User reviewingUser = topicGroup.getHead();

        for (Application application : applications) {
            if (application.getState() == ApplicationState.NOT_ASSESSED &&  Instant.now().isAfter(application.getCreatedAt().plus(java.time.Duration.ofDays(Math.max(afterDuration, 14))))) { //Check if the application is older than rejection duration, but minimal 14days
                reject(reviewingUser , application, ApplicationRejectReason.GENERAL, true, false);
            }
        }
    }

    @Transactional
    public Topic closeTopic(Topic topic, ApplicationRejectReason reason, boolean notifyUser) {
        currentUserProvider().assertCanAccessResearchGroup(topic.getResearchGroup());
        topic.setClosedAt(Instant.now());

        rejectApplicationsForTopic(currentUserProvider().getUser(), topic, reason, notifyUser);

        return topicRepository.save(topic);
    }

    @Transactional
    public List<Application> rejectApplicationsForTopic(User closer, Topic topic, ApplicationRejectReason reason, boolean notifyUser) {
        currentUserProvider().assertCanAccessResearchGroup(topic.getResearchGroup());
        List<Application> applications = applicationRepository.findAllByTopic(topic);
        List<Application> result = new ArrayList<>();

        for (Application application : applications) {
            if (application.getState() != ApplicationState.NOT_ASSESSED) {
                continue;
            }

            result.addAll(reject(closer, application, reason, notifyUser, true));
        }

        return result;
    }

    @Transactional
    public Application reviewApplication(Application application, User reviewer, ApplicationReviewReason reason) {
        currentUserProvider().assertCanAccessResearchGroup(application.getResearchGroup());

        return reviewApplicationWithoutAuth(application, reviewer, reason);
    }

    @Transactional
    public Application reviewApplicationWithoutAuth(Application application, User reviewer, ApplicationReviewReason reason) {
        ApplicationReviewer entity = application.getReviewer(reviewer).orElseGet(() -> {
            ApplicationReviewerId id = new ApplicationReviewerId();
            id.setApplicationId(application.getId());
            id.setUserId(reviewer.getId());

            ApplicationReviewer element = new ApplicationReviewer();
            element.setId(id);
            element.setApplication(application);
            element.setUser(reviewer);

            return element;
        });

        ApplicationReviewerId entityId = entity.getId();

        entity.setReason(reason);
        entity.setReviewedAt(Instant.now());

        application.setReviewers(new ArrayList<>(application.getReviewers().stream().filter(element -> !element.getId().equals(entityId)).toList()));

        if (reason == ApplicationReviewReason.NOT_REVIEWED) {
            applicationReviewerRepository.delete(entity);
        } else {
            entity = applicationReviewerRepository.save(entity);

            application.getReviewers().add(entity);
        }

        return applicationRepository.save(application);
    }

    @Transactional
    public Application updateComment(Application application, String comment) {
        currentUserProvider().assertCanAccessResearchGroup(application.getResearchGroup());
        application.setComment(comment);
        return applicationRepository.save(application);
    }

    public boolean applicationExists(User user, UUID topicId) {
        return applicationRepository.existsPendingApplication(user.getId(), topicId);
    }

    public Application findById(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Application with id %s not found.", applicationId)));
        currentUserProvider().assertCanAccessResearchGroup(application.getResearchGroup());
        return application;
    }
}
