package de.tum.cit.aet.thesis.cron;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.cron.model.ApplicationRejectObject;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import de.tum.cit.aet.thesis.service.ApplicationService;
import de.tum.cit.aet.thesis.service.MailingService;
import de.tum.cit.aet.thesis.service.TopicService;
import de.tum.cit.aet.thesis.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final MailingService mailingService;
    private final UserService userService;

    public AutomaticRejects(ResearchGroupSettingsRepository researchGroupSettingsRepository, TopicService topicService, ApplicationService applicationService, MailingService mailingService, UserService userService) {
        this.researchGroupSettingsRepository = researchGroupSettingsRepository;
        this.topicService = topicService;
        this.applicationService = applicationService;
        this.mailingService = mailingService;
        this.userService = userService;
    }

    @Scheduled(cron = "0 32 00 * * *")
    @Transactional
    public void rejectOldApplications() {
        List<ResearchGroupSettings> enabledResearchGroups = researchGroupSettingsRepository.findAllByAutomaticRejectEnabled();
        //Reject old applications for each research group that has enabled the feature
        for (ResearchGroupSettings settings : enabledResearchGroups) {
            Map<User, List<ApplicationRejectObject>> reminderApplicationsByUser = new HashMap<>();

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
                List<ApplicationRejectObject> topicRejectsInNextSevenDays = applicationService.getListOfApplicationsThatWillBeRejected(topic, settings.getRejectDuration(), referenceDate);
                if (!topicRejectsInNextSevenDays.isEmpty()) {
                    topic.getRoles().forEach(role -> {
                        if (role.getId().getRole() == ThesisRoleName.ADVISOR || role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
                            User loadEntireUser = userService.findById(role.getUser().getId());

                            if (!reminderApplicationsByUser.containsKey(role.getUser())) {
                                reminderApplicationsByUser.put(loadEntireUser , topicRejectsInNextSevenDays);
                            } else {
                                List<ApplicationRejectObject> existingApplications = reminderApplicationsByUser.get(role.getUser());
                                existingApplications.addAll(topicRejectsInNextSevenDays);
                                existingApplications = existingApplications.stream().distinct().toList();
                                reminderApplicationsByUser.put(loadEntireUser, existingApplications);
                            }
                        }
                    });
                }
            }


            // Check All Unreviewed Suggested Topics and reject them if older than the duration
            applicationService.rejectListOfApplicationsIfOlderThan(applicationService.getNotAssesedSuggestedOfResearchGroup(settings.getResearchGroupId()), settings.getRejectDuration() * 7, settings.getResearchGroupId());

            for (User user : reminderApplicationsByUser.keySet()) {
                mailingService.sendApplicationAutomaticRejectReminderEmail(user, reminderApplicationsByUser.get(user));
            }
        }

        log.info("Scheduled Task to automatically reject application ran at {}", Instant.now());
    }
}
