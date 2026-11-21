package de.tum.cit.aet.thesis.presentation.dto;

import de.tum.cit.aet.thesis.presentation.constants.ThesisPresentationState;
import de.tum.cit.aet.thesis.presentation.constants.ThesisPresentationType;
import de.tum.cit.aet.thesis.presentation.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.presentation.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.thesis.dto.PublishedThesisDto;

import java.time.Instant;
import java.util.UUID;

public record PublishedPresentationDto(
		UUID thesisId,
		UUID presentationId,
		ThesisPresentationState state,
		ThesisPresentationType type,
		ThesisPresentationVisibility visibility,
		String location,
		String streamUrl,
		String language,
		Instant scheduledAt,
		PublishedThesisDto thesis
) {
	public static PublishedPresentationDto fromPresentationEntity(ThesisPresentation presentation) {
		if (presentation == null) {
			return null;
		}

		return new PublishedPresentationDto(
				presentation.getThesis().getId(),
				presentation.getId(),
				presentation.getState(),
				presentation.getType(),
				presentation.getVisibility(),
				presentation.getLocation(),
				presentation.getStreamUrl(),
				presentation.getLanguage(),
				presentation.getScheduledAt(),
				PublishedThesisDto.fromThesisEntity(presentation.getThesis())
		);
	}
}
