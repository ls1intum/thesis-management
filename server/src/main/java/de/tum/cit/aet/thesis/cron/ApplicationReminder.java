package de.tum.cit.aet.thesis.cron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.MailingService;

import java.time.Instant;
import java.util.Set;

@Component
public class ApplicationReminder {
    private static final Logger log = LoggerFactory.getLogger(ApplicationReminder.class);
    private final ApplicationRepository applicationRepository;
    private final MailingService mailingService;
    private final UserRepository userRepository;

    public ApplicationReminder(ApplicationRepository applicationRepository, MailingService mailingService, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.mailingService = mailingService;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 10 * * WED")
    public void emailReminder() {
        for (User user : userRepository.getRoleMembers(Set.of("admin", "supervisor", "advisor"))) {
            long unreviewedApplications = applicationRepository.countUnreviewedApplications(user.getId());

            if (unreviewedApplications > 0) {
                mailingService.sendApplicationReminderEmail(user, unreviewedApplications);
            }
        }

        log.info("Scheduled task executed at {}", Instant.now());
    }
}
