package de.tum.cit.aet.thesis.thesis.controller.payload;

import de.tum.cit.aet.thesis.thesis.constants.ThesisState;

import java.time.Instant;

public record ThesisStatePayload(
		ThesisState state,
		Instant changedAt
) { }
