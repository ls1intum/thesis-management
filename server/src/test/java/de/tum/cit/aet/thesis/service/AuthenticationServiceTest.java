package de.tum.cit.aet.thesis.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.entity.NotificationSetting;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.key.NotificationSettingId;
import de.tum.cit.aet.thesis.mock.EntityMockFactory;
import de.tum.cit.aet.thesis.repository.NotificationSettingRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
	@Mock
	private UserRepository userRepository;

	@Mock
	private UploadService uploadService;

	@Mock
	private NotificationSettingRepository notificationSettingRepository;

	@Mock
	private AccessManagementService accessManagementService;

	@Mock
	private JwtAuthenticationToken jwtToken;

	private AuthenticationService authenticationService;
	private User testUser;

	@BeforeEach
	void setUp() {
		authenticationService = new AuthenticationService(
				userRepository,
				uploadService,
				notificationSettingRepository,
				accessManagementService
		);

		testUser = EntityMockFactory.createUser("Test User");
	}

	@Test
	void updateUserInformation_WithAllFields_UpdatesUser() {
		MockMultipartFile avatar = new MockMultipartFile(
				"avatar",
				"avatar.jpg",
				"image/jpeg",
				"test".getBytes()
		);

		when(uploadService.store(any(), any(), any(UploadFileType.class))).thenReturn("stored-file");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authenticationService.updateUserInformation(
				testUser,
				"Updated",
				"User",
				"Male",
				"German",
				"updated@test.com",
				"Bachelor",
				"Computer Science",
				Instant.now(),
				"Java",
				"AI",
				"Thesis Management",
				Map.of("key", "value"),
				avatar,
				null,
				null,
				null
		);

		assertNotNull(result);
		verify(uploadService).store(any(), any(), eq(UploadFileType.IMAGE));
		verify(userRepository).save(any(User.class));
	}

	@Test
	void getNotificationSettings_ReturnsSettings() {
		List<NotificationSetting> settings = new ArrayList<>();
		NotificationSetting setting = new NotificationSetting();
		NotificationSettingId id = new NotificationSettingId();
		id.setName("test-notification");
		id.setUserId(testUser.getId());
		setting.setId(id);
		settings.add(setting);
		testUser.setNotificationSettings(settings);

		List<NotificationSetting> result = authenticationService.getNotificationSettings(testUser);

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	void updateNotificationSettings_WithNewSetting_CreatesSettings() {
		String settingName = "new-notification";
		String email = "yes";
		testUser.setNotificationSettings(new ArrayList<>());

		when(notificationSettingRepository.save(any(NotificationSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<NotificationSetting> result = authenticationService.updateNotificationSettings(
				testUser,
				settingName,
				email
		);

		assertNotNull(result);
		assertEquals(1, result.size());
		verify(notificationSettingRepository).save(any(NotificationSetting.class));
	}

	@Test
	void updateNotificationSettings_WithExistingSetting_UpdatesSettings() {
		String settingName = "existing-notification";
		String email = "yes";

		when(notificationSettingRepository.save(any(NotificationSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<NotificationSetting> result = authenticationService.updateNotificationSettings(
				testUser,
				settingName,
				email
		);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(email, result.getFirst().getEmail());
		verify(notificationSettingRepository).save(any(NotificationSetting.class));
	}

	private void setupJwtMock(String universityId, Map<String, Object> attributes) {
		when(jwtToken.getName()).thenReturn(universityId);
		when(jwtToken.getTokenAttributes()).thenReturn(attributes);
	}

	@Test
	void updateAuthenticatedUser_WithMatriculationNumber_SetsMatriculationNumber() {
		String universityId = "ab12cde";
		testUser.setUniversityId(universityId);
		testUser.setMatriculationNumber(null);

		setupJwtMock(universityId, Map.of(
				"email", "test@example.com",
				"given_name", "Test",
				"family_name", "User",
				"matrikelnr", "12345678"
		));
		when(userRepository.findByUniversityId(universityId)).thenReturn(Optional.of(testUser));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authenticationService.updateAuthenticatedUser(jwtToken);

		assertEquals("12345678", result.getMatriculationNumber());
	}

	@Test
	void updateAuthenticatedUser_WithoutMatriculationNumber_DoesNotOverwriteExisting() {
		String universityId = "ab12cde";
		testUser.setUniversityId(universityId);
		testUser.setMatriculationNumber("existing123");

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("email", "test@example.com");
		attributes.put("given_name", "Test");
		attributes.put("family_name", "User");
		// matrikelnr not present in JWT

		setupJwtMock(universityId, attributes);
		when(userRepository.findByUniversityId(universityId)).thenReturn(Optional.of(testUser));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authenticationService.updateAuthenticatedUser(jwtToken);

		assertEquals("existing123", result.getMatriculationNumber());
	}

	@Test
	void updateAuthenticatedUser_WithEmptyMatriculationNumber_DoesNotOverwriteExisting() {
		String universityId = "ab12cde";
		testUser.setUniversityId(universityId);
		testUser.setMatriculationNumber("existing123");

		setupJwtMock(universityId, Map.of(
				"email", "test@example.com",
				"given_name", "Test",
				"family_name", "User",
				"matrikelnr", ""
		));
		when(userRepository.findByUniversityId(universityId)).thenReturn(Optional.of(testUser));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authenticationService.updateAuthenticatedUser(jwtToken);

		assertEquals("existing123", result.getMatriculationNumber());
	}

	@Test
	void updateAuthenticatedUser_WithNewUser_SetsMatriculationNumber() {
		String universityId = "new12abc";

		setupJwtMock(universityId, Map.of(
				"email", "new@example.com",
				"given_name", "New",
				"family_name", "Student",
				"matrikelnr", "99887766"
		));
		when(userRepository.findByUniversityId(universityId)).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authenticationService.updateAuthenticatedUser(jwtToken);

		assertEquals("99887766", result.getMatriculationNumber());
		assertEquals("New", result.getFirstName());
		assertEquals("Student", result.getLastName());
	}
}
