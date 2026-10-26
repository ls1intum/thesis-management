package de.tum.cit.aet.thesis.core.organization.controller;

import de.tum.cit.aet.thesis.core.organization.controller.payload.DepartmentPayload;
import de.tum.cit.aet.thesis.core.organization.dto.DepartmentDto;
import de.tum.cit.aet.thesis.core.organization.entity.Department;
import de.tum.cit.aet.thesis.core.organization.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** REST controller for departments. Departments are read together with their school, changes are admin only. */
@RestController
@RequestMapping("/v2/departments")
public class DepartmentController {
	private final OrganizationService organizationService;

	/**
	 * Creates the controller.
	 *
	 * @param organizationService the organization service
	 */
	@Autowired
	public DepartmentController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	/**
	 * Creates a department.
	 *
	 * @param payload the department data
	 * @return the created department
	 */
	@PostMapping
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentPayload payload) {
		Department department = organizationService.createDepartment(
				payload.schoolId(), payload.name(), payload.abbreviation(), payload.active());

		return ResponseEntity.ok(DepartmentDto.fromDepartmentEntity(department));
	}

	/**
	 * Updates a department.
	 *
	 * @param departmentId the department id
	 * @param payload the department data
	 * @return the updated department
	 */
	@PutMapping("/{departmentId}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable UUID departmentId, @RequestBody DepartmentPayload payload) {
		Department department = organizationService.updateDepartment(
				organizationService.getDepartment(departmentId),
				payload.schoolId(), payload.name(), payload.abbreviation(), payload.active());

		return ResponseEntity.ok(DepartmentDto.fromDepartmentEntity(department));
	}

	/**
	 * Deletes a department that is not used by a research group.
	 *
	 * @param departmentId the department id
	 * @return an empty response
	 */
	@DeleteMapping("/{departmentId}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<Void> deleteDepartment(@PathVariable UUID departmentId) {
		organizationService.deleteDepartment(organizationService.getDepartment(departmentId));

		return ResponseEntity.noContent().build();
	}
}
