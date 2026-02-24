package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.DataExportDto;
import de.tum.cit.aet.thesis.entity.DataExport;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.service.DataExportService;
import de.tum.cit.aet.thesis.service.DataExportService.RequestStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v2/data-exports")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class DataExportController {
	private final DataExportService dataExportService;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	@Autowired
	public DataExportController(DataExportService dataExportService,
			ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
		this.dataExportService = dataExportService;
		this.currentUserProviderProvider = currentUserProviderProvider;
	}

	private User currentUser() {
		return currentUserProviderProvider.getObject().getUser();
	}

	@PostMapping
	public ResponseEntity<DataExportDto> requestExport() {
		User user = currentUser();
		RequestStatus status = dataExportService.canRequestDataExport(user);

		if (!status.canRequest()) {
			DataExport latest = dataExportService.getLatestExport(user);
			DataExportDto dto = latest != null
					? DataExportDto.fromEntity(latest, false, status.nextRequestDate())
					: DataExportDto.noExport(false, status.nextRequestDate());
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(dto);
		}

		DataExport export = dataExportService.requestDataExport(user);
		return ResponseEntity.ok(DataExportDto.fromEntity(export, false, null));
	}

	@GetMapping("/status")
	public ResponseEntity<DataExportDto> getStatus() {
		User user = currentUser();
		DataExport latest = dataExportService.getLatestExport(user);
		RequestStatus status = dataExportService.canRequestDataExport(user);

		if (latest == null) {
			return ResponseEntity.ok(DataExportDto.noExport(status.canRequest(), status.nextRequestDate()));
		}

		return ResponseEntity.ok(DataExportDto.fromEntity(latest, status.canRequest(), status.nextRequestDate()));
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<Resource> downloadExport(@PathVariable UUID id) {
		DataExport export = dataExportService.findById(id);
		Resource resource = dataExportService.downloadDataExport(export, currentUser());

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data_export.zip")
				.body(resource);
	}
}
