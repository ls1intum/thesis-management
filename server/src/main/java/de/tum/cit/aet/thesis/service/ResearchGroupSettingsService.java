package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.dto.GradingSchemeComponentDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsGradingSchemeDTO;
import de.tum.cit.aet.thesis.entity.GradingSchemeComponent;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.GradingSchemeComponentRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Manages research group-specific settings such as presentation slot duration. */
@Service
public class ResearchGroupSettingsService {
	private final ResearchGroupSettingsRepository repository;
	private final GradingSchemeComponentRepository gradingSchemeComponentRepository;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	private final ResearchGroupRepository researchGroupRepository;

	/**
	 * Injects all required repositories and providers.
	 *
	 * @param repository the research group settings repository
	 * @param gradingSchemeComponentRepository the grading scheme component repository
	 * @param researchGroupRepository the research group repository
	 * @param currentUserProviderProvider the current user provider
	 */
	@Autowired
	public ResearchGroupSettingsService(
			ResearchGroupSettingsRepository repository,
			GradingSchemeComponentRepository gradingSchemeComponentRepository,
			ResearchGroupRepository researchGroupRepository,
			ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
		this.repository = repository;
		this.gradingSchemeComponentRepository = gradingSchemeComponentRepository;
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

	/**
	 * Loads the grading scheme for a research group, verifying the current user's access.
	 *
	 * @param researchGroupId the ID of the research group
	 * @return the grading scheme DTO
	 */
	public ResearchGroupSettingsGradingSchemeDTO getGradingScheme(UUID researchGroupId) {
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
				.orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		List<GradingSchemeComponent> components = gradingSchemeComponentRepository
				.findByResearchGroupIdOrderByPositionAsc(researchGroupId);
		List<GradingSchemeComponentDTO> dtos = components.stream()
				.map(GradingSchemeComponentDTO::fromEntity).toList();
		return new ResearchGroupSettingsGradingSchemeDTO(dtos);
	}

	/**
	 * Validates and saves a grading scheme for a research group, verifying the current user's access.
	 * Deletes existing components and replaces them with the new ones.
	 *
	 * @param researchGroupId the ID of the research group
	 * @param gradingScheme the new grading scheme to save
	 */
	public void saveGradingScheme(UUID researchGroupId, ResearchGroupSettingsGradingSchemeDTO gradingScheme) {
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
				.orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		if (gradingScheme.components() == null) {
			return;
		}

		if (gradingScheme.components().size() > 50) {
			throw new ResourceInvalidParametersException("A maximum of 50 grading scheme components is allowed.");
		}

		BigDecimal regularWeightSum = BigDecimal.ZERO;
		for (GradingSchemeComponentDTO dto : gradingScheme.components()) {
			if (dto == null) {
				throw new ResourceInvalidParametersException("Component must not be null.");
			}
			if (dto.name() == null || dto.name().isBlank()) {
				throw new ResourceInvalidParametersException("Component name must not be empty.");
			}
			if (dto.name().length() > 255) {
				throw new ResourceInvalidParametersException("Component name must not exceed 255 characters.");
			}
			if (!Boolean.TRUE.equals(dto.isBonus())) {
				if (dto.weight() == null) {
					throw new ResourceInvalidParametersException("Component weight must not be null.");
				}
				if (dto.weight().scale() > 2) {
					throw new ResourceInvalidParametersException("Weight must have at most 2 decimal places.");
				}
				if (dto.weight().compareTo(BigDecimal.ZERO) <= 0) {
					throw new ResourceInvalidParametersException("Component weight must be positive.");
				}
				regularWeightSum = regularWeightSum.add(dto.weight());
			}
		}
		if (!gradingScheme.components().isEmpty()
				&& regularWeightSum.compareTo(BigDecimal.valueOf(100)) != 0) {
			throw new ResourceInvalidParametersException("Regular component weights must sum to 100%.");
		}

		gradingSchemeComponentRepository.deleteAllByResearchGroupId(researchGroupId);

		if (!gradingScheme.components().isEmpty()) {
			List<GradingSchemeComponent> entities = new ArrayList<>();
			for (int i = 0; i < gradingScheme.components().size(); i++) {
				GradingSchemeComponentDTO dto = gradingScheme.components().get(i);
				GradingSchemeComponent entity = new GradingSchemeComponent();
				entity.setResearchGroup(researchGroup);
				entity.setName(dto.name());
				entity.setWeight(Boolean.TRUE.equals(dto.isBonus()) ? BigDecimal.ZERO : dto.weight());
				entity.setIsBonus(Boolean.TRUE.equals(dto.isBonus()));
				entity.setPosition(i);
				entities.add(entity);
			}
			gradingSchemeComponentRepository.saveAll(entities);
		}
	}
}
