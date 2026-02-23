package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
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
	private final UserRepository userRepository;
	private final DataExportService dataExportService;
	private final int retentionDays;
	private final int inactiveUserDays;

	public DataRetentionService(ApplicationRepository applicationRepository,
			UserRepository userRepository,
			DataExportService dataExportService,
			@Value("${thesis-management.data-retention.rejected-application-retention-days}") int retentionDays,
			@Value("${thesis-management.data-retention.inactive-user-days}") int inactiveUserDays) {
		this.applicationRepository = applicationRepository;
		this.userRepository = userRepository;
		this.dataExportService = dataExportService;
		this.retentionDays = retentionDays;
		this.inactiveUserDays = inactiveUserDays;
	}

	@Scheduled(cron = "${thesis-management.data-retention.cron}")
	public void runNightlyCleanup() {
		deleteExpiredRejectedApplications();
		disableInactiveUsers();
		dataExportService.processAllPendingExports();
		dataExportService.deleteExpiredExports();
	}

	public int disableInactiveUsers() {
		Instant cutoff = Instant.now().minus(inactiveUserDays, ChronoUnit.DAYS);

		List<User> candidates = userRepository.findInactiveStudentCandidates(cutoff);

		List<User> toDisable = candidates.stream()
				.filter(user -> hasNoRecentActivity(user, cutoff))
				.toList();

		if (toDisable.isEmpty()) {
			return 0;
		}

		for (User user : toDisable) {
			user.setDisabled(true);
			userRepository.save(user);
		}

		log.info("Disabled {} inactive student accounts (inactive for more than {} days)", toDisable.size(), inactiveUserDays);

		return toDisable.size();
	}

	private boolean hasNoRecentActivity(User user, Instant cutoff) {
		boolean hasRecentApplication = applicationRepository.findAllByUser(user).stream()
				.anyMatch(app -> app.getCreatedAt().isAfter(cutoff));

		return !hasRecentApplication;
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
