package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.dto.UserDeletionPreviewDto;
import de.tum.cit.aet.thesis.dto.UserDeletionResultDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.DataExport;
import de.tum.cit.aet.thesis.entity.ThesisRole;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
import de.tum.cit.aet.thesis.repository.DataExportRepository;
import de.tum.cit.aet.thesis.repository.NotificationSettingRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.TopicRoleRepository;
import de.tum.cit.aet.thesis.repository.UserGroupRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.utility.RetentionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/** Handles user account deletion, anonymization, and deferred cleanup with legal retention enforcement. */
@Service
public class UserDeletionService {
	private static final Logger log = LoggerFactory.getLogger(UserDeletionService.class);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

	private final UserRepository userRepository;
	private final ThesisRoleRepository thesisRoleRepository;
	private final TopicRoleRepository topicRoleRepository;
	private final ApplicationRepository applicationRepository;
	private final ApplicationReviewerRepository applicationReviewerRepository;
	private final ResearchGroupRepository researchGroupRepository;
	private final DataExportRepository dataExportRepository;
	private final UserGroupRepository userGroupRepository;
	private final NotificationSettingRepository notificationSettingRepository;
	private final UploadService uploadService;
	private final Path dataExportPath;

	/**
	 * Constructs the service with the required repositories, upload service, and export path.
	 *
	 * @param userRepository the user repository
	 * @param thesisRoleRepository the thesis role repository
	 * @param topicRoleRepository the topic role repository
	 * @param applicationRepository the application repository
	 * @param applicationReviewerRepository the application reviewer repository
	 * @param researchGroupRepository the research group repository
	 * @param dataExportRepository the data export repository
	 * @param userGroupRepository the user group repository
	 * @param notificationSettingRepository the notification setting repository
	 * @param uploadService the upload service
	 * @param dataExportPath the data export directory path
	 */
	public UserDeletionService(
			UserRepository userRepository,
			ThesisRoleRepository thesisRoleRepository,
			TopicRoleRepository topicRoleRepository,
			ApplicationRepository applicationRepository,
			ApplicationReviewerRepository applicationReviewerRepository,
			ResearchGroupRepository researchGroupRepository,
			DataExportRepository dataExportRepository,
			UserGroupRepository userGroupRepository,
			NotificationSettingRepository notificationSettingRepository,
			UploadService uploadService,
			@Value("${thesis-management.data-export.path}") String dataExportPath) {
		this.userRepository = userRepository;
		this.thesisRoleRepository = thesisRoleRepository;
		this.topicRoleRepository = topicRoleRepository;
		this.applicationRepository = applicationRepository;
		this.applicationReviewerRepository = applicationReviewerRepository;
		this.researchGroupRepository = researchGroupRepository;
		this.dataExportRepository = dataExportRepository;
		this.userGroupRepository = userGroupRepository;
		this.notificationSettingRepository = notificationSettingRepository;
		this.uploadService = uploadService;
		this.dataExportPath = Path.of(dataExportPath);
	}

	/**
	 * Returns a preview of what would happen if the given user account were deleted.
	 *
	 * @param userId the user identifier
	 * @return the deletion preview
	 */
	public UserDeletionPreviewDto previewDeletion(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		boolean isResearchGroupHead = researchGroupRepository.existsByHeadId(userId);
		List<ThesisRole> retentionBlockedRoles = getRetentionBlockedThesisRoles(userId);
		int retentionBlockedCount = (int) retentionBlockedRoles.stream()
				.map(r -> r.getThesis().getId())
				.distinct()
				.count();
		Instant earliestDeletion = computeEarliestFullDeletion(retentionBlockedRoles);
		boolean canBeFullyDeleted = !isResearchGroupHead && retentionBlockedCount == 0;

		String message;
		if (isResearchGroupHead) {
			message = "You must transfer research group leadership before deleting your account.";
		} else if (canBeFullyDeleted) {
			message = "Your account and all associated data will be permanently deleted.";
		} else {
			message = "Your account will be deactivated and non-essential data deleted immediately. "
					+ "Your profile and thesis data (" + retentionBlockedCount
					+ " thesis/theses) must be retained until " + formatDate(earliestDeletion)
					+ " per legal requirements, then everything will be fully deleted.";
		}

		return new UserDeletionPreviewDto(
				canBeFullyDeleted,
				retentionBlockedCount,
				earliestDeletion,
				isResearchGroupHead,
				message
		);
	}

	/**
	 * Deletes or soft-deletes the user account depending on legal retention requirements.
	 *
	 * @param userId the user identifier
	 * @return the deletion result
	 */
	public UserDeletionResultDto deleteOrAnonymizeUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (user.isAnonymized() || user.getDeletionRequestedAt() != null) {
			throw new AccessDeniedException("This account has already been deleted");
		}

		if (researchGroupRepository.existsByHeadId(userId)) {
			throw new AccessDeniedException("Cannot delete account while being a research group head. Transfer leadership first.");
		}

		// Delete freely-deletable applications (rejected/not-assessed)
		deleteNonRetainedApplications(userId);

		// Collect export file paths before deleting DB records, then delete files after
		List<String> exportFilePaths = collectExportFilePaths(user);
		deleteDataExportRecords(user);

		List<ThesisRole> retentionBlockedRoles = getRetentionBlockedThesisRoles(userId);

		UserDeletionResultDto result;
		if (retentionBlockedRoles.isEmpty()) {
			// No retention — delete the account first, then clean up files
			result = performFullDeletion(user);
			deleteAllUserFiles(user);
		} else {
			// Retention active — only delete avatar (cosmetic), keep CV/degree/exam
			// as they are part of the thesis evaluation process.
			String avatarPath = user.getAvatar();
			result = performSoftDeletion(user, retentionBlockedRoles);
			uploadService.deleteFile(avatarPath);
		}

		// Delete export files after DB operations succeeded (worst case: orphaned files)
		deleteExportFiles(exportFilePaths);

		return result;
	}

	/** Processes all users whose deferred deletion date has passed and performs full cleanup. */
	public void processDeferredDeletions() {
		List<User> pendingUsers = userRepository.findAllByDeletionScheduledForIsNotNull();

		for (User user : pendingUsers) {
			try {
				UUID userId = user.getId();
				List<ThesisRole> retentionBlocked = getRetentionBlockedThesisRoles(userId);
				if (retentionBlocked.isEmpty()) {
					log.info("Retention expired for user {}, performing full cleanup", userId);

					// Collect file paths before DB changes
					List<String> exportFilePaths = collectExportFilePaths(user);
					List<String> userFilePaths = collectUserFilePaths(user);

					// Delete all remaining related data
					deleteDataExportRecords(user);
					List<Application> remainingApps = applicationRepository.findAllByUserId(userId);
					for (Application app : remainingApps) {
						applicationReviewerRepository.deleteByApplicationId(app.getId());
					}
					applicationRepository.deleteAllByUserId(userId);
					topicRoleRepository.deleteAllByIdUserId(userId);
					thesisRoleRepository.deleteAllByIdUserId(userId);

					// Fully anonymize the tombstone (clear name + file references)
					anonymizeUser(user);
					userRepository.clearDeletionScheduledFor(userId);

					// Delete files after DB operations succeeded
					deleteFilePaths(userFilePaths);
					deleteExportFiles(exportFilePaths);
				}
			} catch (Exception e) {
				log.error("Failed to process deferred deletion for user {}: {}", user.getId(), e.getMessage(), e);
			}
		}
	}

	private UserDeletionResultDto performFullDeletion(User user) {
		UUID userId = user.getId();

		// Delete remaining applications and their reviewers via JPQL to avoid
		// Hibernate session conflicts with eagerly-loaded collections.
		List<Application> remainingApps = applicationRepository.findAllByUserId(userId);
		for (Application app : remainingApps) {
			applicationReviewerRepository.deleteByApplicationId(app.getId());
		}
		applicationRepository.deleteAllByUserId(userId);

		// Delete topic roles
		topicRoleRepository.deleteAllByIdUserId(userId);

		// Delete thesis roles (should be empty if no retention-blocked data)
		thesisRoleRepository.deleteAllByIdUserId(userId);

		// Anonymize the user BEFORE deleting user groups and notification settings.
		// User.groups is eagerly loaded, so bulk-deleting UserGroup rows first would
		// leave stale references in the persistence context, causing Hibernate errors on save.
		anonymizeUser(user);

		// Delete user-owned data (safe to do after anonymizeUser since we don't save the User again)
		notificationSettingRepository.deleteByUserId(userId);
		userGroupRepository.deleteByUserId(userId);

		log.info("Fully deleted user account {}", userId);
		return new UserDeletionResultDto("DELETED", "Your account and all associated data have been permanently deleted.");
	}

	private UserDeletionResultDto performSoftDeletion(User user, List<ThesisRole> retentionBlockedRoles) {
		Instant now = Instant.now();
		Instant earliestDeletion = computeEarliestFullDeletion(retentionBlockedRoles);

		// Deactivate the account but keep name and thesis-related files intact
		// so thesis records remain searchable during the legal retention period.
		// Note: anonymizedAt is NOT set here because the user is not fully anonymized
		// (name, matriculation number, and thesis files are preserved for retention).
		user.setDisabled(true);
		user.setDeletionRequestedAt(now);
		user.setDeletionScheduledFor(earliestDeletion);

		// Clear non-essential data
		user.setEmail(null);
		user.setGender(null);
		user.setNationality(null);
		user.setStudyDegree(null);
		user.setStudyProgram(null);
		user.setEnrolledAt(null);
		user.setAvatar(null);
		user.setProjects(null);
		user.setInterests(null);
		user.setSpecialSkills(null);
		user.setCustomData(new HashMap<>());
		user.setResearchGroup(null);

		// Keep: universityId, firstName, lastName, matriculationNumber,
		// cvFilename, degreeFilename, examinationFilename (needed for thesis evaluation)

		userRepository.save(user);

		// Delete notification settings and user groups (not needed during retention)
		notificationSettingRepository.deleteByUserId(user.getId());
		userGroupRepository.deleteByUserId(user.getId());

		log.info("Soft-deleted user account {}, full deletion scheduled for {}", user.getId(), earliestDeletion);
		return new UserDeletionResultDto("DEACTIVATED",
				"Your account has been deactivated and non-essential data deleted. "
						+ "Your profile and thesis data will be fully deleted after the legal retention period expires ("
						+ formatDate(earliestDeletion) + ").");
	}

	/**
	 * Converts the user row into a minimal tombstone that prevents re-creation
	 * via Keycloak SSO. Only universityId is preserved for identification;
	 * all personal data is cleared.
	 */
	private void anonymizeUser(User user) {
		Instant now = Instant.now();
		user.setDisabled(true);
		user.setAnonymizedAt(now);
		user.setDeletionRequestedAt(now);
		user.setFirstName(null);
		user.setLastName(null);
		user.setEmail(null);
		user.setMatriculationNumber(null);
		user.setGender(null);
		user.setNationality(null);
		user.setStudyDegree(null);
		user.setStudyProgram(null);
		user.setEnrolledAt(null);
		user.setAvatar(null);
		user.setCvFilename(null);
		user.setDegreeFilename(null);
		user.setExaminationFilename(null);
		user.setProjects(null);
		user.setInterests(null);
		user.setSpecialSkills(null);
		user.setCustomData(new HashMap<>());
		user.setResearchGroup(null);
		userRepository.save(user);
	}

	private List<String> collectUserFilePaths(User user) {
		return java.util.stream.Stream.of(
				user.getCvFilename(), user.getDegreeFilename(),
				user.getExaminationFilename(), user.getAvatar())
				.filter(f -> f != null && !f.isBlank())
				.toList();
	}

	private void deleteAllUserFiles(User user) {
		uploadService.deleteFile(user.getCvFilename());
		uploadService.deleteFile(user.getDegreeFilename());
		uploadService.deleteFile(user.getExaminationFilename());
		uploadService.deleteFile(user.getAvatar());
	}

	private void deleteFilePaths(List<String> filenames) {
		for (String filename : filenames) {
			uploadService.deleteFile(filename);
		}
	}

	private List<ThesisRole> getRetentionBlockedThesisRoles(UUID userId) {
		Instant now = Instant.now();
		return thesisRoleRepository.findAllByIdUserIdWithThesis(userId).stream()
				.filter(role -> RetentionUtils.computeRetentionExpiry(role.getThesis()).isAfter(now))
				.toList();
	}

	private Instant computeEarliestFullDeletion(List<ThesisRole> retentionBlockedRoles) {
		return retentionBlockedRoles.stream()
				.map(role -> RetentionUtils.computeRetentionExpiry(role.getThesis()))
				.max(Instant::compareTo)
				.orElse(null);
	}

	private String formatDate(Instant instant) {
		if (instant == null) {
			return "unknown";
		}
		return instant.atZone(ZoneId.of("Europe/Berlin")).format(DATE_FORMATTER);
	}

	private void deleteNonRetainedApplications(UUID userId) {
		List<Application> applications = applicationRepository.findAllByUserId(userId);
		for (Application app : applications) {
			if (app.getState() == ApplicationState.REJECTED || app.getState() == ApplicationState.NOT_ASSESSED) {
				applicationReviewerRepository.deleteByApplicationId(app.getId());
				applicationRepository.deleteApplicationById(app.getId());
			}
		}
	}

	private List<String> collectExportFilePaths(User user) {
		return dataExportRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
				.map(DataExport::getFilePath)
				.filter(p -> p != null)
				.toList();
	}

	private void deleteDataExportRecords(User user) {
		List<DataExport> exports = dataExportRepository.findAllByUserOrderByCreatedAtDesc(user);
		dataExportRepository.deleteAll(exports);
	}

	private void deleteExportFiles(List<String> filePaths) {
		java.nio.file.Path safeBase = dataExportPath.normalize();
		for (String path : filePaths) {
			try {
				java.nio.file.Path filePath = java.nio.file.Path.of(path).normalize();
				if (filePath.startsWith(safeBase)) {
					java.nio.file.Files.deleteIfExists(filePath);
				} else {
					log.warn("Skipping export file deletion outside expected directory: {}", path);
				}
			} catch (java.io.IOException e) {
				log.warn("Failed to delete export file {}: {}", path, e.getMessage());
			}
		}
	}
}
