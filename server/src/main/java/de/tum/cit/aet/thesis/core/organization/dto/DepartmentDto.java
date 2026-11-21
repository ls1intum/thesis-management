package de.tum.cit.aet.thesis.core.organization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.organization.entity.Department;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DepartmentDto(UUID id, UUID schoolId, String name, String abbreviation, boolean active) {
	public static DepartmentDto fromDepartmentEntity(Department department) {
		if (department == null) {
			return null;
		}

		return new DepartmentDto(
				department.getId(),
				department.getSchool().getId(),
				department.getName(),
				department.getAbbreviation(),
				department.isActive()
		);
	}
}
