package de.tum.cit.aet.thesis.core.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.security.JwtAuthConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

	@Mock
	private JwtDecoder jwtDecoder;

	@Mock
	private JwtAuthConverter jwtAuthConverter;

	private StompAuthChannelInterceptor interceptor;

	@BeforeEach
	void setUp() {
		interceptor = new StompAuthChannelInterceptor(jwtDecoder, jwtAuthConverter);
	}

	@Test
	void connectWithAValidBearerTokenAuthenticatesTheSession() {
		Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(60),
				Map.of("alg", "none"), Map.of("preferred_username", "ga12abc"));
		when(jwtDecoder.decode("token-value")).thenReturn(jwt);
		var authentication = new TestingAuthenticationToken("ga12abc", null);
		when(jwtAuthConverter.convert(jwt)).thenReturn(authentication);

		Message<?> message = connectFrameWithAuthorization("Bearer token-value");
		interceptor.preSend(message, null);

		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		assertThat(accessor.getUser()).isEqualTo(authentication);
	}

	@Test
	void connectWithoutAnAuthorizationHeaderIsRejected() {
		Message<?> message = connectFrameWithAuthorization(null);

		assertThatThrownBy(() -> interceptor.preSend(message, null))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void connectWithAMalformedAuthorizationHeaderIsRejected() {
		Message<?> message = connectFrameWithAuthorization("token-value");

		assertThatThrownBy(() -> interceptor.preSend(message, null))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void connectWithATokenTheDecoderRejectsIsRejected() {
		when(jwtDecoder.decode("bad-token")).thenThrow(new RuntimeException("invalid signature"));

		Message<?> message = connectFrameWithAuthorization("Bearer bad-token");

		assertThatThrownBy(() -> interceptor.preSend(message, null))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void nonConnectFramesAreLeftAlone() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, null);

		assertThat(result).isSameAs(message);
	}

	private static Message<?> connectFrameWithAuthorization(String authorizationHeader) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		if (authorizationHeader != null) {
			accessor.setNativeHeader("Authorization", authorizationHeader);
		}
		// Mirrors how the real STOMP subprotocol handler builds inbound messages: headers stay
		// mutable so a channel interceptor can call accessor.setUser(...) in preSend.
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
