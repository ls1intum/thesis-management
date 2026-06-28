package de.tum.cit.aet.thesis.feedback.dto;

import java.util.List;

public record ReviewResultDTO(AssessmentCategory category, String summary, List<FindingDTO> findings) {
}
