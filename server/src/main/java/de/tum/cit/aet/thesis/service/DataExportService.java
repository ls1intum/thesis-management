package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.DataExportState;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.ApplicationReviewer;
import de.tum.cit.aet.thesis.entity.DataExport;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisAssessment;
import de.tum.cit.aet.thesis.entity.ThesisFeedback;
import de.tum.cit.aet.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.DataExportRepository;
import de.tum.cit.aet.thesis.repository.ThesisAssessmentRepository;
import de.tum.cit.aet.thesis.repository.ThesisFeedbackRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Manages the lifecycle of GDPR data exports including creation, download, and expiration. */
@Service
public class DataExportService {
	private static final Logger log = LoggerFactory.getLogger(DataExportService.class);

	private final DataExportRepository dataExportRepository;
	private final ApplicationRepository applicationRepository;
	private final ThesisRepository thesisRepository;
	private final ThesisFeedbackRepository thesisFeedbackRepository;
	private final ThesisAssessmentRepository thesisAssessmentRepository;
	private final UploadService uploadService;
	private final MailingService mailingService;
	private final Path exportPath;
	private final int retentionDays;
	private final int cooldownDays;

	private final ObjectMapper objectMapper;

	/**
	 * Constructs the service with required repositories, upload and mailing services, and configuration values.
	 *
	 * @param dataExportRepository the data export repository
	 * @param applicationRepository the application repository
	 * @param thesisRepository the thesis repository
	 * @param thesisFeedbackRepository the thesis feedback repository
	 * @param thesisAssessmentRepository the thesis assessment repository
	 * @param uploadService the upload service
	 * @param mailingService the mailing service
	 * @param springObjectMapper the Spring-managed ObjectMapper with modules pre-registered
	 * @param exportPath the export directory path
	 * @param retentionDays the export retention period in days
	 * @param cooldownDays the cooldown period between exports
	 */
	public DataExportService(
			DataExportRepository dataExportRepository,
			ApplicationRepository applicationRepository,
			ThesisRepository thesisRepository,
			ThesisFeedbackRepository thesisFeedbackRepository,
			ThesisAssessmentRepository thesisAssessmentRepository,
			UploadService uploadService,
			MailingService mailingService,
			ObjectMapper springObjectMapper,
			@Value("${thesis-management.data-export.path}") String exportPath,
			@Value("${thesis-management.data-export.retention-days}") int retentionDays,
			@Value("${thesis-management.data-export.days-between-exports}") int cooldownDays) {
		this.dataExportRepository = dataExportRepository;
		this.applicationRepository = applicationRepository;
		this.thesisRepository = thesisRepository;
		this.thesisFeedbackRepository = thesisFeedbackRepository;
		this.thesisAssessmentRepository = thesisAssessmentRepository;
		this.uploadService = uploadService;
		this.mailingService = mailingService;
		this.exportPath = Path.of(exportPath);
		this.retentionDays = retentionDays;
		this.cooldownDays = cooldownDays;

		// Use Spring's ObjectMapper (Jackson 3.x with built-in Java 8 date/time support)
		// with export-specific settings applied via rebuild() to avoid mutating the shared instance.
		this.objectMapper = springObjectMapper.rebuild()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.build();

		File dir = this.exportPath.toFile();
		if (!dir.exists() && !dir.mkdirs()) {
			log.warn("Failed to create data export directory: {}", exportPath);
		}
	}

	@Transactional
	public DataExport requestDataExport(User user) {
		RequestStatus status = canRequestDataExport(user);
		if (!status.canRequest()) {
			throw new IllegalStateException("Data export request not allowed. Next request allowed at: " + status.nextRequestDate());
		}

		DataExport export = new DataExport();
		export.setUser(user);
		export.setState(DataExportState.REQUESTED);
		return dataExportRepository.save(export);
	}

	/** Represents whether a user can request a data export and when the next request is allowed. */
	public record RequestStatus(boolean canRequest, Instant nextRequestDate) {}

	/**
	 * Checks whether the user is allowed to request a new data export based on cooldown rules.
	 *
	 * @param user the user to check
	 * @return the request status with eligibility info
	 */
	public RequestStatus canRequestDataExport(User user) {
		List<DataExport> exports = dataExportRepository.findAllByUserOrderByCreatedAtDesc(user);

		if (exports.isEmpty()) {
			return new RequestStatus(true, null);
		}

		DataExport latest = exports.getFirst();

		// Allow re-request if latest export failed or was deleted without being downloaded
		if (latest.getState() == DataExportState.FAILED ||
				latest.getState() == DataExportState.DELETED) {
			return new RequestStatus(true, null);
		}

		// Check cooldown
		Instant nextAllowed = latest.getCreatedAt().plus(cooldownDays, ChronoUnit.DAYS);
		if (Instant.now().isBefore(nextAllowed)) {
			return new RequestStatus(false, nextAllowed);
		}

		return new RequestStatus(true, null);
	}

	/**
	 * Returns the most recent data export for the given user, or null if none exists.
	 *
	 * @param user the user to query
	 * @return the latest export or null
	 */
	public DataExport getLatestExport(User user) {
		List<DataExport> exports = dataExportRepository.findAllByUserOrderByCreatedAtDesc(user);
		if (exports.isEmpty()) {
			return null;
		}
		return exports.getFirst();
	}

	/**
	 * Finds a data export by its unique identifier or throws if not found.
	 *
	 * @param id the export identifier
	 * @return the data export entity
	 */
	public DataExport findById(UUID id) {
		return dataExportRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Data export not found"));
	}

	@Transactional
	public Resource downloadDataExport(DataExport export, User user) {
		if (!export.getUser().getId().equals(user.getId()) && !user.hasAnyGroup("admin")) {
			throw new org.springframework.security.access.AccessDeniedException("You are not allowed to download this export");
		}

		Set<DataExportState> downloadableStates = Set.of(
				DataExportState.EMAIL_SENT, DataExportState.EMAIL_FAILED, DataExportState.DOWNLOADED);
		if (!downloadableStates.contains(export.getState())) {
			throw new IllegalStateException("Export is not available for download");
		}

		if (export.getFilePath() == null) {
			throw new ResourceNotFoundException("Export file not found");
		}

		Path resolvedPath = Path.of(export.getFilePath()).normalize();
		if (!resolvedPath.startsWith(exportPath.normalize())) {
			throw new ResourceNotFoundException("Export file not found");
		}

		FileSystemResource resource = new FileSystemResource(resolvedPath);
		if (!resource.exists()) {
			throw new ResourceNotFoundException("Export file not found on disk");
		}

		export.setState(DataExportState.DOWNLOADED);
		export.setDownloadedAt(Instant.now());
		dataExportRepository.save(export);

		return resource;
	}

	/** Processes all pending data export requests by generating ZIP files and sending notification emails. */
	public void processAllPendingExports() {
		List<DataExport> pending = dataExportRepository.findAllByStateIn(
				List.of(DataExportState.REQUESTED));

		for (DataExport export : pending) {
			// Atomically claim this export to prevent duplicate processing
			// in multi-instance deployments.
			int updated = dataExportRepository.claimForProcessing(export.getId(), DataExportState.REQUESTED);
			if (updated == 0) {
				continue; // Another instance already claimed it
			}

			// Re-fetch with eagerly loaded user because claimForProcessing() used a JPQL
			// UPDATE that bypassed the persistence context, and DataExport.user is lazy.
			DataExport claimed = dataExportRepository.findByIdWithUser(export.getId());
			if (claimed == null) {
				continue;
			}

			try {
				createDataExport(claimed);
			} catch (Exception e) {
				log.error("Failed to create data export {}: {}", claimed.getId(), e.getMessage(), e);
				claimed.setState(DataExportState.FAILED);
				claimed.setCreationFinishedAt(Instant.now());
				dataExportRepository.save(claimed);
			}
		}
	}

	private void createDataExport(DataExport export) throws IOException {
		export.setState(DataExportState.IN_CREATION);

		User user = export.getUser();
		String filename = String.format("export_%s_%d.zip", user.getId(), System.currentTimeMillis());
		Path zipPath = exportPath.resolve(filename);

		try {
			writeZipFile(zipPath, user);
		} catch (IOException e) {
			// Clean up partial ZIP file on failure
			Files.deleteIfExists(zipPath);
			throw e;
		}

		export.setFilePath(zipPath.toString());
		export.setCreationFinishedAt(Instant.now());

		try {
			mailingService.sendDataExportReadyEmail(user, export);
			export.setState(DataExportState.EMAIL_SENT);
		} catch (Exception e) {
			log.warn("Failed to send data export email for export {}: {}", export.getId(), e.getMessage());
			export.setState(DataExportState.EMAIL_FAILED);
		}

		dataExportRepository.save(export);
	}

	private void writeZipFile(Path zipPath, User user) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
			// user.json
			zos.putNextEntry(new ZipEntry("user.json"));
			zos.write(objectMapper.writeValueAsBytes(buildUserData(user)));
			zos.closeEntry();

			// applications.json
			zos.putNextEntry(new ZipEntry("applications.json"));
			zos.write(objectMapper.writeValueAsBytes(buildApplicationsData(user)));
			zos.closeEntry();

			// theses.json
			zos.putNextEntry(new ZipEntry("theses.json"));
			zos.write(objectMapper.writeValueAsBytes(buildThesesData(user)));
			zos.closeEntry();

			// README.txt
			zos.putNextEntry(new ZipEntry("README.txt"));
			zos.write(buildReadme().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			zos.closeEntry();

			// User-uploaded files
			addUserFile(zos, user.getCvFilename(), "files/cv");
			addUserFile(zos, user.getDegreeFilename(), "files/degree_report");
			addUserFile(zos, user.getExaminationFilename(), "files/examination_report");
		}
	}

	/** Deletes data export files that have exceeded the configured retention period. */
	public void deleteExpiredExports() {
		Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
		List<DataExport> expired = dataExportRepository.findExpiredExports(
				cutoff,
				List.of(DataExportState.EMAIL_SENT, DataExportState.EMAIL_FAILED, DataExportState.DOWNLOADED));

		for (DataExport export : expired) {
			try {
				if (export.getFilePath() != null) {
					Path resolvedPath = Path.of(export.getFilePath()).normalize();
					if (resolvedPath.startsWith(exportPath.normalize())) {
						Files.deleteIfExists(resolvedPath);
					} else {
						log.warn("Skipping export file deletion outside expected directory: {}", export.getFilePath());
					}
				}

				DataExportState newState = export.getState() == DataExportState.DOWNLOADED
						? DataExportState.DOWNLOADED_DELETED
						: DataExportState.DELETED;
				export.setState(newState);
				export.setFilePath(null);
				dataExportRepository.save(export);
			} catch (Exception e) {
				log.error("Failed to delete expired export {}: {}", export.getId(), e.getMessage());
			}
		}
	}

	private Map<String, Object> buildUserData(User user) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("firstName", user.getFirstName());
		data.put("lastName", user.getLastName());
		data.put("email", user.getEmail() != null ? user.getEmail().toString() : null);
		data.put("universityId", user.getUniversityId());
		data.put("matriculationNumber", user.getMatriculationNumber());
		data.put("gender", user.getGender());
		data.put("nationality", user.getNationality());
		data.put("studyDegree", user.getStudyDegree());
		data.put("studyProgram", user.getStudyProgram());
		data.put("interests", user.getInterests());
		data.put("specialSkills", user.getSpecialSkills());
		data.put("projects", user.getProjects());
		data.put("customData", user.getCustomData());
		data.put("joinedAt", user.getJoinedAt());
		data.put("enrolledAt", user.getEnrolledAt());
		return data;
	}

	private List<Map<String, Object>> buildApplicationsData(User user) {
		// Use eager query to fetch reviewers and their users in one go,
		// avoiding LazyInitializationException on ApplicationReviewer.user.
		List<Application> applications = applicationRepository.findAllByUserIdWithReviewers(user.getId());
		List<Map<String, Object>> result = new ArrayList<>();

		for (Application app : applications) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("id", app.getId());
			data.put("thesisTitle", app.getThesisTitle());
			data.put("thesisType", app.getThesisType());
			data.put("state", app.getState());
			data.put("motivation", app.getMotivation());
			data.put("desiredStartDate", app.getDesiredStartDate());
			data.put("createdAt", app.getCreatedAt());
			data.put("reviewedAt", app.getReviewedAt());
			data.put("rejectReason", app.getRejectReason());

			// Structured reviewer data (no free-text comments)
			List<Map<String, Object>> reviewers = new ArrayList<>();
			for (ApplicationReviewer reviewer : app.getReviewers()) {
				Map<String, Object> reviewerData = new LinkedHashMap<>();
				reviewerData.put("reviewerName", reviewer.getUser().getFirstName() + " " + reviewer.getUser().getLastName());
				reviewerData.put("decision", reviewer.getReason());
				reviewerData.put("reviewedAt", reviewer.getReviewedAt());
				reviewers.add(reviewerData);
			}
			data.put("reviewers", reviewers);

			result.add(data);
		}

		return result;
	}

	private List<Map<String, Object>> buildThesesData(User user) {
		List<Thesis> theses = thesisRepository.findAllByStudentUserId(user.getId());
		if (theses.isEmpty()) {
			return List.of();
		}

		// Eagerly fetch lazy collections in separate queries to avoid
		// LazyInitializationException (no @Transactional per project convention).
		List<UUID> thesisIds = theses.stream().map(Thesis::getId).toList();
		Map<UUID, List<ThesisFeedback>> feedbackByThesis = thesisFeedbackRepository
				.findAllByThesisIdInOrderByRequestedAtAsc(thesisIds).stream()
				.collect(java.util.stream.Collectors.groupingBy(fb -> fb.getThesis().getId()));
		Map<UUID, List<ThesisAssessment>> assessmentsByThesis = thesisAssessmentRepository
				.findAllByThesisIdInOrderByCreatedAtDesc(thesisIds).stream()
				.collect(java.util.stream.Collectors.groupingBy(a -> a.getThesis().getId()));

		List<Map<String, Object>> result = new ArrayList<>();

		for (Thesis thesis : theses) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("id", thesis.getId());
			data.put("title", thesis.getTitle());
			data.put("type", thesis.getType());
			data.put("state", thesis.getState());
			data.put("language", thesis.getLanguage());
			data.put("keywords", thesis.getKeywords());
			data.put("startDate", thesis.getStartDate());
			data.put("endDate", thesis.getEndDate());
			data.put("grade", thesis.getFinalGrade());

			// Feedback items (from eagerly fetched data)
			List<Map<String, Object>> feedbackItems = new ArrayList<>();
			for (ThesisFeedback fb : feedbackByThesis.getOrDefault(thesis.getId(), List.of())) {
				Map<String, Object> fbData = new LinkedHashMap<>();
				fbData.put("type", fb.getType());
				fbData.put("feedback", fb.getFeedback());
				fbData.put("requestedAt", fb.getRequestedAt());
				fbData.put("completedAt", fb.getCompletedAt());
				feedbackItems.add(fbData);
			}
			data.put("feedback", feedbackItems);

			// Assessment summaries (from eagerly fetched data, no free-text management comments)
			List<Map<String, Object>> assessments = new ArrayList<>();
			for (ThesisAssessment assessment : assessmentsByThesis.getOrDefault(thesis.getId(), List.of())) {
				Map<String, Object> assessmentData = new LinkedHashMap<>();
				assessmentData.put("summary", assessment.getSummary());
				assessmentData.put("positives", assessment.getPositives());
				assessmentData.put("negatives", assessment.getNegatives());
				assessmentData.put("gradeSuggestion", assessment.getGradeSuggestion());
				assessmentData.put("createdAt", assessment.getCreatedAt());
				assessments.add(assessmentData);
			}
			data.put("assessments", assessments);

			// State changes (eagerly fetched via Thesis.states with FetchType.EAGER)
			List<Map<String, Object>> stateChanges = new ArrayList<>();
			for (ThesisStateChange sc : thesis.getStates()) {
				Map<String, Object> scData = new LinkedHashMap<>();
				scData.put("state", sc.getId().getState());
				scData.put("changedAt", sc.getChangedAt());
				stateChanges.add(scData);
			}
			data.put("stateChanges", stateChanges);

			result.add(data);
		}

		return result;
	}

	private void addUserFile(ZipOutputStream zos, String filename, String entryPrefix) {
		if (filename == null || filename.isBlank()) {
			return;
		}
		try {
			FileSystemResource resource = uploadService.load(filename);
			if (resource.exists()) {
				String extension = "";
				int dotIndex = filename.lastIndexOf('.');
				if (dotIndex >= 0) {
					extension = filename.substring(dotIndex);
				}
				zos.putNextEntry(new ZipEntry(entryPrefix + extension));
				try (java.io.InputStream is = resource.getInputStream()) {
					is.transferTo(zos);
				}
				zos.closeEntry();
			}
		} catch (Exception e) {
			log.warn("Failed to include file {} in export: {}", filename, e.getMessage());
		}
	}

	private String buildReadme() {
		return """
				DATA EXPORT
				===========

				This archive contains your personal data as stored in the Thesis Management system.

				Contents:
				- user.json: Your profile information (name, email, university ID, study program, etc.)
				- applications.json: All your thesis applications including review decisions
				- theses.json: All theses where you are a student, including assessments and state changes
				- files/: Your uploaded documents (CV, degree report, examination report)

				Notes:
				- Free-text management comments are excluded as they may contain third-party personal data
				- Structured reviewer decisions (interested/not interested) are included
				- Timestamps are in ISO 8601 format (UTC)

				This export was generated in compliance with GDPR Article 15 (Right of Access)
				and Article 20 (Right to Data Portability).
				""";
	}
}
