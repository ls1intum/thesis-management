package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.StringLimits;
import de.tum.cit.aet.thesis.controller.payload.UpdateNotificationSettingPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateUserInformationPayload;
import de.tum.cit.aet.thesis.dto.NotificationSettingDto;
import de.tum.cit.aet.thesis.dto.UserDto;
import de.tum.cit.aet.thesis.entity.NotificationSetting;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.AuthenticationService;
import de.tum.cit.aet.thesis.service.GravatarService;
import de.tum.cit.aet.thesis.service.UploadService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/** REST controller for managing the authenticated user's profile and notification settings. */
@Slf4j
@RestController
@RequestMapping("/v2/user-info")
public class UserInfoController {
	private final AuthenticationService authenticationService;
	private final UserRepository userRepository;
	private final UploadService uploadService;
	private final GravatarService gravatarService;

	/**
	 * Constructs a new UserInfoController with the required dependencies.
	 *
	 * @param authenticationService the authentication service
	 * @param userRepository the user repository
	 * @param uploadService the upload service
	 * @param gravatarService the gravatar service
	 */
	@Autowired
	public UserInfoController(AuthenticationService authenticationService, UserRepository userRepository, UploadService uploadService, GravatarService gravatarService) {
		this.authenticationService = authenticationService;
		this.userRepository = userRepository;
		this.uploadService = uploadService;
		this.gravatarService = gravatarService;
	}

	/**
	 * Retrieves the profile information of the authenticated user.
	 *
	 * @param jwt the JWT authentication token
	 * @return the authenticated user's profile information
	 */
	@GetMapping
	public ResponseEntity<UserDto> getInfo(JwtAuthenticationToken jwt) {
		User user = this.authenticationService.updateAuthenticatedUser(jwt);

		return ResponseEntity.ok(UserDto.fromUserEntity(user));
	}

	/**
	 * Updates the authenticated user's profile information and uploaded documents.
	 *
	 * @param payload the payload containing updated user information
	 * @param examinationReport the examination report file
	 * @param cv the CV file
	 * @param degreeReport the degree report file
	 * @param avatar the avatar image file
	 * @param jwt the JWT authentication token
	 * @return the updated user profile information
	 */
	@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UserDto> updateInfo(
			@RequestPart("data") UpdateUserInformationPayload payload,
			@RequestPart(value = "examinationReport", required = false) MultipartFile examinationReport,
			@RequestPart(value = "cv", required = false) MultipartFile cv,
			@RequestPart(value = "degreeReport", required = false) MultipartFile degreeReport,
			@RequestPart(value = "avatar", required = false) MultipartFile avatar,
			JwtAuthenticationToken jwt
	) {
		User authenticatedUser = this.authenticationService.getAuthenticatedUser(jwt);

		authenticatedUser = this.authenticationService.updateUserInformation(
				authenticatedUser,
				RequestValidator.validateStringMaxLengthAllowNull(payload.firstName(), StringLimits.SHORTTEXT.getLimit()),
				RequestValidator.validateStringMaxLengthAllowNull(payload.lastName(), StringLimits.SHORTTEXT.getLimit()),
				RequestValidator.validateStringMaxLengthAllowNull(payload.gender(), StringLimits.SHORTTEXT.getLimit()),
				RequestValidator.validateStringMaxLengthAllowNull(payload.nationality(), StringLimits.SHORTTEXT.getLimit()),
				RequestValidator.validateEmailAllowNull(payload.email()),
				RequestValidator.validateStringMaxLengthAllowNull(payload.studyDegree(), StringLimits.SHORTTEXT.getLimit()),
				RequestValidator.validateStringMaxLengthAllowNull(payload.studyProgram(), StringLimits.SHORTTEXT.getLimit()),
				payload.enrolledAt(),
				RequestValidator.validateStringMaxLengthAllowNull(payload.specialSkills(), StringLimits.LONGTEXT.getLimit()),
				RequestValidator.validateStringMaxLengthAllowNull(payload.interests(), StringLimits.LONGTEXT.getLimit()),
				RequestValidator.validateStringMaxLengthAllowNull(payload.projects(), StringLimits.LONGTEXT.getLimit()),
				RequestValidator.validateNotNull(payload.customData()),
				avatar,
				examinationReport,
				cv,
				degreeReport
		);

		return ResponseEntity.ok(UserDto.fromUserEntity(authenticatedUser));
	}

	/**
	 * Retrieves the notification settings of the authenticated user.
	 *
	 * @param jwt the JWT authentication token
	 * @return the list of notification settings
	 */
	@GetMapping("/notifications")
	public ResponseEntity<List<NotificationSettingDto>> getNotificationSettings(JwtAuthenticationToken jwt) {
		User user = this.authenticationService.getAuthenticatedUser(jwt);

		List<NotificationSetting> settings = authenticationService.getNotificationSettings(user);

		return ResponseEntity.ok(
				settings.stream().map(NotificationSettingDto::fromNotificationSettingEntity).toList()
		);
	}

	/**
	 * Updates the notification settings of the authenticated user.
	 *
	 * @param payload the payload containing notification setting updates
	 * @param jwt the JWT authentication token
	 * @return the updated list of notification settings
	 */
	@PutMapping("/notifications")
	public ResponseEntity<List<NotificationSettingDto>> updateNotificationSettings(
			@RequestBody UpdateNotificationSettingPayload payload,
			JwtAuthenticationToken jwt
	) {
		User user = this.authenticationService.getAuthenticatedUser(jwt);

		List<NotificationSetting> settings = authenticationService.updateNotificationSettings(
				user,
				RequestValidator.validateStringMaxLength(payload.name(), StringLimits.SHORTTEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.email(), StringLimits.SHORTTEXT.getLimit())
		);

		return ResponseEntity.ok(
				settings.stream().map(NotificationSettingDto::fromNotificationSettingEntity).toList()
		);
	}

	/**
	 * Imports the authenticated user's profile picture from an external avatar service.
	 * The request is made server-side so that the user's IP address is not exposed to the external service.
	 *
	 * @param jwt the JWT authentication token
	 * @return the updated user profile information
	 */
	@PostMapping("/import-profile-picture")
	public ResponseEntity<UserDto> importProfilePicture(JwtAuthenticationToken jwt) {
		User user = this.authenticationService.getAuthenticatedUser(jwt);

		String email = user.getEmail() != null ? user.getEmail().getAddress() : null;
		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		Optional<byte[]> imageBytes = gravatarService.fetchProfilePicture(email);
		if (imageBytes.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		String oldAvatar = user.getAvatar();
		String storedFilename = uploadService.storeBytes(imageBytes.get(), "png", 1024 * 1024);
		user.setAvatar(storedFilename);
		user = userRepository.save(user);

		if (oldAvatar != null && !oldAvatar.equals(storedFilename)) {
			uploadService.deleteFile(oldAvatar);
		}

		return ResponseEntity.ok(UserDto.fromUserEntity(user));
	}
}
