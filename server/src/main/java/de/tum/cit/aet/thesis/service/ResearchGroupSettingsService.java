package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResearchGroupSettingsService {
    private final ResearchGroupSettingsRepository repository;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    private final ResearchGroupRepository researchGroupRepository;

    @Autowired
    public ResearchGroupSettingsService(ResearchGroupSettingsRepository repository, ResearchGroupRepository researchGroupRepository, ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
        this.repository = repository;
        this.currentUserProviderProvider = currentUserProviderProvider;
        this.researchGroupRepository = researchGroupRepository;
    }

    private CurrentUserProvider currentUserProvider() {
        return currentUserProviderProvider.getObject();
    }

    public ResearchGroupSettings saveOrUpdate(ResearchGroupSettings settings) {
        return repository.save(settings);
    }

    public Optional<ResearchGroupSettings> getByResearchGroupId(UUID researchGroupId) {
        ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId).orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
        currentUserProvider().assertCanAccessResearchGroup(researchGroup);
        return repository.findById(researchGroupId);
    }


}

