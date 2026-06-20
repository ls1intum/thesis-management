package de.tum.cit.aet.thesis.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThesisAbstractSource {
	/** The abstract was entered or edited by a human and must never be auto-overwritten. */
	MANUAL("MANUAL"),
	/** The abstract was auto-filled by extracting it from an uploaded PDF and may be refreshed. */
	EXTRACTED("EXTRACTED");

	private final String value;
}
