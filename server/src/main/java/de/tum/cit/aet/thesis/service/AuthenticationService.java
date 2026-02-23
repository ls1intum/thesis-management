package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.entity.NotificationSetting;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.UserGroup;
import de.tum.cit.aet.thesis.entity.key.NotificationSettingId;
import de.tum.cit.aet.thesis.entity.key.UserGroupId;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.NotificationSettingRepository;
import de.tum.cit.aet.thesis.repository.UserGroupRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Manages user authentication, profile synchronization, and notification settings based on JWT tokens. */
@Service
public class AuthenticationService {
	private final UserRepository userRepository;
	private final UserGroupRepository userGroupRepository;
	private final UploadService uploadService;
	private final NotificationSettingRepository notificationSettingRepository;

	/**
	 * Injects the user, user group, upload, and notification setting repositories.
	 *
	 * @param userRepository the user repository
	 * @param userGroupRepository the user group repository
	 * @param uploadService the upload service for file storage
	 * @param notificationSettingRepository the notification setting repository
	 */
	@Autowired
	public AuthenticationService(UserRepository userRepository, UserGroupRepository userGroupRepository, UploadService uploadService, NotificationSettingRepository notificationSettingRepository) {
		this.userRepository = userRepository;
		this.userGroupRepository = userGroupRepository;
		this.uploadService = uploadService;
		this.notificationSettingRepository = notificationSettingRepository;
	}

	/**
	 * Returns the authenticated user identified by the university ID in the JWT.
	 *
	 * @param jwt the JWT authentication token
	 * @return the authenticated user
	 */
	public User getAuthenticatedUser(JwtAuthenticationToken jwt) {
		return userRepository.findByUniversityId(getUniversityId(jwt))
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
	}

	/**
	 * Returns the authenticated user with their research group eagerly loaded.
	 *
	 * @param jwt the JWT authentication token
	 * @return the authenticated user with research group loaded
	 */
	public User getAuthenticatedUserWithResearchGroup(JwtAuthenticationToken jwt) {
		return userRepository.findByUniversityIdWithResearchGroup(getUniversityId(jwt))
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public User updateAuthenticatedUser(JwtAuthenticationToken jwt) {
		Map<String, Object> attributes = jwt.getTokenAttributes();
		String universityId = getUniversityId(jwt);

		String email = (String) attributes.get("email");
		String firstName = (String) attributes.get("given_name");
		String lastName = (String) attributes.get("family_name");
		String matriculationNumber = (String) attributes.get("matrikelnr");

		List<String> groups = jwt.getAuthorities().stream()
				.filter(authority -> authority.getAuthority().startsWith("ROLE_"))
				.map(authority -> authority.getAuthority().replace("ROLE_", "")).toList();

		User user = userRepository.findByUniversityId(universityId).orElseGet(() -> {
			User newUser = new User();
			Instant currentTime = Instant.now();

			newUser.setJoinedAt(currentTime);
			newUser.setUpdatedAt(currentTime);

			return newUser;
		});

		user.setUniversityId(universityId);

		if (email != null && !email.isEmpty()) {
			user.setEmail(email);
		}

		if (firstName != null && !firstName.isEmpty()) {
			user.setFirstName(firstName);
		}

		if (lastName != null && !lastName.isEmpty()) {
			user.setLastName(lastName);
		}

		if (matriculationNumber != null && !matriculationNumber.isEmpty()) {
			user.setMatriculationNumber(matriculationNumber);
		}

		user.setLastLoginAt(Instant.now());

		if (user.isDisabled()) {
			user.setDisabled(false);
		}

		user = userRepository.save(user);

		userGroupRepository.deleteByUserId(user.getId());

		Set<UserGroup> userGroups = new HashSet<>();

		for (String group : groups) {
			UserGroup entity = new UserGroup();
			UserGroupId entityId = new UserGroupId();

			entityId.setUserId(user.getId());
			entityId.setGroup(group);

			entity.setUser(user);
			entity.setId(entityId);

			userGroups.add(userGroupRepository.save(entity));
		}

		user.setGroups(userGroups);

		return userRepository.save(user);
	}

	/**
	 * Updates the user's profile information, including personal details and uploaded documents.
	 *
	 * @param user the user to update
	 * @param firstName the user's first name
	 * @param lastName the user's last name
	 * @param gender the user's gender
	 * @param nationality the user's nationality
	 * @param email the user's email address
	 * @param studyDegree the user's study degree
	 * @param studyProgram the user's study program
	 * @param enrolledAt the enrollment date
	 * @param specialSkills the user's special skills
	 * @param interests the user's interests
	 * @param projects the user's projects
	 * @param customData additional custom data fields
	 * @param avatar the avatar image file
	 * @param examinationReport the examination report file
	 * @param cv the CV file
	 * @param degreeReport the degree report file
	 * @return the updated user
	 */
	public User updateUserInformation(
			User user,
			String firstName,
			String lastName,
			String gender,
			String nationality,
			String email,
			String studyDegree,
			String studyProgram,
			Instant enrolledAt,
			String specialSkills,
			String interests,
			String projects,
			Map<String, String> customData,
			MultipartFile avatar,
			MultipartFile examinationReport,
			MultipartFile cv,
			MultipartFile degreeReport
	) {
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setGender(gender);
		user.setNationality(nationality);
		user.setEmail(email);
		user.setStudyDegree(studyDegree);
		user.setStudyProgram(studyProgram);
		user.setEnrolledAt(enrolledAt);
		user.setSpecialSkills(specialSkills);
		user.setInterests(interests);
		user.setProjects(projects);
		user.setCustomData(customData);

		if (avatar != null) {
			user.setAvatar(avatar.isEmpty() ? null : uploadService.store(avatar, 1024 * 1024, UploadFileType.IMAGE));
		}

		user.setExaminationFilename(examinationReport == null ? null : uploadService.store(examinationReport, 3 * 1024 * 1024, UploadFileType.PDF));
		user.setCvFilename(cv == null ? null : uploadService.store(cv, 3 * 1024 * 1024, UploadFileType.PDF));
		user.setDegreeFilename(degreeReport == null ? null : uploadService.store(degreeReport, 3 * 1024 * 1024, UploadFileType.PDF));

		return userRepository.save(user);
	}

	/**
	 * Returns the notification settings for the given user.
	 *
	 * @param user the user whose notification settings to retrieve
	 * @return the list of notification settings
	 */
	public List<NotificationSetting> getNotificationSettings(User user) {
		return user.getNotificationSettings();
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public List<NotificationSetting> updateNotificationSettings(User user, String name, String email) {
		List<NotificationSetting> settings = user.getNotificationSettings();

		for (NotificationSetting setting : settings) {
			if (setting.getId().getName().equals(name)) {
				setting.setEmail(email);
				setting.setUpdatedAt(Instant.now());

				notificationSettingRepository.save(setting);

				return settings;
			}
		}

		NotificationSettingId entityId = new NotificationSettingId();
		entityId.setName(name);
		entityId.setUserId(user.getId());

		NotificationSetting entity = new NotificationSetting();
		entity.setId(entityId);
		entity.setUpdatedAt(Instant.now());
		entity.setEmail(email);
		entity.setUser(user);

		settings.add(notificationSettingRepository.save(entity));

		user.setNotificationSettings(settings);

		return settings;
	}

	private String getUniversityId(JwtAuthenticationToken jwt) {
		return jwt.getName();
	}
}
