package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.UpdateResearchGroupSettingsPayload;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsPhasesDTO;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.service.ResearchGroupSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v2/research-group-settings")
public class ResearchGroupSettingsController {
    private final ResearchGroupSettingsService service;

    @Autowired
    public ResearchGroupSettingsController(ResearchGroupSettingsService service) {
        this.service = service;
    }

    @GetMapping("/{researchGroupId}")
    @PreAuthorize("hasAnyRole('admin', 'group-admin')")
    public ResponseEntity<ResearchGroupSettingsDTO> getSettings(@PathVariable UUID researchGroupId) {
        Optional<ResearchGroupSettings> settings = service.getByResearchGroupId(researchGroupId);
        if (settings.isEmpty()) {
            throw new ResourceNotFoundException("Research group settings not found");
        }
        return ResponseEntity.ok(ResearchGroupSettingsDTO.fromEntity(settings.get()));
    }

    @PostMapping("/{researchGroupId}/automatic-reject")
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

        ResearchGroupSettings saved = service.saveOrUpdate(toSave);
        return ResponseEntity.ok(ResearchGroupSettingsDTO.fromEntity(saved));
    }

    @GetMapping("/{researchGroupId}/phase-settings")
    @PreAuthorize("hasAnyRole('admin', 'group-admin')")
    public ResponseEntity<ResearchGroupSettingsPhasesDTO> getPhaseSettings(@PathVariable UUID researchGroupId) {
        Optional<ResearchGroupSettings> existingSettings = service.getByResearchGroupId(researchGroupId);
        ResearchGroupSettings settings = existingSettings.orElseGet(ResearchGroupSettings::new);

        return ResponseEntity.ok(
                ResearchGroupSettingsPhasesDTO.fromEntity(settings));
    }
}

