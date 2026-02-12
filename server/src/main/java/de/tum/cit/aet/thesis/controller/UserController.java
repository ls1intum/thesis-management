package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.dto.KeycloakUserDto;
import de.tum.cit.aet.thesis.dto.LightUserDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.service.AccessManagementService;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;
import de.tum.cit.aet.thesis.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** REST controller for managing users and retrieving user documents. */
@Slf4j
@RestController
@RequestMapping("/v2/users")
public class UserController {
	private final UserService userService;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	private final AccessManagementService accessManagementService;

	/**
	 * Injects the user service, current user provider, and access management service.
	 *
	 * @param userService the user service
	 * @param currentUserProviderProvider the current user provider
	 * @param accessManagementService the access management service
	 */
	@Autowired
	public UserController(UserService userService,
		ObjectProvider<CurrentUserProvider> currentUserProviderProvider, AccessManagementService accessManagementService) {
		this.userService = userService;
		this.currentUserProviderProvider = currentUserProviderProvider;
		this.accessManagementService = accessManagementService;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Retrieves a paginated list of users with optional search and group filtering.
	 *
	 * @param searchQuery the search query to filter users
	 * @param groups the groups to filter by
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction
	 * @return the paginated list of users
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<PaginationDto<LightUserDto>> getUsers(
			@RequestParam(required = false) String searchQuery,
			@RequestParam(required = false) String[] groups,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "joinedAt") String sortBy,
			@RequestParam(required = false, defaultValue = "desc") String sortOrder
	) {
		Page<User> users = userService.getAll(searchQuery, groups, page, limit, sortBy, sortOrder);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(users.map(LightUserDto::fromUserEntity)));
	}

	/**
	 * Searches for users in Keycloak and returns them with their local account status.
	 *
	 * @param searchKey the search key to filter Keycloak users
	 * @return the list of Keycloak users with local account status
	 */
	@GetMapping("/keycloak")
	@PreAuthorize("hasAnyRole('admin', 'group-admin')")
	public ResponseEntity<List<KeycloakUserDto>> getKeycloakUsers(
			@RequestParam(required = false, defaultValue = "") String searchKey
	) {
		List<KeycloakUserInformation> keycloakUserInformation = accessManagementService.getAllUsers(searchKey);

		List<String> universityIds = keycloakUserInformation.stream()
				.map(KeycloakUserInformation::username)
				.toList();

		List<User> existingUsers = userService.findAllByUniversityIdIn(universityIds);

		Map<String, User> userLookup = existingUsers.stream()
				.collect(Collectors.toMap(User::getUniversityId, Function.identity()));

		List<KeycloakUserDto> users = keycloakUserInformation.stream()
				.map(user -> KeycloakUserDto.from(user, userLookup.get(user.username())))
				.toList();

		return ResponseEntity.ok(users);
	}

	/**
	 * Downloads the examination report PDF for a user.
	 *
	 * @param userId the ID of the user
	 * @return the examination report PDF as a resource
	 */
	@GetMapping("/{userId}/examination-report")
	public ResponseEntity<Resource> getExaminationReport(@PathVariable UUID userId) {
		User user = userService.findById(userId);

		if (!user.hasFullAccess(currentUserProvider().getUser())) {
			throw new AccessDeniedException("You are not allowed to access data from this user");
		}

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=examination_report_%s.pdf", userId))
				.body(userService.getExaminationReport(user));
	}

	/**
	 * Downloads the CV PDF for a user.
	 *
	 * @param userId the ID of the user
	 * @return the CV PDF as a resource
	 */
	@GetMapping("/{userId}/cv")
	public ResponseEntity<Resource> getCV(@PathVariable UUID userId) {
		User user = userService.findById(userId);

		if (!user.hasFullAccess(currentUserProvider().getUser())) {
			throw new AccessDeniedException("You are not allowed to access data from this user");
		}

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=cv_%s.pdf", userId))
				.body(userService.getCV(user));
	}

	/**
	 * Downloads the degree report PDF for a user.
	 *
	 * @param userId the ID of the user
	 * @return the degree report PDF as a resource
	 */
	@GetMapping("/{userId}/degree-report")
	public ResponseEntity<Resource> getDegreeReport(@PathVariable UUID userId) {
		User user = userService.findById(userId);

		if (!user.hasFullAccess(currentUserProvider().getUser())) {
			throw new AccessDeniedException("You are not allowed to access data from this user");
		}

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=degree_report_%s.pdf", userId))
				.body(userService.getDegreeReport(user));
	}
}
