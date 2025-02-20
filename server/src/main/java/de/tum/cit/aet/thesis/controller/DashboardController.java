package de.tum.cit.aet.thesis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import de.tum.cit.aet.thesis.dto.TaskDto;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.AuthenticationService;
import de.tum.cit.aet.thesis.service.DashboardService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v2/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final AuthenticationService authenticationService;

    @Autowired
    public DashboardController(DashboardService dashboardService, AuthenticationService authenticationService) {
        this.dashboardService = dashboardService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskDto>> getTasks(JwtAuthenticationToken jwt) {
        User authenticatedUser = authenticationService.getAuthenticatedUser(jwt);

        return ResponseEntity.ok(dashboardService.getTasks(authenticatedUser));
    }
}
