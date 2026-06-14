package de.tum.cit.aet.thesis.core.topic.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TopicState {
	OPEN("OPEN"),
	CLOSED("WRITING"),
	DRAFT("DRAFT"),
	EXPIRED("EXPIRED");

	private final String value;
}
