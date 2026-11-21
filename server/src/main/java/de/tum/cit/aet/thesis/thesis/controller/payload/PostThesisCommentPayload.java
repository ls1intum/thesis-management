package de.tum.cit.aet.thesis.thesis.controller.payload;

import de.tum.cit.aet.thesis.thesis.constants.ThesisCommentType;

public record PostThesisCommentPayload(
		String message,
		ThesisCommentType commentType
) {
}
