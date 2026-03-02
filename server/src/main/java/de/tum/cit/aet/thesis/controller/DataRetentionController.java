package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.DataRetentionResultDto;
import de.tum.cit.aet.thesis.dto.ThesisAnonymizationResultDto;
import de.tum.cit.aet.thesis.service.DataRetentionService;
import de.tum.cit.aet.thesis.service.ThesisAnonymizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing data retention policy operations.
 */
@RestController
@RequestMapping("/v2/data-retention")
public class DataRetentionController {

	private final DataRetentionService dataRetentionService;
	private final ThesisAnonymizationService thesisAnonymizationService;

	/**
	 * Constructs a new DataRetentionController with the required dependencies.
	 *
	 * @param dataRetentionService the data retention service
	 * @param thesisAnonymizationService the thesis anonymization service
	 */
	@Autowired
	public DataRetentionController(DataRetentionService dataRetentionService,
			ThesisAnonymizationService thesisAnonymizationService) {
		this.dataRetentionService = dataRetentionService;
		this.thesisAnonymizationService = thesisAnonymizationService;
	}

	/**
	 * Triggers cleanup of expired rejected applications based on the retention policy.
	 *
	 * @return the cleanup result with deletion count
	 */
	@PostMapping("/cleanup-rejected-applications")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<DataRetentionResultDto> triggerCleanup() {
		int deleted = dataRetentionService.deleteExpiredRejectedApplications();
		return ResponseEntity.ok(new DataRetentionResultDto(deleted));
	}

	/**
	 * Triggers anonymization of theses that have exceeded the 5-year retention period.
	 *
	 * @return the result with anonymized thesis count
	 */
	@PostMapping("/anonymize-expired-theses")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<ThesisAnonymizationResultDto> triggerThesisAnonymization() {
		int anonymized = thesisAnonymizationService.anonymizeExpiredTheses();
		return ResponseEntity.ok(new ThesisAnonymizationResultDto(anonymized));
	}
}
