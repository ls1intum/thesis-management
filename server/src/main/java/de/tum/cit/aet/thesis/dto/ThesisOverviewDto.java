package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.constants.ThesisPresentationType;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ThesisOverviewDto(
	UUID thesisId,
	String title,
	String type,
	ThesisState state,
	Instant startDate,
	Instant endDate,
	Instant createdAt,
	Set<String> keywords,
	List<MinimalUserDto> students,
	List<MinimalUserDto> advisors,
	List<MinimalUserDto> supervisors,
	MinimalResearchGroupDto researchGroup,
	List<ThesisDto.ThesisStateChangeDto> states,
	List<ThesisPresentationOverviewDto> presentations
) {

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public record ThesisPresentationOverviewDto(
		UUID presentationId,
		ThesisPresentationType type,
		Instant scheduledAt
	) {
		public static ThesisPresentationOverviewDto fromPresentationEntity(ThesisPresentation presentation) {
			if (presentation == null) {
				return null;
			}

			return new ThesisPresentationOverviewDto(
				presentation.getId(),
				presentation.getType(),
				presentation.getScheduledAt()
			);
		}
	}

	public static ThesisOverviewDto fromThesisEntity(Thesis thesis) {
		if (thesis == null) {
			return null;
		}

		List<MinimalUserDto> students = thesis.getStudents().stream()
			.map(MinimalUserDto::fromUserEntity).toList();
		List<MinimalUserDto> advisors = thesis.getAdvisors().stream()
			.map(MinimalUserDto::fromUserEntity).toList();
		List<MinimalUserDto> supervisors = thesis.getSupervisors().stream()
			.map(MinimalUserDto::fromUserEntity).toList();

		List<ThesisDto.ThesisStateChangeDto> states = ThesisDto.computeStateChanges(thesis);

		List<ThesisPresentationOverviewDto> presentations = thesis.getPresentations().stream()
			.map(ThesisPresentationOverviewDto::fromPresentationEntity).toList();

		return new ThesisOverviewDto(
			thesis.getId(),
			thesis.getTitle(),
			thesis.getType(),
			thesis.getState(),
			thesis.getStartDate(),
			thesis.getEndDate(),
			thesis.getCreatedAt(),
			thesis.getKeywords(),
			students,
			advisors,
			supervisors,
			MinimalResearchGroupDto.fromResearchGroupEntity(thesis.getResearchGroup()),
			states,
			presentations
		);
	}
}
