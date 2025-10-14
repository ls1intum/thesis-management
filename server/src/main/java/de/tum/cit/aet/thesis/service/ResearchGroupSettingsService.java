package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResearchGroupSettingsService {
    private final ResearchGroupSettingsRepository repository;

    @Autowired
    public ResearchGroupSettingsService(ResearchGroupSettingsRepository repository) {
        this.repository = repository;
    }

    public ResearchGroupSettings saveOrUpdate(ResearchGroupSettings settings) {
        return repository.save(settings);
    }

    public Optional<ResearchGroupSettings> getByResearchGroupId(UUID researchGroupId) {
        return repository.findById(researchGroupId);
    }
}

