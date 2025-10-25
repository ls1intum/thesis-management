package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.CreateResearchGroupPayload;
import de.tum.cit.aet.thesis.dto.LightResearchGroupDto;
import de.tum.cit.aet.thesis.dto.LightUserDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.ResearchGroupDto;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.ResearchGroupService;
import de.tum.cit.aet.thesis.utility.RequestValidator;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  @GetMapping("/light")
  public ResponseEntity<List<LightResearchGroupDto>> getLightResearchGroups(
          @RequestParam(required = false) String search
  ) {
    Page<ResearchGroup> groups = researchGroupService.getAllLight(search);

    List<LightResearchGroupDto> dtos = groups.getContent()
            .stream()
            .map(LightResearchGroupDto::fromResearchGroupEntity)
            .toList();

    return ResponseEntity.ok(dtos);
  }

  @GetMapping("/light/active")
  public ResponseEntity<List<LightResearchGroupDto>> getActiveLightResearchGroups(
  ) {
    List<ResearchGroup> activeGroups = researchGroupService.getActiveResearchGroupsForUser();

    return ResponseEntity.ok(
            activeGroups.stream()
                    .map(LightResearchGroupDto::fromResearchGroupEntity)
                    .distinct()
                    .toList()
    );
  }

  @GetMapping("/{researchGroupId}")
  public ResponseEntity<ResearchGroupDto> getResearchGroup(
      @PathVariable("researchGroupId") UUID researchGroupId
  ) {
    ResearchGroup researchGroup = researchGroupService.findById(researchGroupId, true);

    return ResponseEntity.ok(ResearchGroupDto.fromResearchGroupEntity(researchGroup));
  }

    @GetMapping("/{researchGroupId}/members")
    @PreAuthorize("hasAnyRole('admin', 'group-admin')")
    public ResponseEntity<PaginationDto<LightUserDto>> getResearchGroupMembers(
        @PathVariable("researchGroupId") UUID researchGroupId,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "50") Integer limit,
        @RequestParam(required = false, defaultValue = "joinedAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortOrder
    ) {
      Page<User> users = researchGroupService.getAllResearchGroupMembers(researchGroupId, page, limit, sortBy, sortOrder);

      return ResponseEntity.ok(PaginationDto.fromSpringPage(users.map(LightUserDto::fromUserEntity)));
    }

  @PostMapping
  @PreAuthorize("hasRole('admin')")
  public ResponseEntity<ResearchGroupDto> createResearchGroup(
      @RequestBody CreateResearchGroupPayload payload
  ) {
    ResearchGroup researchGroup = researchGroupService.createResearchGroup(
        RequestValidator.validateNotNull(payload.headUsername()),
        RequestValidator.validateNotNull(payload.name()),
        RequestValidator.validateNotNull(payload.abbreviation()),
        RequestValidator.validateStringMaxLengthAllowNull(payload.description(), 500),
        payload.websiteUrl(),
        payload.campus()
    );

    return ResponseEntity.ok(ResearchGroupDto.fromResearchGroupEntity(researchGroup));
  }

  @PutMapping("/{researchGroupId}")
  @PreAuthorize("hasAnyRole('admin', 'group-admin')")
  public ResponseEntity<ResearchGroupDto> updateResearchGroup(
      @PathVariable("researchGroupId") UUID researchGroupId,
      @RequestBody CreateResearchGroupPayload payload
  ) {
    ResearchGroup researchGroup = researchGroupService.findById(researchGroupId);

    researchGroup = researchGroupService.updateResearchGroup(
        researchGroup,
        RequestValidator.validateNotNull(payload.headUsername()),
        RequestValidator.validateNotNull(payload.name()),
        RequestValidator.validateNotNull(payload.abbreviation()),
        RequestValidator.validateStringMaxLengthAllowNull(payload.description(), 500),
        payload.websiteUrl(),
        payload.campus()
    );

    return ResponseEntity.ok(ResearchGroupDto.fromResearchGroupEntity(researchGroup));
  }

  @PatchMapping("/{researchGroupId}/archive")
  @PreAuthorize("hasRole('admin')")
  public ResponseEntity<Void> archiveResearchGroup(
      @PathVariable("researchGroupId") UUID researchGroupId
  ) {
    ResearchGroup researchGroup = researchGroupService.findById(researchGroupId);
    researchGroupService.archiveResearchGroup(researchGroup);

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{researchGroupId}/assign/{username}")
  @PreAuthorize("hasAnyRole('admin', 'group-admin')")
  public ResponseEntity<LightUserDto> assignUserToResearchGroup(
      @PathVariable("researchGroupId") UUID researchGroupId,
      @PathVariable("username") String username
  ) {
    User user = researchGroupService.assignUserToResearchGroup(username, researchGroupId);

    return ResponseEntity.ok(LightUserDto.fromUserEntity(user));
  }

    @PutMapping("/{researchGroupId}/remove/{userId}")
    @PreAuthorize("hasAnyRole('admin', 'group-admin')")
    public ResponseEntity<LightUserDto> removeUserFromResearchGroup(
        @PathVariable("researchGroupId") UUID researchGroupId,
        @PathVariable("userId") UUID userId
    ) {
        User user = researchGroupService.removeUserFromResearchGroup(userId, researchGroupId);

        //User can be removed no matter if they have open topic/thesis or not
        return ResponseEntity.ok(LightUserDto.fromUserEntity(user));
    }

  @PutMapping("/{researchGroupId}/member/{userId}/role")
  @PreAuthorize("hasAnyRole('admin', 'group-admin')")
  public ResponseEntity<LightUserDto> updateResearchGroupMemberRole(
          @PathVariable UUID researchGroupId,
          @PathVariable UUID userId,
          @RequestParam("role") String role
  ) {
    User user = researchGroupService.updateResearchGroupMemberRole(researchGroupId, userId, role);
    return ResponseEntity.ok(LightUserDto.fromUserEntity(user));
  }

  @PutMapping("/{researchGroupId}/member/{userId}/group-admin")
  @PreAuthorize("hasAnyRole('admin', 'group-admin')")
  public ResponseEntity<LightUserDto> updateResearchGroupAdminRole(
          @PathVariable UUID researchGroupId,
          @PathVariable UUID userId
  ) {
    User user = researchGroupService.changeResearchGroupAdminRole(researchGroupId, userId);
    return ResponseEntity.ok(LightUserDto.fromUserEntity(user));
  }
}