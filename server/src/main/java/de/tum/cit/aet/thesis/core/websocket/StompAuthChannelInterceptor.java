package de.tum.cit.aet.thesis.core.websocket;

import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Refuses STOMP sessions whose WebSocket handshake was not authenticated.
 *
 * <p>The handshake is an ordinary HTTP request, so it carries the session cookie and goes through the same
 * security filter chain as every other request ({@code UserSessionAuthenticationFilter}). Spring attaches the
 * authenticated user of that request to the STOMP session, with the same {@code ROLE_*} authorities (from
 * {@code user_groups}) an HTTP request from that user gets. {@code WebSecurityConfig} permits the handshake itself
 * so that this interceptor, not a redirect or a bare 401 on the upgrade request, decides.</p>
 *
 * <p>Bearer tokens are not accepted here: Keycloak access tokens are only exchanged for a session at
 * {@code POST /v2/auth/session}. A socket that is already open when its session ends stays open until it closes.</p>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand()) && !isAuthenticated(accessor.getUser())) {
			throw new BadCredentialsException("The WebSocket handshake carried no valid session");
		}

		return message;
	}

	private static boolean isAuthenticated(Principal user) {
		return user instanceof Authentication authentication
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
