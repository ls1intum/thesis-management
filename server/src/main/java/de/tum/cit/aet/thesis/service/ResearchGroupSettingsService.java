package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** Manages research group-specific settings such as presentation slot duration. */
@Service
public class ResearchGroupSettingsService {
	private final ResearchGroupSettingsRepository repository;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	private final ResearchGroupRepository researchGroupRepository;

	/**
	 * Injects the settings repository, research group repository, and current user provider.
	 *
	 * @param repository the research group settings repository
	 * @param researchGroupRepository the research group repository
	 * @param currentUserProviderProvider the current user provider
	 */
	@Autowired
	public ResearchGroupSettingsService(
			ResearchGroupSettingsRepository repository,
			ResearchGroupRepository researchGroupRepository,
			ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
		this.repository = repository;
		this.currentUserProviderProvider = currentUserProviderProvider;
		this.researchGroupRepository = researchGroupRepository;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Persists or updates the given research group settings.
	 *
	 * @param settings the research group settings to save
	 * @return the saved research group settings
	 */
	public ResearchGroupSettings saveOrUpdate(ResearchGroupSettings settings) {
		return repository.save(settings);
	}

	/**
	 * Returns the settings for the given research group, verifying the current user's access.
	 *
	 * @param researchGroupId the ID of the research group
	 * @return an Optional containing the settings if found
	 */
	public Optional<ResearchGroupSettings> getByResearchGroupId(UUID researchGroupId) {
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId).orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);
		return repository.findById(researchGroupId);
	}

	/**
	 * Returns the configured presentation slot duration in minutes, defaulting to 30 if not set.
	 *
	 * @param researchGroupId the ID of the research group
	 * @return the presentation duration in minutes
	 */
	public int getPresentationDurationInMinutes(UUID researchGroupId) {
		return repository.findById(researchGroupId)
				.map(ResearchGroupSettings::getPresentationSlotDuration)
				.orElse(30);
	}
}
