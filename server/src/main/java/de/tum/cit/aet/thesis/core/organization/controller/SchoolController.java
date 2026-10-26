package de.tum.cit.aet.thesis.core.organization.controller;

import de.tum.cit.aet.thesis.core.organization.controller.payload.SchoolPayload;
import de.tum.cit.aet.thesis.core.organization.dto.SchoolDto;
import de.tum.cit.aet.thesis.core.organization.entity.Department;
import de.tum.cit.aet.thesis.core.organization.entity.School;
import de.tum.cit.aet.thesis.core.organization.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** REST controller for schools (faculties). Reading is open to signed-in users, changes are admin only. */
@RestController
@RequestMapping("/v2/schools")
public class SchoolController {
	private final OrganizationService organizationService;

	/**
	 * Creates the controller.
	 *
	 * @param organizationService the organization service
	 */
	@Autowired
	public SchoolController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	/**
	 * Lists all schools including their departments.
	 *
	 * @return the schools
	 */
	@GetMapping
	public ResponseEntity<List<SchoolDto>> getSchools() {
		Map<UUID, List<Department>> departmentsBySchool = organizationService.getDepartments().stream()
				.collect(Collectors.groupingBy(department -> department.getSchool().getId()));

		return ResponseEntity.ok(organizationService.getSchools().stream()
				.map(school -> SchoolDto.fromSchoolEntity(school, departmentsBySchool.getOrDefault(school.getId(), List.of())))
				.toList());
	}

	/**
	 * Creates a school.
	 *
	 * @param payload the school data
	 * @return the created school
	 */
	@PostMapping
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<SchoolDto> createSchool(@RequestBody SchoolPayload payload) {
		School school = organizationService.createSchool(
				payload.name(), payload.abbreviation(), payload.websiteUrl(), payload.thesisPortalUrl(), payload.active());

		return ResponseEntity.ok(SchoolDto.fromSchoolEntity(school, List.of()));
	}

	/**
	 * Updates a school.
	 *
	 * @param schoolId the school id
	 * @param payload the school data
	 * @return the updated school
	 */
	@PutMapping("/{schoolId}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<SchoolDto> updateSchool(@PathVariable UUID schoolId, @RequestBody SchoolPayload payload) {
		School school = organizationService.updateSchool(
				organizationService.getSchool(schoolId),
				payload.name(), payload.abbreviation(), payload.websiteUrl(), payload.thesisPortalUrl(), payload.active());

		return ResponseEntity.ok(SchoolDto.fromSchoolEntity(school,
				organizationService.getDepartments().stream().filter(d -> d.getSchool().getId().equals(school.getId())).toList()));
	}

	/**
	 * Deletes a school that is not used anywhere.
	 *
	 * @param schoolId the school id
	 * @return an empty response
	 */
	@DeleteMapping("/{schoolId}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<Void> deleteSchool(@PathVariable UUID schoolId) {
		organizationService.deleteSchool(organizationService.getSchool(schoolId));

		return ResponseEntity.noContent().build();
	}
}
