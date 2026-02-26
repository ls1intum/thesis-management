package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.jsonb.ThesisMetadata;
import de.tum.cit.aet.thesis.repository.ThesisAssessmentRepository;
import de.tum.cit.aet.thesis.repository.ThesisCommentRepository;
import de.tum.cit.aet.thesis.repository.ThesisFeedbackRepository;
import de.tum.cit.aet.thesis.repository.ThesisFileRepository;
import de.tum.cit.aet.thesis.repository.ThesisPresentationInviteRepository;
import de.tum.cit.aet.thesis.repository.ThesisPresentationRepository;
import de.tum.cit.aet.thesis.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.ThesisStateChangeRepository;
import de.tum.cit.aet.thesis.utility.RetentionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Handles thesis anonymization after the legal retention period expires. */
@Service
public class ThesisAnonymizationService {
	private static final Logger log = LoggerFactory.getLogger(ThesisAnonymizationService.class);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

	private final ThesisRepository thesisRepository;
	private final ThesisFileRepository thesisFileRepository;
	private final ThesisProposalRepository thesisProposalRepository;
	private final ThesisCommentRepository thesisCommentRepository;
	private final ThesisAssessmentRepository thesisAssessmentRepository;
	private final ThesisFeedbackRepository thesisFeedbackRepository;
	private final ThesisPresentationInviteRepository thesisPresentationInviteRepository;
	private final ThesisPresentationRepository thesisPresentationRepository;
	private final ThesisStateChangeRepository thesisStateChangeRepository;
	private final ThesisRoleRepository thesisRoleRepository;
	private final UploadService uploadService;
	private final MailingService mailingService;
	private final int notificationLeadDays;

	public ThesisAnonymizationService(
			ThesisRepository thesisRepository,
			ThesisFileRepository thesisFileRepository,
			ThesisProposalRepository thesisProposalRepository,
			ThesisCommentRepository thesisCommentRepository,
			ThesisAssessmentRepository thesisAssessmentRepository,
			ThesisFeedbackRepository thesisFeedbackRepository,
			ThesisPresentationInviteRepository thesisPresentationInviteRepository,
			ThesisPresentationRepository thesisPresentationRepository,
			ThesisStateChangeRepository thesisStateChangeRepository,
			ThesisRoleRepository thesisRoleRepository,
			UploadService uploadService,
			MailingService mailingService,
			@Value("${thesis-management.data-retention.thesis-anonymization-notification-days}") int notificationLeadDays) {
		this.thesisRepository = thesisRepository;
		this.thesisFileRepository = thesisFileRepository;
		this.thesisProposalRepository = thesisProposalRepository;
		this.thesisCommentRepository = thesisCommentRepository;
		this.thesisAssessmentRepository = thesisAssessmentRepository;
		this.thesisFeedbackRepository = thesisFeedbackRepository;
		this.thesisPresentationInviteRepository = thesisPresentationInviteRepository;
		this.thesisPresentationRepository = thesisPresentationRepository;
		this.thesisStateChangeRepository = thesisStateChangeRepository;
		this.thesisRoleRepository = thesisRoleRepository;
		this.uploadService = uploadService;
		this.mailingService = mailingService;
		this.notificationLeadDays = notificationLeadDays;
	}

	/**
	 * Sends notification emails to research group heads for theses approaching anonymization.
	 * Theses within the notification lead period that haven't been notified yet will trigger
	 * one email per research group.
	 */
	public void sendAnonymizationNotifications() {
		Instant now = Instant.now();
		Instant notificationHorizon = now.plus(notificationLeadDays, ChronoUnit.DAYS);

		List<Thesis> candidates = thesisRepository.findAnonymizationCandidates();

		// Group theses that need notification by research group
		Map<UUID, List<Thesis>> thesesByResearchGroup = new HashMap<>();

		for (Thesis thesis : candidates) {
			if (thesis.getAnonymizationNotifiedAt() != null) {
				continue;
			}

			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);
			if (expiry.isBefore(notificationHorizon)) {
				UUID rgId = thesis.getResearchGroup().getId();
				thesesByResearchGroup.computeIfAbsent(rgId, k -> new ArrayList<>()).add(thesis);
			}
		}

		for (Map.Entry<UUID, List<Thesis>> entry : thesesByResearchGroup.entrySet()) {
			List<Thesis> theses = entry.getValue();
			if (theses.isEmpty()) {
				continue;
			}

			try {
				Thesis firstThesis = theses.getFirst();
				Instant expiry = RetentionUtils.computeRetentionExpiry(firstThesis);
				String anonymizationDate = expiry.atZone(ZoneId.of("Europe/Berlin")).format(DATE_FORMATTER);

				mailingService.sendThesisAnonymizationReminderEmail(
						firstThesis.getResearchGroup(),
						theses,
						anonymizationDate
				);

				for (Thesis thesis : theses) {
					thesis.setAnonymizationNotifiedAt(now);
					thesisRepository.save(thesis);
				}

				log.info("Sent anonymization notification for {} theses in research group {}",
						theses.size(), firstThesis.getResearchGroup().getName());
			} catch (Exception e) {
				log.error("Failed to send anonymization notification for research group {}: {}",
						entry.getKey(), e.getMessage(), e);
			}
		}
	}

	/**
	 * Anonymizes all theses whose retention period has expired.
	 *
	 * @return the number of anonymized theses
	 */
	public int anonymizeExpiredTheses() {
		Instant now = Instant.now();
		List<Thesis> candidates = thesisRepository.findAnonymizationCandidates();

		int anonymizedCount = 0;

		for (Thesis thesis : candidates) {
			Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);
			if (!expiry.isBefore(now)) {
				continue;
			}

			try {
				anonymizeThesis(thesis);
				anonymizedCount++;
				log.info("Anonymized thesis {} ('{}')", thesis.getId(), thesis.getTitle());
			} catch (Exception e) {
				log.error("Failed to anonymize thesis {}: {}", thesis.getId(), e.getMessage(), e);
			}
		}

		if (anonymizedCount > 0) {
			log.info("Thesis anonymization complete: {} theses anonymized", anonymizedCount);
		}

		return anonymizedCount;
	}

	private void anonymizeThesis(Thesis thesis) {
		UUID thesisId = thesis.getId();

		// 1. Collect all file paths before deleting records
		List<String> filenames = new ArrayList<>();
		filenames.addAll(thesisFileRepository.findFilenamesByThesisId(thesisId));
		filenames.addAll(thesisProposalRepository.findFilenamesByThesisId(thesisId));
		filenames.addAll(thesisCommentRepository.findFilenamesByThesisId(thesisId));

		// 2. Delete child records in correct FK order
		thesisPresentationInviteRepository.deleteAllByPresentationThesisId(thesisId);
		thesisPresentationRepository.deleteAllByThesisId(thesisId);
		thesisCommentRepository.deleteAllByThesisId(thesisId);
		thesisFileRepository.deleteAllByThesisId(thesisId);
		thesisProposalRepository.deleteAllByThesisId(thesisId);
		thesisAssessmentRepository.deleteAllByThesisId(thesisId);
		thesisFeedbackRepository.deleteAllByThesisId(thesisId);
		thesisStateChangeRepository.deleteAllByThesisId(thesisId);
		thesisRoleRepository.deleteAllByThesisId(thesisId);

		// 3. Re-fetch thesis to avoid stale Hibernate references after bulk deletes
		thesis = thesisRepository.findById(thesisId).orElseThrow();

		// 4. Anonymize thesis record: clear personal/content fields, preserve structural data
		thesis.setInfo("");
		thesis.setAbstractField("");
		thesis.setFinalFeedback(null);
		thesis.setKeywords(new HashSet<>());
		thesis.setMetadata(new ThesisMetadata(new HashMap<>(), new HashMap<>()));
		thesis.setApplication(null);
		thesis.setAnonymizedAt(Instant.now());

		thesisRepository.save(thesis);

		// 4. Delete collected files from disk
		for (String filename : filenames) {
			uploadService.deleteFile(filename);
		}
	}
}
