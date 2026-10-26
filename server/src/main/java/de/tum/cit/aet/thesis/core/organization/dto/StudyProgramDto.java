package de.tum.cit.aet.thesis.core.organization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.organization.entity.StudyProgram;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record StudyProgramDto(UUID id, String key, String name, boolean active, MinimalSchoolDto school) {
	public static StudyProgramDto fromStudyProgramEntity(StudyProgram studyProgram) {
		if (studyProgram == null) {
			return null;
		}

		return new StudyProgramDto(
				studyProgram.getId(),
				studyProgram.getKey(),
				studyProgram.getName(),
				studyProgram.isActive(),
				MinimalSchoolDto.fromSchoolEntity(studyProgram.getSchool())
		);
	}
}
