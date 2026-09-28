package de.tum.cit.aet.thesis.core.websocket;

import de.tum.cit.aet.thesis.core.security.JwtAuthConverter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Authenticates a STOMP session from the {@code Authorization} header of its {@code CONNECT}
 * frame. A native {@code WebSocket} handshake cannot carry custom HTTP headers, so the JWT that
 * every other request sends as a bearer token instead travels as a STOMP header once the socket is
 * open; {@code WebSecurityConfig} permits the unauthenticated handshake itself.
 *
 * <p>Reuses the exact same {@link JwtDecoder} and {@link JwtAuthConverter} beans the HTTP resource
 * server filter chain uses, so a STOMP session ends up with the same {@code ROLE_*} authorities
 * (from {@code user_groups}) as an HTTP request from the same user.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
	private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtDecoder jwtDecoder;
	private final JwtAuthConverter jwtAuthConverter;

	/**
	 * Creates the interceptor.
	 *
	 * @param jwtDecoder       decodes the bearer token from a CONNECT frame's Authorization header
	 * @param jwtAuthConverter builds the same authentication/authorities an HTTP request would get
	 */
	public StompAuthChannelInterceptor(JwtDecoder jwtDecoder, JwtAuthConverter jwtAuthConverter) {
		this.jwtDecoder = jwtDecoder;
		this.jwtAuthConverter = jwtAuthConverter;
	}

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
			accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
		}

		return message;
	}

	private AbstractAuthenticationToken authenticate(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new BadCredentialsException("Missing bearer token on STOMP CONNECT frame");
		}

		try {
			Jwt jwt = jwtDecoder.decode(authorizationHeader.substring(BEARER_PREFIX.length()));
			return jwtAuthConverter.convert(jwt);
		} catch (RuntimeException e) {
			log.debug("Rejected STOMP CONNECT with an invalid token", e);
			throw new BadCredentialsException("Invalid bearer token on STOMP CONNECT frame", e);
		}
	}
}
