package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.PublishedThesisDto;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.service.ThesisService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** REST controller for publicly accessible finished thesis data. */
@Slf4j
@RestController
@RequestMapping("/v2/published-theses")
public class PublishedThesisController {
	private final ThesisService thesisService;

	/**
	 * Injects the thesis service.
	 *
	 * @param thesisService the thesis service
	 */
	@Autowired
	public PublishedThesisController(ThesisService thesisService) {
		this.thesisService = thesisService;
	}

	/**
	 * Retrieves a paginated list of finished theses with optional filtering.
	 *
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction
	 * @param search the search query to filter theses
	 * @param researchGroupIds the research group IDs to filter by
	 * @param types the thesis types to filter by
	 * @return the paginated list of published theses
	 */
	@GetMapping
	public ResponseEntity<PaginationDto<PublishedThesisDto>> getTheses(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "endDate") String sortBy,
			@RequestParam(required = false, defaultValue = "desc") String sortOrder,
			@RequestParam(required = false) String search,
			@RequestParam(required = false, defaultValue = "") UUID[] researchGroupIds,
			@RequestParam(required = false) String[] types
	) {
		limit = RequestValidator.clampPageSize(limit);
		Page<Thesis> theses = thesisService.getAll(
				null,
				true,
				search,
				new ThesisState[]{ThesisState.FINISHED},
				types,
				page,
				limit,
				sortBy,
				sortOrder,
				researchGroupIds
		);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(theses.map(PublishedThesisDto::fromThesisEntity)));
	}

	/**
	 * Downloads the PDF file of a finished thesis.
	 *
	 * @param thesisId the ID of the thesis
	 * @return the thesis PDF as a resource
	 */
	@GetMapping("/{thesisId}/thesis")
	public ResponseEntity<Resource> getThesisFile(
			@PathVariable UUID thesisId
	) {
		Thesis thesis = thesisService.findById(thesisId);

		if (!thesis.hasReadAccess(null)) {
			throw new AccessDeniedException("You do not have the required permissions to view this thesis");
		}

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=thesis_%s.pdf", thesisId))
				.body(thesisService.getThesisFile(thesis.getLatestFile("THESIS").orElseThrow()));
	}
}
