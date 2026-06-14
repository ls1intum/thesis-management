package de.tum.cit.aet.thesis.thesis.controller.payload;

import de.tum.cit.aet.thesis.thesis.constants.ThesisVisibility;

public record AddThesisGradePayload(
		String finalGrade,
		String finalFeedback,
		ThesisVisibility visibility
) {
}
