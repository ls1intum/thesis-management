package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.DataRetentionResultDto;
import de.tum.cit.aet.thesis.service.DataRetentionService;
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

	/**
	 * Constructs a new DataRetentionController with the required dependencies.
	 *
	 * @param dataRetentionService the data retention service
	 */
	@Autowired
	public DataRetentionController(DataRetentionService dataRetentionService) {
		this.dataRetentionService = dataRetentionService;
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
}
