package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.constants.DataExportState;
import de.tum.cit.aet.thesis.entity.DataExport;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DataExportDto(
		UUID id,
		DataExportState state,
		Instant createdAt,
		Instant creationFinishedAt,
		Instant downloadedAt,
		boolean canRequest,
		Instant nextRequestDate
) {
	public static DataExportDto fromEntity(DataExport entity, boolean canRequest, Instant nextRequestDate) {
		return new DataExportDto(
				entity.getId(),
				entity.getState(),
				entity.getCreatedAt(),
				entity.getCreationFinishedAt(),
				entity.getDownloadedAt(),
				canRequest,
				nextRequestDate
		);
	}

	public static DataExportDto noExport(boolean canRequest, Instant nextRequestDate) {
		return new DataExportDto(null, null, null, null, null, canRequest, nextRequestDate);
	}
}
