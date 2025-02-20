package de.tum.cit.aet.thesis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import de.tum.cit.aet.thesis.dto.LightUserDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.service.AuthenticationService;
import de.tum.cit.aet.thesis.service.UserService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v2/users")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Autowired
    public UserController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
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

    @GetMapping("/{userId}/examination-report")
    public ResponseEntity<Resource> getExaminationReport(@PathVariable UUID userId, JwtAuthenticationToken jwt) {
        User authenticatedUser = authenticationService.getAuthenticatedUser(jwt);
        User user = userService.findById(userId);

        if (!user.hasFullAccess(authenticatedUser)) {
            throw new AccessDeniedException("You are not allowed to access data from this user");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=examination_report_%s.pdf", userId))
                .body(userService.getExaminationReport(user));
    }

    @GetMapping("/{userId}/cv")
    public ResponseEntity<Resource> getCV(@PathVariable UUID userId, JwtAuthenticationToken jwt) {
        User authenticatedUser = authenticationService.getAuthenticatedUser(jwt);
        User user = userService.findById(userId);

        if (!user.hasFullAccess(authenticatedUser)) {
            throw new AccessDeniedException("You are not allowed to access data from this user");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=cv_%s.pdf", userId))
                .body(userService.getCV(user));
    }

    @GetMapping("/{userId}/degree-report")
    public ResponseEntity<Resource> getDegreeReport(@PathVariable UUID userId, JwtAuthenticationToken jwt) {
        User authenticatedUser = authenticationService.getAuthenticatedUser(jwt);
        User user = userService.findById(userId);

        if (!user.hasFullAccess(authenticatedUser)) {
            throw new AccessDeniedException("You are not allowed to access data from this user");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=degree_report_%s.pdf", userId))
                .body(userService.getDegreeReport(user));
    }
}
