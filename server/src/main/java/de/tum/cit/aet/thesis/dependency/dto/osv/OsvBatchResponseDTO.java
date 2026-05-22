package de.tum.cit.aet.thesis.dependency.dto.osv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * DTO representing the response from the OSV batch API.
 * Contains results for each query in the same order as the request.
 *
 * @param results the list of vulnerability results, one per query in the original request
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsvBatchResponseDTO(List<OsvVulnerabilityResultDTO> results) {
}
