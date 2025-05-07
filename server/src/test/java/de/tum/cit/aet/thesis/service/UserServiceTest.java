package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.mock.EntityMockFactory;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

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
}