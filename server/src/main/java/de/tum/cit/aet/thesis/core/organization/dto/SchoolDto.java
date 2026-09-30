package de.tum.cit.aet.thesis.core.organization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.organization.entity.Department;
import de.tum.cit.aet.thesis.core.organization.entity.School;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record SchoolDto(
		UUID id,
		String name,
		String abbreviation,
		String websiteUrl,
		String thesisPortalUrl,
		boolean active,
		List<DepartmentDto> departments
) {
	public static SchoolDto fromSchoolEntity(School school, List<Department> departments) {
		if (school == null) {
			return null;
		}

		return new SchoolDto(
				school.getId(),
				school.getName(),
				school.getAbbreviation(),
				school.getWebsiteUrl(),
				school.getThesisPortalUrl(),
				school.isActive(),
				departments.stream()
						.sorted(Comparator.comparing(Department::getName, String.CASE_INSENSITIVE_ORDER))
						.map(DepartmentDto::fromDepartmentEntity)
						.toList()
		);
	}
}
