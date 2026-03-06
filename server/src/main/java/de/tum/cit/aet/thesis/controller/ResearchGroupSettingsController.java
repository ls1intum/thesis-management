package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.UpdateResearchGroupSettingsPayload;
import de.tum.cit.aet.thesis.dto.GradingSchemeComponentDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsGradingSchemeDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsPhasesDTO;
import de.tum.cit.aet.thesis.entity.GradingSchemeComponent;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.repository.GradingSchemeComponentRepository;
import de.tum.cit.aet.thesis.service.ResearchGroupService;
import de.tum.cit.aet.thesis.service.ResearchGroupSettingsService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** REST controller for managing research group settings such as auto-reject and presentation configuration. */
@RestController
@RequestMapping("/v2/research-group-settings")
public class ResearchGroupSettingsController {
	private final ResearchGroupSettingsService service;
	private final GradingSchemeComponentRepository gradingSchemeComponentRepository;
	private final ResearchGroupService researchGroupService;

	@Autowired
	public ResearchGroupSettingsController(ResearchGroupSettingsService service,
			GradingSchemeComponentRepository gradingSchemeComponentRepository,
			ResearchGroupService researchGroupService) {
		this.service = service;
		this.gradingSchemeComponentRepository = gradingSchemeComponentRepository;
		this.researchGroupService = researchGroupService;
	}

	/**
	 * Retrieves the settings for a research group by its ID.
	 *
	 * @param researchGroupId the ID of the research group
	 * @return the research group settings
	 */
	@GetMapping("/{researchGroupId}")
	@PreAuthorize("hasAnyRole('admin', 'group-admin')")
	public ResponseEntity<ResearchGroupSettingsDTO> getSettings(@PathVariable UUID researchGroupId) {
		Optional<ResearchGroupSettings> settings = service.getByResearchGroupId(researchGroupId);
		ResearchGroupSettings returnSettings = settings.orElseGet(ResearchGroupSettings::new);

		ResearchGroupSettingsGradingSchemeDTO gradingScheme = loadGradingScheme(researchGroupId);
		return ResponseEntity.ok(ResearchGroupSettingsDTO.fromEntity(returnSettings, gradingScheme));
	}

	/**
	 * Creates or updates the settings for a research group.
	 *
	 * @param researchGroupId the ID of the research group
	 * @param newSettings the payload containing the updated settings
	 * @return the saved research group settings
	 */
	@PostMapping("/{researchGroupId}")
	@PreAuthorize("hasAnyRole('admin', 'group-admin')")
	public ResponseEntity<ResearchGroupSettingsDTO> createOrUpdateRejectSettings(@PathVariable UUID researchGroupId, @RequestBody UpdateResearchGroupSettingsPayload newSettings) {
		Optional<ResearchGroupSettings> settings = service.getByResearchGroupId(researchGroupId);
		ResearchGroupSettings toSave = settings.orElseGet(ResearchGroupSettings::new);

		if (toSave.getResearchGroupId() == null) {
			toSave.setResearchGroupId(researchGroupId);
		}
		if (newSettings.rejectSettings() != null) {
			toSave.setAutomaticRejectEnabled(newSettings.rejectSettings().automaticRejectEnabled());
			toSave.setRejectDuration(newSettings.rejectSettings().rejectDuration());
		}
		if (newSettings.presentationSettings() != null) {
			toSave.setPresentationSlotDuration(newSettings.presentationSettings().presentationSlotDuration());
		}
		if (newSettings.phaseSettings() != null) {
			toSave.setProposalPhaseActive(newSettings.phaseSettings().proposalPhaseActive());
		}
		if (newSettings.emailSettings() != null) {
			String validatedEmail = RequestValidator.validateEmailAllowNull(
					newSettings.emailSettings().applicationNotificationEmail() == null ? null : newSettings.emailSettings().applicationNotificationEmail().trim());
			toSave.setApplicationNotificationEmail(validatedEmail);
		}
		if (newSettings.writingGuideSettings() != null) {
			String link = newSettings.writingGuideSettings().scientificWritingGuideLink();
			toSave.setScientificWritingGuideLink(link != null && !link.trim().isEmpty() ? link.trim() : null);
		}
		if (newSettings.applicationEmailSettings() != null) {
			toSave.setIncludeApplicationDataInEmail(
					newSettings.applicationEmailSettings().includeApplicationDataInEmail());
		}

		if (newSettings.gradingSchemeSettings() != null) {
			saveGradingScheme(researchGroupId, newSettings.gradingSchemeSettings());
		}

		ResearchGroupSettings saved = service.saveOrUpdate(toSave);
		ResearchGroupSettingsGradingSchemeDTO gradingScheme = loadGradingScheme(researchGroupId);
		return ResponseEntity.ok(ResearchGroupSettingsDTO.fromEntity(saved, gradingScheme));
	}

	/**
	 * Retrieves the phase settings for a research group.
	 *
	 * @param researchGroupId the ID of the research group
	 * @return the phase settings for the research group
	 */
	@GetMapping("/{researchGroupId}/phase-settings")
	@PreAuthorize("hasAnyRole('admin', 'group-admin')")
	public ResponseEntity<ResearchGroupSettingsPhasesDTO> getPhaseSettings(@PathVariable UUID researchGroupId) {
		Optional<ResearchGroupSettings> existingSettings = service.getByResearchGroupId(researchGroupId);
		ResearchGroupSettings settings = existingSettings.orElseGet(ResearchGroupSettings::new);

		return ResponseEntity.ok(
				ResearchGroupSettingsPhasesDTO.fromEntity(settings));
	}

	@GetMapping("/{researchGroupId}/grading-scheme")
	@PreAuthorize("hasAnyRole('admin', 'group-admin', 'supervisor', 'advisor')")
	public ResponseEntity<ResearchGroupSettingsGradingSchemeDTO> getGradingScheme(@PathVariable UUID researchGroupId) {
		return ResponseEntity.ok(loadGradingScheme(researchGroupId));
	}

	private ResearchGroupSettingsGradingSchemeDTO loadGradingScheme(UUID researchGroupId) {
		List<GradingSchemeComponent> components = gradingSchemeComponentRepository
				.findByResearchGroupIdOrderByPositionAsc(researchGroupId);
		List<GradingSchemeComponentDTO> dtos = components.stream()
				.map(GradingSchemeComponentDTO::fromEntity).toList();
		return new ResearchGroupSettingsGradingSchemeDTO(dtos);
	}

	private void saveGradingScheme(UUID researchGroupId, ResearchGroupSettingsGradingSchemeDTO gradingScheme) {
		if (gradingScheme.components() != null) {
			BigDecimal regularWeightSum = BigDecimal.ZERO;
			for (GradingSchemeComponentDTO dto : gradingScheme.components()) {
				if (dto.name() == null || dto.name().isBlank()) {
					throw new ResourceInvalidParametersException("Component name must not be empty.");
				}
				if (!Boolean.TRUE.equals(dto.isBonus())) {
					if (dto.weight() == null) {
						throw new ResourceInvalidParametersException("Component weight must not be null.");
					}
					regularWeightSum = regularWeightSum.add(dto.weight());
				}
			}
			if (!gradingScheme.components().isEmpty()
					&& regularWeightSum.compareTo(BigDecimal.valueOf(100)) != 0) {
				throw new ResourceInvalidParametersException("Regular component weights must sum to 100%.");
			}
		}

		gradingSchemeComponentRepository.deleteAllByResearchGroupId(researchGroupId);

		if (gradingScheme.components() != null) {
			var researchGroup = researchGroupService.findById(researchGroupId);
			for (int i = 0; i < gradingScheme.components().size(); i++) {
				GradingSchemeComponentDTO dto = gradingScheme.components().get(i);
				GradingSchemeComponent entity = new GradingSchemeComponent();
				entity.setResearchGroup(researchGroup);
				entity.setName(dto.name());
				entity.setWeight(dto.weight());
				entity.setIsBonus(Boolean.TRUE.equals(dto.isBonus()));
				entity.setPosition(i);
				gradingSchemeComponentRepository.save(entity);
			}
		}
	}
}
