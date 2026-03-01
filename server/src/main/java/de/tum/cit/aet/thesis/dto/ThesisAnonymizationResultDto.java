package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ThesisAnonymizationResultDto(int anonymizedTheses) {
}
