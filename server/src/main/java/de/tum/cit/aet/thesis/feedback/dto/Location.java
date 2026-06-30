package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Location(Integer page, String section, String quote) {
}
