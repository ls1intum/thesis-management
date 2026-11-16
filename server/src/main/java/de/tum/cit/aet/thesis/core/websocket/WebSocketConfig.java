package de.tum.cit.aet.thesis.core.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket transport used to push live progress events (see
 * {@code feedback.progress}) to the client while an AI review is running. There is no SockJS
 * fallback — this is an internal tool used from modern browsers only.
 *
 * <p>The handshake is an ordinary HTTP request authenticated by the session cookie, like every other
 * request; {@link StompAuthChannelInterceptor} additionally refuses frames of sessions without an
 * authenticated user and restricts the destinations a session may subscribe to and send to.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private final String clientHost;
	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

	/**
	 * Creates the configuration.
	 *
	 * @param clientHost                  the single allowed origin for the STOMP endpoint handshake
	 * @param stompAuthChannelInterceptor refuses unauthenticated sessions and restricts their destinations
	 */
	public WebSocketConfig(@Value("${thesis-management.client.host}") String clientHost,
			StompAuthChannelInterceptor stompAuthChannelInterceptor) {
		this.clientHost = clientHost;
		this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOrigins(clientHost);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.setApplicationDestinationPrefixes("/app");
		registry.enableSimpleBroker("/topic", "/queue");
		// Default user destination prefix "/user" — SimpMessagingTemplate.convertAndSendToUser(...)
		// resolves to "/user/{username}/queue/...", which the session subscribes to as
		// "/user/queue/...".
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}
}
