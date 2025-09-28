package de.tum.cit.aet.thesis.cron;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import de.tum.cit.aet.thesis.service.ApplicationService;
import de.tum.cit.aet.thesis.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AutomaticRejects {
    private static final Logger log = LoggerFactory.getLogger(ApplicationReminder.class);
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
        //Reject old applications for each research group that has enabled the feature
        for (ResearchGroupSettings settings : enabledResearchGroups) {
            Map<User, List<Application>> reminderApplicationsByUser = new HashMap<>();

            List<Topic> openTopics = topicService.getOpenFromResearchGroup(settings.getResearchGroupId());
            for (Topic topic : openTopics) {
                Instant referenceDate;

                if (topic.getApplicationDeadline() != null) {
                    referenceDate = topic.getApplicationDeadline();
                } else if (topic.getIntendedStart() != null) {
                    referenceDate = topic.getIntendedStart();
                } else {
                    referenceDate = null;
                }

                applicationService.rejectAllApplicationsAutomatically(topic, settings.getRejectDuration(), referenceDate, settings.getResearchGroupId());

                // Add the applications that will be rejected in the next 7 days to the map for sending reminders
                List<Application> topicRejectsInNextSevenDays = applicationService.getListOfApplicationsThatWillBeRejected(topic, settings.getRejectDuration(), referenceDate);
                topic.getRoles().forEach(role -> {
                    if (role.getId().getRole() == ThesisRoleName.ADVISOR || role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
                        if (!reminderApplicationsByUser.containsKey(role.getUser())) {
                            reminderApplicationsByUser.put(role.getUser(), topicRejectsInNextSevenDays);
                        } else {
                            List<Application> existingApplications = reminderApplicationsByUser.get(role.getUser());
                            existingApplications.addAll(topicRejectsInNextSevenDays);
                            reminderApplicationsByUser.put(role.getUser(), existingApplications);
                        }
                    }
                });
            }

            // Check All Unreviewed Suggested Topics and reject them if older than the duration
            applicationService.rejectListOfApplicationsIfOlderThan(applicationService.getNotAssesedSuggestedOfResearchGroup(settings.getResearchGroupId()), settings.getRejectDuration() * 7, settings.getResearchGroupId());
        }

        log.info("Scheduled Task to automatically reject application ran at ", Instant.now());
    }
}
