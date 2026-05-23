package de.tum.cit.aet.thesis.dependency.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * DTO representing metadata from a CycloneDX SBOM.
 *
 * @param timestamp     when the SBOM was generated
 * @param componentName the name of the main component
 * @param version       the version of the main component
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record SbomMetadataDTO(
		Instant timestamp,
		String componentName,
		String version
) {
}
