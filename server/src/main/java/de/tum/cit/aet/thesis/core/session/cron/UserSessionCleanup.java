package de.tum.cit.aet.thesis.core.session.cron;

import de.tum.cit.aet.thesis.core.session.security.InvalidSessionTokenLimiter;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Housekeeping for user sessions. Session timeouts are enforced on every request, so this job is not a
 * security control: it ends sessions nobody used again and deletes old session history.
 */
@Component
public class UserSessionCleanup {
	private static final Logger log = LoggerFactory.getLogger(UserSessionCleanup.class);

	private final UserSessionService sessionService;
	private final InvalidSessionTokenLimiter limiter;

	/**
	 * Creates the job.
	 *
	 * @param sessionService the session service
	 * @param limiter the limiter whose bookkeeping is pruned together with the sessions
	 */
	public UserSessionCleanup(UserSessionService sessionService, InvalidSessionTokenLimiter limiter) {
		this.sessionService = sessionService;
		this.limiter = limiter;
	}

	/** Ends overdue sessions, deletes ended sessions past their retention period, and prunes the limiter. */
	@Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
	public void run() {
		int deleted = sessionService.cleanup();
		limiter.prune();

		if (deleted > 0) {
			log.info("Deleted {} ended user sessions past their retention period", deleted);
		}
	}
}
