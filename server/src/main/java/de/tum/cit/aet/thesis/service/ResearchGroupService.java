package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResearchGroupService {

    private final ResearchGroupRepository repository;

    public Optional<ResearchGroup> getById(UUID id) {
        return repository.findById(id);
    }
}