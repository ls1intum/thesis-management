package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.UpdateResearchGroupSettingsPayload;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsGradingSchemeDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsPhasesDTO;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
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

import java.util.Optional;
import java.util.UUID;

/** REST controller for managing research group settings such as auto-reject and presentation configuration. */
@RestController
@RequestMapping("/v2/research-group-settings")
public class ResearchGroupSettingsController {
	private final ResearchGroupSettingsService service;

	/**
	 * Injects the research group settings service.
	 *
	 * @param service the research group settings service
	 */
	@Autowired
	public ResearchGroupSettingsController(ResearchGroupSettingsService service) {
		this.service = service;
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

		ResearchGroupSettingsGradingSchemeDTO gradingScheme = service.getGradingScheme(researchGroupId);
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
			service.saveGradingScheme(researchGroupId, newSettings.gradingSchemeSettings());
		}

		ResearchGroupSettings saved = service.saveOrUpdate(toSave);
		ResearchGroupSettingsGradingSchemeDTO gradingScheme = service.getGradingScheme(researchGroupId);
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

	/**
	 * Retrieves the grading scheme for a research group.
	 *
	 * @param researchGroupId the ID of the research group
	 * @return the grading scheme
	 */
	@GetMapping("/{researchGroupId}/grading-scheme")
	@PreAuthorize("hasAnyRole('admin', 'group-admin', 'supervisor', 'advisor')")
	public ResponseEntity<ResearchGroupSettingsGradingSchemeDTO> getGradingScheme(@PathVariable UUID researchGroupId) {
		return ResponseEntity.ok(service.getGradingScheme(researchGroupId));
	}
}
