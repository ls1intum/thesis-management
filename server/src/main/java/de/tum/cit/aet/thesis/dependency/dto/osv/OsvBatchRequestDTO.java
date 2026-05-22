package de.tum.cit.aet.thesis.dependency.dto.osv;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * DTO representing a batch request to the OSV API.
 * Contains a list of package queries to check for vulnerabilities in a single request.
 *
 * @param queries the list of package queries (max 1000 per request according to OSV API limits)
 * @see <a href="https://osv.dev/docs/#tag/api/operation/OSV_QueryAffectedBatch">OSV Batch API Documentation</a>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record OsvBatchRequestDTO(List<OsvQueryDTO> queries) {

	/**
	 * Maximum number of queries allowed in a single batch request as per OSV API limits.
	 */
	public static final int MAX_BATCH_SIZE = 1000;

	public OsvBatchRequestDTO {
		if (queries != null && queries.size() > MAX_BATCH_SIZE) {
			throw new IllegalArgumentException("OSV batch request cannot contain more than " + MAX_BATCH_SIZE + " queries");
		}
	}
}
