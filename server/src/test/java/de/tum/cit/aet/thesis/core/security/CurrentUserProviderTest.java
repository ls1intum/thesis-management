package de.tum.cit.aet.thesis.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.tum.cit.aet.thesis.core.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.entity.UserGroup;
import de.tum.cit.aet.thesis.core.user.entity.key.UserGroupId;
import de.tum.cit.aet.thesis.core.user.service.AuthenticationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

	@Mock
	private AuthenticationService authenticationService;

	private CurrentUserProvider provider;

	private User user;
	private ResearchGroup researchGroup;
	private JwtAuthenticationToken jwtToken;

	@BeforeEach
	void setUp() {
		provider = new CurrentUserProvider(authenticationService);
		researchGroup = new ResearchGroup();
		researchGroup.setId(UUID.randomUUID());
		researchGroup.setArchived(false);
		user = new User();
		user.setId(UUID.randomUUID());
		user.setResearchGroup(researchGroup);
		Set<UserGroup> groups = new HashSet<>();
		user.setGroups(groups);
		Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).claim("sub", "user").build();
		jwtToken = new JwtAuthenticationToken(jwt);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private void setAuthInContext(org.springframework.security.core.Authentication auth) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
	}

	private void givenJwtAuthAndUser() {
		setAuthInContext(jwtToken);
		org.mockito.Mockito.lenient().when(authenticationService.getAuthenticatedUserWithResearchGroup(any())).thenReturn(user);
	}

	private void assignGroups(String... groupNames) {
		Set<UserGroup> userGroups = new HashSet<>();
		for (String g : groupNames) {
			UserGroupId id = new UserGroupId();
			id.setUserId(user.getId());
			id.setGroup(g);
			UserGroup ug = new UserGroup();
			ug.setId(id);
			ug.setUser(user);
			userGroups.add(ug);
		}
		user.setGroups(userGroups);
	}

	@Test
	void getUser_noJwt_throwsAccessDenied() {
		setAuthInContext(new UsernamePasswordAuthenticationToken("alice", "pw"));
		assertThrows(AccessDeniedException.class, () -> provider.getUser());
	}

	@Test
	void getUser_jwtAuth_returnsCachedUserAfterFirstCall() {
		givenJwtAuthAndUser();
		User firstCall = provider.getUser();
		User secondCall = provider.getUser();

		assertSame(user, firstCall);
		assertSame(firstCall, secondCall);
		verify(authenticationService, times(1)).getAuthenticatedUserWithResearchGroup(any());
	}

	@Test
	void getResearchGroupOrThrow_returnsUserGroup() {
		givenJwtAuthAndUser();
		assertSame(researchGroup, provider.getResearchGroupOrThrow());
	}

	@Test
	void getResearchGroupOrThrow_archivedGroup_throws() {
		researchGroup.setArchived(true);
		givenJwtAuthAndUser();
		assertThrows(AccessDeniedException.class, () -> provider.getResearchGroupOrThrow());
	}

	@Test
	void getResearchGroupOrThrow_noGroupNonPrivileged_throws() {
		user.setResearchGroup(null);
		assignGroups("advisor"); // not admin/student
		givenJwtAuthAndUser();
		assertThrows(AccessDeniedException.class, () -> provider.getResearchGroupOrThrow());
	}

	@Test
	void getResearchGroupOrThrow_noGroupButAdmin_returnsNull() {
		user.setResearchGroup(null);
		assignGroups("admin");
		givenJwtAuthAndUser();
		// admins are allowed to see all groups so a null user-group is acceptable
		assertEquals(null, provider.getResearchGroupOrThrow());
	}

	@Test
	void roleFlags_reflectGroupMembership() {
		assignGroups("student");
		givenJwtAuthAndUser();
		assertTrue(provider.isStudent());
		assertFalse(provider.isAdmin());
		assertFalse(provider.isSupervisor());
		assertFalse(provider.isExaminer());
		assertFalse(provider.isAnonymous());
		assertTrue(provider.canSeeAllResearchGroups());
	}

	@Test
	void anonymous_whenNoGroups() {
		givenJwtAuthAndUser();
		assertTrue(provider.isAnonymous());
		assertTrue(provider.canSeeAllResearchGroups());
	}

	@Test
	void supervisorAndExaminer_flagsMapToKeycloakGroups() {
		assignGroups("advisor", "supervisor");
		givenJwtAuthAndUser();
		assertTrue(provider.isSupervisor());
		assertTrue(provider.isExaminer());
		assertFalse(provider.canSeeAllResearchGroups());
	}

	@Test
	void assertCanAccessResearchGroup_archivedTarget_throws() {
		givenJwtAuthAndUser();
		ResearchGroup target = new ResearchGroup();
		target.setId(UUID.randomUUID());
		target.setArchived(true);
		assertThrows(AccessDeniedException.class, () -> provider.assertCanAccessResearchGroup(target));
	}

	@Test
	void assertCanAccessResearchGroup_adminAlwaysOk() {
		assignGroups("admin");
		givenJwtAuthAndUser();
		ResearchGroup target = new ResearchGroup();
		target.setId(UUID.randomUUID());
		target.setArchived(false);
		provider.assertCanAccessResearchGroup(target);
	}

	@Test
	void assertCanAccessResearchGroup_sameGroup_ok() {
		assignGroups("advisor");
		givenJwtAuthAndUser();
		provider.assertCanAccessResearchGroup(researchGroup);
	}

	@Test
	void assertCanAccessResearchGroup_differentGroup_throws() {
		assignGroups("advisor");
		givenJwtAuthAndUser();
		ResearchGroup other = new ResearchGroup();
		other.setId(UUID.randomUUID());
		other.setArchived(false);
		assertThrows(AccessDeniedException.class, () -> provider.assertCanAccessResearchGroup(other));
	}

	@Test
	void assertCanAccessResearchGroup_nullTargetForNonPrivilegedUser_throws() {
		assignGroups("advisor");
		givenJwtAuthAndUser();
		assertThrows(AccessDeniedException.class, () -> provider.assertCanAccessResearchGroup(null));
	}

	@Test
	void assertSameResearchGroupIfNotPrivileged_studentDoesNotCheck() {
		assignGroups("student");
		givenJwtAuthAndUser();
		ResearchGroup other = new ResearchGroup();
		other.setId(UUID.randomUUID());
		// Should not throw for student even with mismatched group
		provider.assertSameResearchGroupIfNotPrivileged(other);
	}
}
