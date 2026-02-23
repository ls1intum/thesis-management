package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.dto.UserDeletionPreviewDto;
import de.tum.cit.aet.thesis.dto.UserDeletionResultDto;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.DataExport;
import de.tum.cit.aet.thesis.entity.ThesisRole;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.DataExportRepository;
import de.tum.cit.aet.thesis.repository.NotificationSettingRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.TopicRoleRepository;
import de.tum.cit.aet.thesis.repository.UserGroupRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserDeletionService {
	private static final Logger log = LoggerFactory.getLogger(UserDeletionService.class);
	private static final int RETENTION_YEARS = 5;
	private static final Set<ThesisState> TERMINAL_STATES = Set.of(ThesisState.FINISHED, ThesisState.DROPPED_OUT);

	private final UserRepository userRepository;
	private final ThesisRoleRepository thesisRoleRepository;
	private final TopicRoleRepository topicRoleRepository;
	private final ApplicationRepository applicationRepository;
	private final ResearchGroupRepository researchGroupRepository;
	private final DataExportRepository dataExportRepository;
	private final UserGroupRepository userGroupRepository;
	private final NotificationSettingRepository notificationSettingRepository;
	private final UploadService uploadService;

	public UserDeletionService(
			UserRepository userRepository,
			ThesisRoleRepository thesisRoleRepository,
			TopicRoleRepository topicRoleRepository,
			ApplicationRepository applicationRepository,
			ResearchGroupRepository researchGroupRepository,
			DataExportRepository dataExportRepository,
			UserGroupRepository userGroupRepository,
			NotificationSettingRepository notificationSettingRepository,
			UploadService uploadService) {
		this.userRepository = userRepository;
		this.thesisRoleRepository = thesisRoleRepository;
		this.topicRoleRepository = topicRoleRepository;
		this.applicationRepository = applicationRepository;
		this.researchGroupRepository = researchGroupRepository;
		this.dataExportRepository = dataExportRepository;
		this.userGroupRepository = userGroupRepository;
		this.notificationSettingRepository = notificationSettingRepository;
		this.uploadService = uploadService;
	}

	public UserDeletionPreviewDto previewDeletion(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		boolean isResearchGroupHead = researchGroupRepository.existsByHeadId(userId);
		boolean hasActiveTheses = hasActiveTheses(userId);
		List<ThesisRole> retentionBlockedRoles = getRetentionBlockedThesisRoles(userId);
		int retentionBlockedCount = (int) retentionBlockedRoles.stream()
				.map(r -> r.getThesis().getId())
				.distinct()
				.count();
		Instant earliestDeletion = computeEarliestFullDeletion(retentionBlockedRoles);
		boolean canBeFullyDeleted = !hasActiveTheses && !isResearchGroupHead && retentionBlockedCount == 0;

		String message;
		if (isResearchGroupHead) {
			message = "You must transfer research group leadership before deleting your account.";
		} else if (hasActiveTheses) {
			message = "You have active theses that must be completed or dropped before deletion.";
		} else if (canBeFullyDeleted) {
			message = "Your account and all associated data will be permanently deleted.";
		} else {
			message = "Your account will be deactivated and non-essential data deleted immediately. "
					+ "Your profile and thesis data (" + retentionBlockedCount
					+ " thesis/theses) must be retained until " + earliestDeletion
					+ " per legal requirements, then everything will be fully deleted.";
		}

		return new UserDeletionPreviewDto(
				canBeFullyDeleted,
				hasActiveTheses,
				retentionBlockedCount,
				earliestDeletion,
				isResearchGroupHead,
				message
		);
	}

	@Transactional
	public UserDeletionResultDto deleteOrAnonymizeUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (user.isAnonymized() || user.getDeletionRequestedAt() != null) {
			throw new AccessDeniedException("This account has already been deleted");
		}

		if (researchGroupRepository.existsByHeadId(userId)) {
			throw new AccessDeniedException("Cannot delete account while being a research group head. Transfer leadership first.");
		}

		if (hasActiveTheses(userId)) {
			throw new AccessDeniedException("Cannot delete account with active theses. Complete or drop out first.");
		}

		// Delete freely-deletable applications (rejected/not-assessed)
		deleteNonRetainedApplications(userId);

		// Delete data exports (files + records)
		deleteDataExports(user);

		List<ThesisRole> retentionBlockedRoles = getRetentionBlockedThesisRoles(userId);

		if (retentionBlockedRoles.isEmpty()) {
			// No retention — delete all user files and the account
			deleteAllUserFiles(user);
			return performFullDeletion(user);
		} else {
			// Retention active — only delete avatar (cosmetic), keep CV/degree/exam
			// as they are part of the thesis evaluation process.
			// TODO: Once application file snapshotting is implemented (i.e. copying cvFilename,
			//  degreeFilename, and examinationFilename onto the Application or Thesis at submission
			//  time), we can delete user.cvFilename, user.degreeFilename, and
			//  user.examinationFilename here as well, because the snapshots on the retained
			//  thesis/application records would still be available for evaluation purposes.
			uploadService.deleteFile(user.getAvatar());
			return performSoftDeletion(user, retentionBlockedRoles);
		}
	}

	@Transactional
	public void processDeferredDeletions() {
		List<User> pendingUsers = userRepository.findAllByDeletionRequestedAtIsNotNull();

		for (User user : pendingUsers) {
			List<ThesisRole> retentionBlocked = getRetentionBlockedThesisRoles(user.getId());
			if (retentionBlocked.isEmpty()) {
				log.info("Retention expired for user {}, performing full deletion", user.getId());
				deleteAllUserFiles(user);
				deleteDataExports(user);
				performFullDeletion(user);
			}
		}
	}

	private UserDeletionResultDto performFullDeletion(User user) {
		UUID userId = user.getId();

		// Delete remaining applications (use entity-based deletion to keep Hibernate session consistent)
		List<Application> remainingApps = applicationRepository.findAllByUserId(userId);
		applicationRepository.deleteAll(remainingApps);

		// Delete topic roles
		topicRoleRepository.deleteAllByIdUserId(userId);

		// Delete thesis roles (should be empty if no retention-blocked data)
		thesisRoleRepository.deleteAllByIdUserId(userId);

		// Explicitly delete user-owned entities to avoid Hibernate session conflicts
		// (UserGroup is EAGER-fetched and causes TransientPropertyValueException otherwise)
		notificationSettingRepository.deleteAll(user.getNotificationSettings());
		userGroupRepository.deleteByUserId(userId);
		user.getGroups().clear();
		user.getNotificationSettings().clear();

		userRepository.delete(user);

		log.info("Fully deleted user account {}", userId);
		return new UserDeletionResultDto("DELETED", "Your account and all associated data have been permanently deleted.");
	}

	private UserDeletionResultDto performSoftDeletion(User user, List<ThesisRole> retentionBlockedRoles) {
		Instant now = Instant.now();
		Instant earliestDeletion = computeEarliestFullDeletion(retentionBlockedRoles);

		// Deactivate the account but keep profile data intact so thesis records
		// remain searchable by name during the legal retention period.
		user.setDisabled(true);
		user.setDeletionRequestedAt(now);
		user.setDeletionScheduledFor(earliestDeletion);

		// Delete non-essential data that is not needed for thesis retention.
		// Keep CV, degree report, and examination report as they are part of
		// the thesis evaluation process and may still need to be referenced.
		user.setAvatar(null);
		user.setProjects(null);
		user.setInterests(null);
		user.setSpecialSkills(null);
		user.setCustomData(new HashMap<>());

		userRepository.save(user);

		// Delete notification settings and user groups (not needed during retention)
		notificationSettingRepository.deleteAll(user.getNotificationSettings());
		userGroupRepository.deleteByUserId(user.getId());

		log.info("Soft-deleted user account {}, full deletion scheduled for {}", user.getId(), earliestDeletion);
		return new UserDeletionResultDto("DEACTIVATED",
				"Your account has been deactivated and non-essential data deleted. "
						+ "Your profile and thesis data will be fully deleted after the legal retention period expires ("
						+ earliestDeletion + ").");
	}

	private void deleteAllUserFiles(User user) {
		uploadService.deleteFile(user.getCvFilename());
		uploadService.deleteFile(user.getDegreeFilename());
		uploadService.deleteFile(user.getExaminationFilename());
		uploadService.deleteFile(user.getAvatar());
	}

	private boolean hasActiveTheses(UUID userId) {
		return thesisRoleRepository.findAllByIdUserIdWithThesis(userId).stream()
				.anyMatch(role -> !TERMINAL_STATES.contains(role.getThesis().getState()));
	}

	private List<ThesisRole> getRetentionBlockedThesisRoles(UUID userId) {
		Instant now = Instant.now();
		return thesisRoleRepository.findAllByIdUserIdWithThesis(userId).stream()
				.filter(role -> TERMINAL_STATES.contains(role.getThesis().getState()))
				.filter(role -> computeRetentionExpiry(role).isAfter(now))
				.toList();
	}

	private Instant computeRetentionExpiry(ThesisRole role) {
		// Retention: 5 years after end of calendar year of thesis completion
		Instant createdAt = role.getThesis().getCreatedAt();
		ZonedDateTime zdt = createdAt.atZone(ZoneId.of("Europe/Berlin"));
		// End of the calendar year + 5 years
		return ZonedDateTime.of(zdt.getYear() + RETENTION_YEARS, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Berlin"))
				.toInstant();
	}

	private Instant computeEarliestFullDeletion(List<ThesisRole> retentionBlockedRoles) {
		return retentionBlockedRoles.stream()
				.map(this::computeRetentionExpiry)
				.max(Instant::compareTo)
				.orElse(null);
	}

	private void deleteNonRetainedApplications(UUID userId) {
		List<Application> applications = applicationRepository.findAllByUserId(userId);
		for (Application app : applications) {
			if (app.getState() == ApplicationState.REJECTED || app.getState() == ApplicationState.NOT_ASSESSED) {
				applicationRepository.delete(app);
			}
		}
	}

	private void deleteDataExports(User user) {
		List<DataExport> exports = dataExportRepository.findAllByUserOrderByCreatedAtDesc(user);
		for (DataExport export : exports) {
			if (export.getFilePath() != null) {
				try {
					java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(export.getFilePath()));
				} catch (java.io.IOException e) {
					log.warn("Failed to delete export file {}: {}", export.getFilePath(), e.getMessage());
				}
			}
			dataExportRepository.delete(export);
		}
	}
}
