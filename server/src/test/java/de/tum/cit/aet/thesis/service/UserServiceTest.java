package de.tum.cit.aet.thesis.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.mock.EntityMockFactory;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
	@Mock
	private UserRepository userRepository;

	@Mock
	private UploadService uploadService;

	@Mock
	private AccessManagementService accessManagementService;

	@Mock
	private ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	@Mock
	private CurrentUserProvider currentUserProvider;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = EntityMockFactory.createUser("Test");
	}

	@Test
	void getAll_WithNoFilters_ReturnsAllUsers() {
		List<User> users = Collections.singletonList(testUser);
		Page<User> expectedPage = new PageImpl<>(users);
		when(userRepository.searchUsers(
				any(),
				any(),
				any(),
				any(PageRequest.class)
		)).thenReturn(expectedPage);
		when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
		when(currentUserProvider.getResearchGroupOrThrow()).thenReturn(testUser.getResearchGroup());

		Page<User> result = userService.getAll(
				null,
				null,
				0,
				10,
				"id",
				"asc"
		);

		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		assertEquals(testUser, result.getContent().getFirst());
		verify(userRepository).searchUsers(
				any(),
				any(),
				any(),
				eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")))
		);
	}

	@Test
	void findById_WithExistingUser_ReturnsUser() {
		when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

		User result = userService.findById(testUser.getId());

		assertNotNull(result);
		assertEquals(testUser.getId(), result.getId());
		assertEquals(testUser.getFirstName(), result.getFirstName());
		assertEquals(testUser.getLastName(), result.getLastName());
		assertEquals(testUser.getEmail(), result.getEmail());
		verify(userRepository).findById(testUser.getId());
	}

	@Test
	void findById_WithNonExistingUser_ThrowsResourceNotFoundException() {
		when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () ->
				userService.findById(testUser.getId())
		);
		verify(userRepository).findById(testUser.getId());
	}

	@Test
	void findOrCreateByUniversityId_WithExistingUser_ReturnsLocalUser() {
		testUser.setUniversityId("ab12cde");
		when(userRepository.findByUniversityId("ab12cde")).thenReturn(Optional.of(testUser));

		User result = userService.findOrCreateByUniversityId("ab12cde");

		assertSame(testUser, result);
		verify(userRepository).findByUniversityId("ab12cde");
		org.mockito.Mockito.verifyNoInteractions(accessManagementService);
		org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(any());
	}

	@Test
	void findOrCreateByUniversityId_WithMissingUser_FetchesFromKeycloakAndPersists() {
		when(userRepository.findByUniversityId("ab12cde")).thenReturn(Optional.empty());
		KeycloakUserInformation keycloakUser = new KeycloakUserInformation(
				UUID.randomUUID(),
				"ab12cde",
				"Ada",
				"Lovelace",
				"ada@example.com",
				Map.of("matrikelnr", List.of("01234567"))
		);
		when(accessManagementService.getUserByUsername("ab12cde")).thenReturn(keycloakUser);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = userService.findOrCreateByUniversityId("ab12cde");

		assertNotNull(result);
		assertEquals("ab12cde", result.getUniversityId());
		assertEquals("Ada", result.getFirstName());
		assertEquals("Lovelace", result.getLastName());
		assertEquals("ada@example.com", result.getEmail().getAddress());
		assertEquals("01234567", result.getMatriculationNumber());
		assertNotNull(result.getJoinedAt());
		assertNotNull(result.getUpdatedAt());
		verify(userRepository).save(any(User.class));
	}

	@Test
	void findOrCreateByUniversityId_OnConcurrentCreate_RecoversByReFetching() {
		KeycloakUserInformation keycloakUser = new KeycloakUserInformation(
				UUID.randomUUID(), "ab12cde", "Ada", "Lovelace", "ada@example.com", Map.of()
		);
		User concurrentlyCreated = EntityMockFactory.createUser("Concurrent");
		concurrentlyCreated.setUniversityId("ab12cde");

		when(userRepository.findByUniversityId("ab12cde"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(concurrentlyCreated));
		when(accessManagementService.getUserByUsername("ab12cde")).thenReturn(keycloakUser);
		when(userRepository.save(any(User.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate universityId"));

		User result = userService.findOrCreateByUniversityId("ab12cde");

		assertSame(concurrentlyCreated, result);
		verify(userRepository, org.mockito.Mockito.times(2)).findByUniversityId("ab12cde");
	}
}
