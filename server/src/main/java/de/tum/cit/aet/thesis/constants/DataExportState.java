package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DataExportState {
	REQUESTED("REQUESTED"),
	IN_CREATION("IN_CREATION"),
	EMAIL_SENT("EMAIL_SENT"),
	EMAIL_FAILED("EMAIL_FAILED"),
	DOWNLOADED("DOWNLOADED"),
	DELETED("DELETED"),
	DOWNLOADED_DELETED("DOWNLOADED_DELETED"),
	FAILED("FAILED");

	private final String value;
}
