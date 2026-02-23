package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.repository.ApplicationRepository;
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
	private final int retentionDays;

	public DataRetentionService(ApplicationRepository applicationRepository,
			@Value("${thesis-management.data-retention.rejected-application-retention-days}") int retentionDays) {
		this.applicationRepository = applicationRepository;
		this.retentionDays = retentionDays;
	}

	@Scheduled(cron = "${thesis-management.data-retention.cron}")
	public void runNightlyCleanup() {
		deleteExpiredRejectedApplications();
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
