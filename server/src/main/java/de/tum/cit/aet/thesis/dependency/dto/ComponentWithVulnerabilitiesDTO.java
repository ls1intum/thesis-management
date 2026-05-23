package de.tum.cit.aet.thesis.dependency.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

/**
 * DTO representing a component and its associated vulnerabilities.
 *
 * @param componentKey    unique identifier for the component (typically the purl)
 * @param vulnerabilities list of vulnerabilities affecting this component
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ComponentWithVulnerabilitiesDTO(
		String componentKey,
		List<VulnerabilityDTO> vulnerabilities
) implements Serializable {
}
