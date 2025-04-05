package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.ResearchGroupDto;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.service.ResearchGroupService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/research-groups")
@RequiredArgsConstructor
public class ResearchGroupController {

  private final ResearchGroupService researchGroupService;

  @GetMapping("/{researchGroupId}")
  public ResponseEntity<ResearchGroupDto> getResearchGroup(@PathVariable UUID researchGroupId) {
    ResearchGroup researchGroup = researchGroupService.findById(researchGroupId);

    return ResponseEntity.ok(ResearchGroupDto.fromResearchGroupEntity(researchGroup));
  }
}