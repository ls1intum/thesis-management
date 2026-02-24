package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class DataRetentionService {
	private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

	private final ApplicationRepository applicationRepository;
	private final ApplicationReviewerRepository applicationReviewerRepository;
	private final UserRepository userRepository;
	private final DataExportService dataExportService;
	private final UserDeletionService userDeletionService;
	private final int retentionDays;
	private final int inactiveUserDays;

	public DataRetentionService(ApplicationRepository applicationRepository,
			ApplicationReviewerRepository applicationReviewerRepository,
			UserRepository userRepository,
			DataExportService dataExportService,
			UserDeletionService userDeletionService,
			@Value("${thesis-management.data-retention.rejected-application-retention-days}") int retentionDays,
			@Value("${thesis-management.data-retention.inactive-user-days}") int inactiveUserDays) {
		this.applicationRepository = applicationRepository;
		this.applicationReviewerRepository = applicationReviewerRepository;
		this.userRepository = userRepository;
		this.dataExportService = dataExportService;
		this.userDeletionService = userDeletionService;
		this.retentionDays = retentionDays;
		this.inactiveUserDays = inactiveUserDays;
	}

	@Scheduled(cron = "${thesis-management.data-retention.cron}")
	public void runNightlyCleanup() {
		runStep("deleteExpiredRejectedApplications", this::deleteExpiredRejectedApplications);
		runStep("disableInactiveUsers", this::disableInactiveUsers);
		runStep("processAllPendingExports", dataExportService::processAllPendingExports);
		runStep("deleteExpiredExports", dataExportService::deleteExpiredExports);
		runStep("processDeferredDeletions", userDeletionService::processDeferredDeletions);
	}

	private void runStep(String name, Runnable step) {
		try {
			step.run();
		} catch (Exception e) {
			log.error("Nightly cleanup step '{}' failed: {}", name, e.getMessage(), e);
		}
	}

	public int disableInactiveUsers() {
		Instant cutoff = Instant.now().minus(inactiveUserDays, ChronoUnit.DAYS);

		List<User> toDisable = userRepository.findInactiveStudentCandidates(cutoff);

		if (toDisable.isEmpty()) {
			return 0;
		}

		toDisable.forEach(user -> user.setDisabled(true));
		userRepository.saveAll(toDisable);

		log.info("Disabled {} inactive student accounts (inactive for more than {} days)", toDisable.size(), inactiveUserDays);

		return toDisable.size();
	}

	public int deleteExpiredRejectedApplications() {
		Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

		List<UUID> expiredIds = applicationRepository.findExpiredRejectedApplicationIds(cutoffDate);

		if (expiredIds.isEmpty()) {
			return 0;
		}

		int totalDeleted = 0;
		int totalFailed = 0;

		for (UUID id : expiredIds) {
			try {
				applicationReviewerRepository.deleteByApplicationId(id);
				applicationRepository.deleteApplicationById(id);
				totalDeleted++;
			} catch (Exception e) {
				log.error("Failed to delete rejected application {}: {}", id, e.getMessage());
				totalFailed++;
			}
		}

		log.info("Data retention cleanup: deleted {} rejected applications, {} failures (retention: {} days)",
				totalDeleted, totalFailed, retentionDays);

		return totalDeleted;
	}
}
