package de.tum.cit.aet.thesis.cron;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import de.tum.cit.aet.thesis.service.ApplicationService;
import de.tum.cit.aet.thesis.service.TopicService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class AutomaticRejects {
    private final ResearchGroupSettingsRepository researchGroupSettingsRepository;
    private final TopicService topicService;
    private final ApplicationService applicationService;

    public AutomaticRejects(ResearchGroupSettingsRepository researchGroupSettingsRepository, TopicService topicService, ApplicationService applicationService) {
        this.researchGroupSettingsRepository = researchGroupSettingsRepository;
        this.topicService = topicService;
        this.applicationService = applicationService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void rejectOldApplications() {
        List<ResearchGroupSettings> enabledResearchGroups = researchGroupSettingsRepository.findAllByAutomaticRejectEnabled();
        for (ResearchGroupSettings settings : enabledResearchGroups) {
            List<Topic> openTopics = topicService.getOpenFromResearchGroup(settings.getResearchGroupId());
            for (Topic topic : openTopics) {
                Instant referenceDate;

                if (topic.getApplicationDeadline() != null) {
                    referenceDate = topic.getApplicationDeadline();
                } else if (topic.getIntendedStart() != null) {
                    referenceDate = topic.getIntendedStart();
                } else {
                    applicationService.rejectAllApplicationsAutomatically(topic, settings.getRejectDuration());
                    return;
                }

                if (!referenceDate.isBefore(java.time.Instant.now().plus(java.time.Duration.ofDays((long) settings.getRejectDuration() * 7)))) {
                    applicationService.rejectAllApplicationsAutomatically(topic, -1);
                };
            }
        }
    }
}
