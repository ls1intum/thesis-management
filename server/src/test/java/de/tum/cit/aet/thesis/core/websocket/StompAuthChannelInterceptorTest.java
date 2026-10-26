package de.tum.cit.aet.thesis.core.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.security.Principal;

/**
 * A STOMP session is authenticated by the session cookie of its WebSocket handshake. The handshake request goes
 * through the same security filter chain as every other request, so Spring already attached the authenticated user
 * to the STOMP session; the interceptor only refuses sessions that have none. Bearer tokens are not accepted.
 */
class StompAuthChannelInterceptorTest {
	private StompAuthChannelInterceptor interceptor;

	@BeforeEach
	void setUp() {
		interceptor = new StompAuthChannelInterceptor();
	}

	@Test
	void connectFromAHandshakeAuthenticatedBySessionIsAccepted() {
		var user = new TestingAuthenticationToken("ga12abc", null, "ROLE_student");
		user.setAuthenticated(true);
		Message<?> message = connectFrame(user, null);

		Message<?> result = interceptor.preSend(message, null);

		assertThat(result).isSameAs(message);
		assertThat(StompHeaderAccessor.wrap(message).getUser()).isEqualTo(user);
	}

	@Test
	void connectFromAnUnauthenticatedHandshakeIsRejected() {
		Message<?> message = connectFrame(null, null);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void aBearerTokenInTheConnectFrameDoesNotAuthenticate() {
		Message<?> message = connectFrame(null, "Bearer some-keycloak-token");

		assertThatThrownBy(() -> interceptor.preSend(message, null))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void anonymousAndUnauthenticatedPrincipalsAreRejected() {
		var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
		var notAuthenticated = new TestingAuthenticationToken("ga12abc", null);
		notAuthenticated.setAuthenticated(false);
		Principal plain = () -> "ga12abc";

		for (Principal principal : new Principal[] {anonymous, notAuthenticated, plain}) {
			Message<?> message = connectFrame(principal, null);

			assertThatThrownBy(() -> interceptor.preSend(message, null))
					.isInstanceOf(BadCredentialsException.class);
		}
	}

	@Test
	void nonConnectFramesAreLeftAlone() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, null);

		assertThat(result).isSameAs(message);
	}

	private static Message<?> connectFrame(Principal handshakeUser, String authorizationHeader) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		if (authorizationHeader != null) {
			accessor.setNativeHeader("Authorization", authorizationHeader);
		}
		accessor.setUser(handshakeUser);
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
