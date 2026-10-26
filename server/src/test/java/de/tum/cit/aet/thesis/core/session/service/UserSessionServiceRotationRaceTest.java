package de.tum.cit.aet.thesis.core.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.session.config.UserSessionProperties;
import de.tum.cit.aet.thesis.core.session.config.UserSessionProperties.UserAgentMismatchAction;
import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.entity.UserSessionEndReason;
import de.tum.cit.aet.thesis.core.session.repository.UserSessionRepository;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.AuthResult;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.ClientInfo;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Reason;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService.Rejected;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Rotation and logout are separate database operations. If the session is ended after the request claimed its
 * rotation but before the old row is marked rotated, the replacement must not stay alive and no cookie for it may
 * be handed out.
 */
class UserSessionServiceRotationRaceTest {
	private static final Instant NOW = Instant.parse("2026-03-02T09:00:00Z");
	private static final ClientInfo CLIENT = new ClientInfo("203.0.113.7", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Chrome/130.0.0.0 Safari/537.36");

	@Test
	void rotationThatLosesTheRaceAgainstALogout_endsItsReplacementAndHandsOutNoCookie() {
		UserSessionRepository repository = mock(UserSessionRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		UserSessionProperties properties = new UserSessionProperties(Duration.ofHours(4), Duration.ofHours(24), 10,
				Duration.ofMinutes(5), Duration.ofSeconds(30), Duration.ofDays(30), 20, UserAgentMismatchAction.END);
		UserSessionService service = new UserSessionService(repository, userRepository, properties, Clock.fixed(NOW, ZoneOffset.UTC));

		UserSessionTokens.Issued issued = UserSessionTokens.generate();
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setUniversityId("ga12abc");
		UserSession old = new UserSession();
		old.setId(UUID.randomUUID());
		old.setTokenIdentifier(issued.identifier());
		old.setVerifierHash(UserSessionTokens.hashVerifier(issued.verifier()));
		old.setUserId(user.getId());
		old.setCreatedAt(NOW.minusSeconds(60));
		old.setLastUsedAt(NOW.minusSeconds(60));
		old.setExpiresAt(NOW.plus(Duration.ofHours(23)));
		old.setCsrfToken("csrf");
		old.setUserAgentSignature(UserAgentSignature.of(CLIENT.userAgent()));
		old.setRotationRequired(true);

		when(repository.findByTokenIdentifier(issued.identifier())).thenReturn(Optional.of(old));
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(repository.claimRotation(old.getId())).thenReturn(1);
		when(repository.save(any(UserSession.class))).thenAnswer(invocation -> {
			UserSession saved = invocation.getArgument(0);
			saved.setId(UUID.randomUUID());
			return saved;
		});
		// the old session was ended by a logout in between: nothing is left to mark as rotated
		when(repository.markRotated(eq(old.getId()), any(), any(), any())).thenReturn(0);

		AuthResult result = service.authenticate(issued.cookieValue(), CLIENT);

		assertThat(result).isInstanceOf(Rejected.class);
		assertThat(((Rejected) result).reason()).isEqualTo(Reason.ENDED);
		ArgumentCaptor<UserSession> replacement = ArgumentCaptor.forClass(UserSession.class);
		verify(repository).save(replacement.capture());
		verify(repository).endById(eq(replacement.getValue().getId()), any(), eq(UserSessionEndReason.REVOKED));
		verify(repository, never()).touchLastUsed(any(), any(), any());
	}
}
