package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.service.AccessManagementService;
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
import org.springframework.web.bind.annotation.*;
import de.tum.cit.aet.thesis.dto.LightUserDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.UserService;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v2/users")
public class UserController {
    private final UserService userService;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

    private final AccessManagementService accessManagementService;

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

    public record KeycloakUserElement(UUID id, String username, String firstName, String lastName , String email, boolean hasResearchGroup) {}
    @GetMapping("/keycloak")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<KeycloakUserElement>> getKeycloakUsers(
            @RequestParam(required = false, defaultValue = "") String searchKey
    ) {
        List<KeycloakUserInformation> keycloakUserInformation = accessManagementService.getAllUsers(searchKey);

        //Check if user exists in our System, if yes check if he has a research group and set the flag
        List<KeycloakUserElement> users = keycloakUserInformation.stream()
                .map(user -> {
                    boolean hasResearchGroup = false;

                    try {
                        User systemUser = userService.findByUniversityId(user.username());
                        hasResearchGroup = systemUser.getResearchGroup() != null;
                    } catch (ResourceNotFoundException e) {
                        // User not found — keep flags as false
                    }

                    return new KeycloakUserElement(
                            user.id(),
                            user.username(),
                            user.firstName(),
                            user.lastName(),
                            user.email(),
                            hasResearchGroup
                    );
                })
                .toList();

        return ResponseEntity.ok(users);
    }

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
