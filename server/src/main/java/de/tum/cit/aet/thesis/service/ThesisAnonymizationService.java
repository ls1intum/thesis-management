package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ThesisState;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Handles thesis anonymization after the legal retention period expires. */
@Service
public class ThesisAnonymizationService {
	private static final Logger log = LoggerFactory.getLogger(ThesisAnonymizationService.class);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

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

	/**
	 * Constructs a new ThesisAnonymizationService with the required repositories and services.
	 *
	 * @param thesisRepository the thesis repository
	 * @param thesisFileRepository the thesis file repository
	 * @param thesisProposalRepository the thesis proposal repository
	 * @param thesisCommentRepository the thesis comment repository
	 * @param thesisAssessmentRepository the thesis assessment repository
	 * @param thesisFeedbackRepository the thesis feedback repository
	 * @param thesisPresentationInviteRepository the thesis presentation invite repository
	 * @param thesisPresentationRepository the thesis presentation repository
	 * @param thesisStateChangeRepository the thesis state change repository
	 * @param thesisRoleRepository the thesis role repository
	 * @param uploadService the upload service for file operations
	 * @param mailingService the mailing service for notifications
	 * @param notificationLeadDays the number of days before anonymization to send notifications
	 */
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

				if (firstThesis.getResearchGroup().getHead() == null) {
					log.warn("Skipping anonymization reminder for research group {}: no head assigned", entry.getKey());
					continue;
				}

				// Compute the earliest expiry date across all theses for the email subject line
				Instant earliestExpiry = theses.stream()
						.map(RetentionUtils::computeRetentionExpiry)
						.min(Instant::compareTo)
						.orElse(RetentionUtils.computeRetentionExpiry(firstThesis));
				String anonymizationDate = earliestExpiry.atZone(RetentionUtils.BERLIN).format(DATE_FORMATTER);

				// Send email first — only mark as notified on success.
				// Duplicate notifications on retry are preferable to zero notifications before data destruction.
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

	/**
	 * Computes warnings about anonymizing the given thesis.
	 *
	 * @param thesis the thesis to check
	 * @return a list of warning messages (empty if no warnings)
	 */
	public List<String> computeAnonymizationWarnings(Thesis thesis) {
		List<String> warnings = new ArrayList<>();

		if (thesis.isAnonymized()) {
			warnings.add("This thesis has already been anonymized.");
			return warnings;
		}

		if (thesis.getState() != ThesisState.FINISHED && thesis.getState() != ThesisState.DROPPED_OUT) {
			warnings.add("This thesis is in state " + thesis.getState() + " and has not been finished or dropped out.");
		}

		Instant expiry = RetentionUtils.computeRetentionExpiry(thesis);
		if (!expiry.isBefore(Instant.now())) {
			String expiryDate = expiry.atZone(RetentionUtils.BERLIN).format(DATE_FORMATTER);
			warnings.add("The retention period for this thesis has not expired yet. It expires on " + expiryDate + ".");
		}

		return warnings;
	}

	/**
	 * Anonymizes a single thesis by clearing personal data and deleting all associated child records.
	 * Structural data (title, type, grade, dates) is preserved for statistical purposes.
	 *
	 * @param thesis the thesis to anonymize
	 */
	public void anonymizeThesis(Thesis thesis) {
		UUID thesisId = thesis.getId();

		// 1. Collect all file paths before deleting records
		List<String> filenames = new ArrayList<>();
		filenames.addAll(thesisFileRepository.findFilenamesByThesisId(thesisId));
		filenames.addAll(thesisProposalRepository.findFilenamesByThesisId(thesisId));
		filenames.addAll(thesisCommentRepository.findFilenamesByThesisId(thesisId));

		// 2. Mark thesis as anonymized and clear personal data FIRST.
		//    findAnonymizationCandidates() filters by anonymizedAt IS NULL, so after this save
		//    the thesis won't be picked up again even if subsequent child deletion fails.
		thesis.setAnonymizedAt(Instant.now());
		thesis.setInfo("");
		thesis.setAbstractField("");
		thesis.setFinalFeedback(null);
		thesis.setKeywords(new HashSet<>());
		thesis.setMetadata(new ThesisMetadata(new HashMap<>(), new HashMap<>()));
		thesis.setApplication(null);
		thesisRepository.save(thesis);

		// 3. Delete child records in correct FK order.
		//    This must happen after saving the thesis to avoid Hibernate stale-state conflicts:
		//    the Thesis entity eagerly loads states and roles, so bulk-deleting them before save
		//    would leave the persistence context with references to non-existent rows.
		try {
			thesisPresentationInviteRepository.deleteAllByPresentationThesisId(thesisId);
			thesisPresentationRepository.deleteAllByThesisId(thesisId);
			thesisCommentRepository.deleteAllByThesisId(thesisId);
			thesisFileRepository.deleteAllByThesisId(thesisId);
			thesisProposalRepository.deleteAllByThesisId(thesisId);
			thesisAssessmentRepository.deleteAllByThesisId(thesisId);
			thesisFeedbackRepository.deleteAllByThesisId(thesisId);
			thesisStateChangeRepository.deleteAllByThesisId(thesisId);
			thesisRoleRepository.deleteAllByThesisId(thesisId);
		} catch (Exception ex) {
			// Keep thesis eligible for retry if cleanup failed
			thesis.setAnonymizedAt(null);
			thesisRepository.save(thesis);
			throw ex;
		}

		// 4. Delete collected files from disk (best-effort, non-critical)
		for (String filename : filenames) {
			try {
				uploadService.deleteFile(filename);
			} catch (Exception e) {
				log.warn("Failed to delete file '{}' for thesis {}: {}", filename, thesisId, e.getMessage());
			}
		}
	}
}
