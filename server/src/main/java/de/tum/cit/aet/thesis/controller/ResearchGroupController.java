package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.CreateResearchGroupPayload;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.ResearchGroupDto;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.AuthenticationService;
import de.tum.cit.aet.thesis.service.ResearchGroupService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/research-groups")
public class ResearchGroupController {

  private final ResearchGroupService researchGroupService;

  @Autowired
  public ResearchGroupController(ResearchGroupService researchGroupService) {
    this.researchGroupService = researchGroupService;
  }

  @GetMapping
  public ResponseEntity<PaginationDto<ResearchGroupDto>> getResearchGroups(
      @RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "") String[] heads,
      @RequestParam(required = false, defaultValue = "") String[] campuses,
      @RequestParam(required = false, defaultValue = "false") boolean includeArchived,
      @RequestParam(required = false, defaultValue = "0") Integer page,
      @RequestParam(required = false, defaultValue = "50") Integer limit,
      @RequestParam(required = false, defaultValue = "name") String sortBy,
      @RequestParam(required = false, defaultValue = "desc") String sortOrder
  ) {
    Page<ResearchGroup> researchGroups = researchGroupService.getAll(
        heads,
        campuses,
        includeArchived,
        search,
        page,
        limit,
        sortBy,
        sortOrder
    );

    return ResponseEntity.ok(PaginationDto.fromSpringPage(
        researchGroups.map(ResearchGroupDto::fromResearchGroupEntity)));
  }

  @GetMapping("/{researchGroupId}")
  public ResponseEntity<ResearchGroupDto> getResearchGroup(
      @PathVariable("researchGroupId") UUID researchGroupId
  ) {
    ResearchGroup researchGroup = researchGroupService.findById(researchGroupId);

    return ResponseEntity.ok(ResearchGroupDto.fromResearchGroupEntity(researchGroup));
  }

  @PostMapping
  @PreAuthorize("hasRole('admin')")
  public ResponseEntity<ResearchGroupDto> createResearchGroup(
      @RequestBody CreateResearchGroupPayload payload
  ) {
    ResearchGroup researchGroup = researchGroupService.createResearchGroup(
        RequestValidator.validateNotNull(payload.headId()),
        RequestValidator.validateNotNull(payload.name()),
        payload.abbreviation(),
        payload.description(),
        payload.websiteUrl(),
        payload.campus()
    );

    return ResponseEntity.ok(ResearchGroupDto.fromResearchGroupEntity(researchGroup));
  }

  @PutMapping("/{researchGroupId}")
  @PreAuthorize("hasRole('admin')")
  public ResponseEntity<ResearchGroupDto> updateResearchGroup(
      @PathVariable("researchGroupId") UUID researchGroupId,
      @RequestBody CreateResearchGroupPayload payload
  ) {
    ResearchGroup researchGroup = researchGroupService.findById(researchGroupId);

    researchGroup = researchGroupService.updateResearchGroup(
        researchGroup,
        RequestValidator.validateNotNull(payload.headId()),
        RequestValidator.validateNotNull(payload.name()),
        payload.abbreviation(),
        payload.description(),
        payload.websiteUrl(),
        payload.campus()
    );

    return ResponseEntity.ok(ResearchGroupDto.fromResearchGroupEntity(researchGroup));
  }

  @PatchMapping("/{researchGroupId}/archive")
  @PreAuthorize("hasRole('admin')")
  public ResponseEntity<Void> archiveResearchGroup(
      @PathVariable("researchGroupId") UUID researchGroupId,
      JwtAuthenticationToken jwt
  ) {
    ResearchGroup researchGroup = researchGroupService.findById(researchGroupId);
    researchGroupService.archiveResearchGroup(researchGroup);

    return ResponseEntity.noContent().build();
  }
}