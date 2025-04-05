package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResearchGroupService {

  private final ResearchGroupRepository researchGroupRepository;

  public ResearchGroup findById(UUID researchGroupId) {
    return researchGroupRepository.findById(researchGroupId)
        .orElseThrow(() -> new ResourceNotFoundException(
            String.format("Research Group with id %s not found.", researchGroupId)));
  }
}