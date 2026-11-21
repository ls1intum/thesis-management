package de.tum.cit.aet.thesis.feedback.progress;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Drives the real {@link SimpAnnotationMethodMessageHandler} rather than calling the controller
 * directly, so the destination the client publishes to — application prefix, path variable and all
 * — is what is actually asserted. A typo on either side of that string silently costs every client
 * its progress bar, which is exactly what the handshake exists to prevent.
 */
@ExtendWith(MockitoExtension.class)
class ReviewProgressSubscriptionControllerTest {

	@Mock
	private ReviewProgressPublisher progressPublisher;

	private SimpAnnotationMethodMessageHandler handler;
	private final UUID jobId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBean("reviewProgressSubscriptionController",
				ReviewProgressSubscriptionController.class,
				() -> new ReviewProgressSubscriptionController(progressPublisher));
		context.refresh();

		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
		handler = new SimpAnnotationMethodMessageHandler(channel, channel, new SimpMessagingTemplate(channel));
		handler.setApplicationContext(context);
		handler.setDestinationPrefixes(List.of("/app"));
		handler.afterPropertiesSet();
	}

	@Test
	void aProbeOnTheJobsDestinationAcknowledgesToTheProbingUser() {
		handler.handleMessage(probe("/app/ai-review-progress/" + jobId + "/probe", "ga12abc"));

		verify(progressPublisher).publishSubscribed("ga12abc", jobId);
	}

	@Test
	void aProbeOnAnUnknownDestinationIsIgnored() {
		handler.handleMessage(probe("/app/ai-review-progress/" + jobId, "ga12abc"));

		verifyNoInteractions(progressPublisher);
	}

	private Message<byte[]> probe(String destination, String username) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination(destination);
		accessor.setSessionId("session-1");
		accessor.setSessionAttributes(new HashMap<>());
		accessor.setUser(() -> username);
		accessor.setLeaveMutable(true);

		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
