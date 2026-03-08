package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThesisRoleName {
	STUDENT("STUDENT"),
	SUPERVISOR("SUPERVISOR"),
	EXAMINER("EXAMINER");

	private final String value;
}
