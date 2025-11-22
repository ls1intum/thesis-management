package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.UpdateResearchGroupSettingsPayload;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsRejectDTO;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.service.ResearchGroupSettingsService;
import org.springframework.beans.factory.ObjectProvider;
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
    public ResponseEntity<ResearchGroupSettings> getSettings(@PathVariable UUID researchGroupId) {
        Optional<ResearchGroupSettings> settings = service.getByResearchGroupId(researchGroupId);
        return settings.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{researchGroupId}/automatic-reject")
    @PreAuthorize("hasAnyRole('admin', 'group-admin')")
    public ResponseEntity<ResearchGroupSettings> createOrUpdateRejectSettings(@PathVariable UUID researchGroupId, @RequestBody UpdateResearchGroupSettingsPayload newSettings) {
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

        ResearchGroupSettings saved = service.saveOrUpdate(toSave);
        return ResponseEntity.ok(saved);
    }
}

