package de.tum.cit.aet.thesis.core.websocket;

import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
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
 * {@code user_groups}) an HTTP request from that user gets. The handshake itself therefore requires a session
 * ({@code WebSecurityConfig}); this interceptor additionally refuses {@code CONNECT} and {@code STOMP} frames
 * without an authenticated user and limits what an authenticated session may subscribe to and send.</p>
 *
 * <p>Bearer tokens are not accepted here: Keycloak access tokens are only exchanged for a session at
 * {@code POST /v2/auth/session}. A socket that is already open when its session ends stays open until it closes.</p>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
	private static final String USER_PROGRESS_DESTINATION_PREFIX = "/user/queue/ai-review-progress/";
	private static final String APPLICATION_DESTINATION_PREFIX = "/app/";

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor == null) {
			return message;
		}

		StompCommand command = accessor.getCommand();

		// STOMP is the 1.2 spelling of CONNECT and opens a session just the same.
		if ((StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) && !isAuthenticated(accessor.getUser())) {
			throw new BadCredentialsException("The WebSocket handshake carried no valid session");
		}

		// The simple broker would otherwise let any session subscribe with wildcards (for example "/queue/**") to
		// the events of every other user, and publish to any broker destination. A session may only follow its
		// own AI review progress and talk to the application destinations.
		if (StompCommand.SUBSCRIBE.equals(command) && !isOwnProgressDestination(accessor.getDestination())) {
			throw new AccessDeniedException("Subscriptions are limited to the own AI review progress");
		}

		if (StompCommand.SEND.equals(command) && !isApplicationDestination(accessor.getDestination())) {
			throw new AccessDeniedException("Messages may only be sent to application destinations");
		}

		return message;
	}

	private static boolean isOwnProgressDestination(String destination) {
		return destination != null
				&& destination.startsWith(USER_PROGRESS_DESTINATION_PREFIX)
				&& destination.length() > USER_PROGRESS_DESTINATION_PREFIX.length()
				&& destination.indexOf('*', USER_PROGRESS_DESTINATION_PREFIX.length()) < 0;
	}

	private static boolean isApplicationDestination(String destination) {
		return destination != null && destination.startsWith(APPLICATION_DESTINATION_PREFIX);
	}

	private static boolean isAuthenticated(Principal user) {
		return user instanceof Authentication authentication
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
