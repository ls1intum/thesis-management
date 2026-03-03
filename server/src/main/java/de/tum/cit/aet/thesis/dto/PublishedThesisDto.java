package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.entity.Thesis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublishedThesisDto(
		UUID thesisId,
		ThesisState state,
		String title,
		String type,
		Instant startDate,
		Instant endDate,
		String abstractText,
		List<MinimalUserDto> students,
		List<MinimalUserDto> supervisors,
		List<MinimalUserDto> examiners,
		LightResearchGroupDto researchGroup
) {
	public static PublishedThesisDto fromThesisEntity(Thesis thesis) {
		if (thesis == null) {
			return null;
		}

		return new PublishedThesisDto(
				thesis.getId(),
				thesis.getState(),
				thesis.getTitle(),
				thesis.getType(),
				thesis.getStartDate(),
				thesis.getEndDate(),
				thesis.getAbstractField(),
				thesis.getStudents().stream().map(MinimalUserDto::fromUserEntity).toList(),
				thesis.getSupervisors().stream().map(MinimalUserDto::fromUserEntity).toList(),
				thesis.getExaminers().stream().map(MinimalUserDto::fromUserEntity).toList(),
				LightResearchGroupDto.fromResearchGroupEntity(thesis.getResearchGroup())
		);
	}
}
