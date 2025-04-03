package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.service.ResearchGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/research-groups")
@RequiredArgsConstructor
public class ResearchGroupController {

    private final ResearchGroupService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<ResearchGroup> getById(@PathVariable UUID id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}