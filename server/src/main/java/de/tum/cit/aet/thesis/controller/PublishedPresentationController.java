package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.PublishedPresentationDto;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.service.ThesisPresentationService;
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

@Slf4j
@RestController
@RequestMapping("/v2/published-presentations")
public class PublishedPresentationController {
	private final ThesisPresentationService thesisPresentationService;

	@Autowired
	public PublishedPresentationController(ThesisPresentationService thesisPresentationService) {
		this.thesisPresentationService = thesisPresentationService;
	}

	@GetMapping()
	public ResponseEntity<PaginationDto<PublishedPresentationDto>> getPresentations(
			@RequestParam(required = false, defaultValue = "false") boolean includeDrafts,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "scheduledAt") String sortBy,
			@RequestParam(required = false, defaultValue = "asc") String sortOrder,
			@RequestParam(required = false) UUID researchGroupId
	) {
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

	@GetMapping("/{presentationId}")
	public ResponseEntity<PublishedPresentationDto> getPresentation(
			@PathVariable UUID presentationId
	) {
		ThesisPresentation presentation = thesisPresentationService.getPublicPresentation(presentationId);

		return ResponseEntity.ok(PublishedPresentationDto.fromPresentationEntity(presentation));
	}
}
