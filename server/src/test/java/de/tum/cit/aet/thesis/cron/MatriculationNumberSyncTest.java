package de.tum.cit.aet.thesis.cron;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.mock.EntityMockFactory;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.AccessManagementService;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MatriculationNumberSyncTest {
	@Mock
	private UserRepository userRepository;

	@Mock
	private AccessManagementService accessManagementService;

	private MatriculationNumberSync matriculationNumberSync;

	@BeforeEach
	void setUp() {
		matriculationNumberSync = new MatriculationNumberSync(userRepository, accessManagementService);
	}

	@Test
	void syncMatriculationNumbers_WithNoUsersWithoutMatriculationNumber_DoesNothing() {
		when(userRepository.findIdsWithoutMatriculationNumber()).thenReturn(List.of());

		matriculationNumberSync.syncMatriculationNumbers();

		verify(userRepository).findIdsWithoutMatriculationNumber();
		verify(accessManagementService, never()).getUserByUsername(any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void syncMatriculationNumbers_WithKeycloakMatriculationNumber_UpdatesUser() {
		User user = EntityMockFactory.createUser("Student");
		user.setUniversityId("ab12cde");
		user.setMatriculationNumber(null);

		when(userRepository.findIdsWithoutMatriculationNumber()).thenReturn(List.of(user.getId()));
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(accessManagementService.getUserByUsername("ab12cde")).thenReturn(
				new KeycloakUserInformation(UUID.randomUUID(), "ab12cde", "Student", "Student", "student@test.com",
						Map.of("matrikelnr", List.of("12345678")))
		);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		matriculationNumberSync.syncMatriculationNumbers();

		assertEquals("12345678", user.getMatriculationNumber());
		verify(userRepository).save(user);
	}

	@Test
	void syncMatriculationNumbers_WithoutKeycloakMatriculationNumber_DoesNotUpdateUser() {
		User user = EntityMockFactory.createUser("Professor");
		user.setUniversityId("xy99abc");
		user.setMatriculationNumber(null);

		when(userRepository.findIdsWithoutMatriculationNumber()).thenReturn(List.of(user.getId()));
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(accessManagementService.getUserByUsername("xy99abc")).thenReturn(
				new KeycloakUserInformation(UUID.randomUUID(), "xy99abc", "Prof", "Test", "prof@test.com", null)
		);

		matriculationNumberSync.syncMatriculationNumbers();

		assertNull(user.getMatriculationNumber());
		verify(userRepository, never()).save(any());
	}

	@Test
	void syncMatriculationNumbers_WithKeycloakError_SkipsUserAndContinues() {
		User user1 = EntityMockFactory.createUser("FailUser");
		user1.setUniversityId("fail01");
		user1.setMatriculationNumber(null);

		User user2 = EntityMockFactory.createUser("SuccessUser");
		user2.setUniversityId("success01");
		user2.setMatriculationNumber(null);

		when(userRepository.findIdsWithoutMatriculationNumber()).thenReturn(List.of(user1.getId(), user2.getId()));
		when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
		when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
		when(accessManagementService.getUserByUsername("fail01")).thenThrow(new RuntimeException("Keycloak unavailable"));
		when(accessManagementService.getUserByUsername("success01")).thenReturn(
				new KeycloakUserInformation(UUID.randomUUID(), "success01", "Success", "User", "success@test.com",
						Map.of("matrikelnr", List.of("87654321")))
		);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		matriculationNumberSync.syncMatriculationNumbers();

		assertNull(user1.getMatriculationNumber());
		assertEquals("87654321", user2.getMatriculationNumber());
		verify(userRepository).save(user2);
		verify(userRepository, times(1)).save(any());
	}

	@Test
	void syncMatriculationNumbers_WithEmptyMatriculationNumber_DoesNotUpdateUser() {
		User user = EntityMockFactory.createUser("Student");
		user.setUniversityId("ab12xyz");
		user.setMatriculationNumber(null);

		when(userRepository.findIdsWithoutMatriculationNumber()).thenReturn(List.of(user.getId()));
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(accessManagementService.getUserByUsername("ab12xyz")).thenReturn(
				new KeycloakUserInformation(UUID.randomUUID(), "ab12xyz", "Student", "Test", "student@test.com",
						Map.of("matrikelnr", List.of("")))
		);

		matriculationNumberSync.syncMatriculationNumbers();

		assertNull(user.getMatriculationNumber());
		verify(userRepository, never()).save(any());
	}

	@Test
	void syncSingleUser_WithNonExistentUser_ReturnsFalse() {
		UUID userId = UUID.randomUUID();
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		boolean result = matriculationNumberSync.syncSingleUser(userId);

		assertFalse(result);
		verify(accessManagementService, never()).getUserByUsername(any());
	}

	@Test
	void syncSingleUser_WithExistingMatriculationNumber_ReturnsFalse() {
		User user = EntityMockFactory.createUser("Student");
		user.setMatriculationNumber("12345678");

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		boolean result = matriculationNumberSync.syncSingleUser(user.getId());

		assertFalse(result);
		verify(accessManagementService, never()).getUserByUsername(any());
	}

	@Test
	void syncSingleUser_WithKeycloakMatriculationNumber_UpdatesAndReturnsTrue() {
		User user = EntityMockFactory.createUser("Student");
		user.setUniversityId("ab12cde");
		user.setMatriculationNumber(null);

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(accessManagementService.getUserByUsername("ab12cde")).thenReturn(
				new KeycloakUserInformation(UUID.randomUUID(), "ab12cde", "Student", "Student", "student@test.com",
						Map.of("matrikelnr", List.of("12345678")))
		);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		boolean result = matriculationNumberSync.syncSingleUser(user.getId());

		assertTrue(result);
		assertEquals("12345678", user.getMatriculationNumber());
		verify(userRepository).save(user);
	}

	@Test
	void syncMatriculationNumbers_WithMultipleUsers_UpdatesAllWithMatriculationNumber() {
		User student1 = EntityMockFactory.createUser("Student1");
		student1.setUniversityId("st01abc");
		student1.setMatriculationNumber(null);

		User student2 = EntityMockFactory.createUser("Student2");
		student2.setUniversityId("st02def");
		student2.setMatriculationNumber(null);

		User professor = EntityMockFactory.createUser("Professor");
		professor.setUniversityId("pr01ghi");
		professor.setMatriculationNumber(null);

		when(userRepository.findIdsWithoutMatriculationNumber())
				.thenReturn(List.of(student1.getId(), student2.getId(), professor.getId()));
		when(userRepository.findById(student1.getId())).thenReturn(Optional.of(student1));
		when(userRepository.findById(student2.getId())).thenReturn(Optional.of(student2));
		when(userRepository.findById(professor.getId())).thenReturn(Optional.of(professor));
		when(accessManagementService.getUserByUsername("st01abc")).thenReturn(
				new KeycloakUserInformation(UUID.randomUUID(), "st01abc", "Student1", "Test", "s1@test.com",
						Map.of("matrikelnr", List.of("11111111")))
		);
		when(accessManagementService.getUserByUsername("st02def")).thenReturn(
				new KeycloakUserInformation(UUID.randomUUID(), "st02def", "Student2", "Test", "s2@test.com",
						Map.of("matrikelnr", List.of("22222222")))
		);
		when(accessManagementService.getUserByUsername("pr01ghi")).thenReturn(
				new KeycloakUserInformation(UUID.randomUUID(), "pr01ghi", "Professor", "Test", "prof@test.com",
						Map.of())
		);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		matriculationNumberSync.syncMatriculationNumbers();

		assertEquals("11111111", student1.getMatriculationNumber());
		assertEquals("22222222", student2.getMatriculationNumber());
		assertNull(professor.getMatriculationNumber());
		verify(userRepository, times(2)).save(any());
	}
}
