package de.tum.cit.aet.thesis.cron;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.MailingService;
import de.tum.cit.aet.thesis.service.ResearchGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/** Scheduled task that sends weekly email reminders about unreviewed applications. */
@Component
public class ApplicationReminder {
	private static final Logger log = LoggerFactory.getLogger(ApplicationReminder.class);
	private final ApplicationRepository applicationRepository;
	private final MailingService mailingService;
	private final UserRepository userRepository;
	private final ResearchGroupService researchGroupService;

	/**
	 * Injects the application repository, mailing service, user repository, and research group service.
	 *
	 * @param applicationRepository the application repository
	 * @param mailingService the mailing service
	 * @param userRepository the user repository
	 * @param researchGroupService the research group service
	 */
	public ApplicationReminder(ApplicationRepository applicationRepository, MailingService mailingService, UserRepository userRepository,
		ResearchGroupService researchGroupService) {
		this.applicationRepository = applicationRepository;
		this.mailingService = mailingService;
		this.userRepository = userRepository;
		this.researchGroupService = researchGroupService;
	}

	@Scheduled(cron = "0 0 10 * * WED")
	public void emailReminder() {
		for (ResearchGroup researchGroup : researchGroupService.getAll(null, null, false, null, 0,
			-1, "name", "asc").getContent()) {
			for (User user : userRepository.getRoleMembers(Set.of("admin", "supervisor", "advisor"),
				researchGroup.getId())) {
				long unreviewedApplications =
					applicationRepository.countUnreviewedApplications(user.getId(),
						user.getResearchGroup().getId());

				if (unreviewedApplications > 0) {
					mailingService.sendApplicationReminderEmail(user, unreviewedApplications);
				}
			}
		}

		log.info("Scheduled task executed at {}", Instant.now());
	}
}
