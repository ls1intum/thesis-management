package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThesisCommentType {
	SUPERVISOR("SUPERVISOR"),
	THESIS("THESIS");

	private final String value;
}
