package de.tum.cit.aet.thesis.thesis.controller.payload;

import java.util.List;

public record CreateAssessmentPayload(
		String summary,
		String positives,
		String negatives,
		String gradeSuggestion,
		List<GradeComponentPayload> gradeComponents
) {
}
