package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.DataRetentionResultDto;
import de.tum.cit.aet.thesis.service.DataRetentionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/data-retention")
public class DataRetentionController {

	private final DataRetentionService dataRetentionService;

	@Autowired
	public DataRetentionController(DataRetentionService dataRetentionService) {
		this.dataRetentionService = dataRetentionService;
	}

	@PostMapping("/cleanup-rejected-applications")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<DataRetentionResultDto> triggerCleanup() {
		int deleted = dataRetentionService.deleteExpiredRejectedApplications();
		return ResponseEntity.ok(new DataRetentionResultDto(deleted));
	}
}
