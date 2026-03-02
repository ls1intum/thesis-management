package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.PublishedPresentationDto;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.service.ThesisPresentationService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** REST controller for publicly accessible thesis presentation data. */
@Slf4j
@RestController
@RequestMapping("/v2/published-presentations")
public class PublishedPresentationController {
	private final ThesisPresentationService thesisPresentationService;

	/**
	 * Injects the thesis presentation service.
	 *
	 * @param thesisPresentationService the thesis presentation service
	 */
	@Autowired
	public PublishedPresentationController(ThesisPresentationService thesisPresentationService) {
		this.thesisPresentationService = thesisPresentationService;
	}

	/**
	 * Retrieves a paginated list of published thesis presentations.
	 *
	 * @param includeDrafts whether to include draft presentations
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction
	 * @param researchGroupId the research group ID to filter by
	 * @return the paginated list of published presentations
	 */
	@GetMapping()
	public ResponseEntity<PaginationDto<PublishedPresentationDto>> getPresentations(
			@RequestParam(required = false, defaultValue = "false") boolean includeDrafts,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "scheduledAt") String sortBy,
			@RequestParam(required = false, defaultValue = "asc") String sortOrder,
			@RequestParam(required = false) UUID researchGroupId
	) {
		limit = RequestValidator.clampPageSize(limit);
		Page<ThesisPresentation> presentations = thesisPresentationService.getPublicPresentations(
				includeDrafts,
				page,
				limit,
				sortBy,
				sortOrder,
				researchGroupId
		);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(
				presentations.map(PublishedPresentationDto::fromPresentationEntity)
		));
	}

	/**
	 * Retrieves a single published presentation by its ID.
	 *
	 * @param presentationId the ID of the presentation
	 * @return the published presentation
	 */
	@GetMapping("/{presentationId}")
	public ResponseEntity<PublishedPresentationDto> getPresentation(
			@PathVariable UUID presentationId
	) {
		ThesisPresentation presentation = thesisPresentationService.getPublicPresentation(presentationId);

		return ResponseEntity.ok(PublishedPresentationDto.fromPresentationEntity(presentation));
	}
}
