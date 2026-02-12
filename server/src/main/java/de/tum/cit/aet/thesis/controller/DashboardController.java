package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.TaskDto;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST controller for retrieving dashboard data such as pending tasks. */
@Slf4j
@RestController
@RequestMapping("/v2/dashboard")
public class DashboardController {
	private final DashboardService dashboardService;
	private final CurrentUserProvider currentUserProvider;

	/**
	 * Injects the dashboard service and current user provider.
	 *
	 * @param dashboardService the dashboard service
	 * @param currentUserProvider the current user provider
	 */
	@Autowired
	public DashboardController(DashboardService dashboardService,
		CurrentUserProvider currentUserProvider) {
		this.dashboardService = dashboardService;
	this.currentUserProvider = currentUserProvider;
	}

	/**
	 * Retrieves the list of pending tasks for the authenticated user.
	 *
	 * @return the list of pending tasks
	 */
	@GetMapping("/tasks")
	public ResponseEntity<List<TaskDto>> getTasks() {
		return ResponseEntity.ok(dashboardService.getTasks(currentUserProvider.getUser()));
	}
}
