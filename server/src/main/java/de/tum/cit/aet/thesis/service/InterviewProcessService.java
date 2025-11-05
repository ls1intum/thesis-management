package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.repository.InterviewProcessRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

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

    public InterviewProcess createInterviewProcess(UUID topicId) {
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
}
