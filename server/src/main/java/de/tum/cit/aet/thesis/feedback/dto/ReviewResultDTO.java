package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ReviewResultDTO(AssessmentCategory category, Integer score, String summary, List<FindingDTO> findings) {
}
