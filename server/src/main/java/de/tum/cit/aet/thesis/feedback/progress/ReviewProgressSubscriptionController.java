package de.tum.cit.aet.thesis.feedback.progress;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Answers the client's "am I subscribed yet?" probe for one review job.
 *
 * <p>A review starts as soon as the client POSTs it, and progress events are not replayed, so the
 * client must know its subscription is live before firing that request. STOMP's {@code receipt}
 * header would be the natural confirmation, but Spring's simple broker only ever answers receipts
 * on {@code DISCONNECT}, so the client instead probes this destination and treats the
 * {@link ReviewProgressStatus#SUBSCRIBED} event echoed back over its own progress queue as the
 * confirmation. Routing the answer through the broker — rather than replying directly to the
 * probe — is what makes it meaningful: it arrives only once the broker has actually registered the
 * subscription, so an unanswered probe (the client retries a few times) means the client would
 * genuinely have missed events.
 */
@Controller
public class ReviewProgressSubscriptionController {
	private final ReviewProgressPublisher progressPublisher;

	/**
	 * Creates the controller.
	 *
	 * @param progressPublisher publishes the acknowledgement to the probing user's job destination
	 */
	public ReviewProgressSubscriptionController(ReviewProgressPublisher progressPublisher) {
		this.progressPublisher = progressPublisher;
	}

	/**
	 * Echoes a {@link ReviewProgressStatus#SUBSCRIBED} event back to the caller's own destination
	 * for the given job. Clients may probe repeatedly until one answer arrives; every probe is
	 * independent and nothing is stored.
	 *
	 * @param jobId     the client-supplied id correlating events to one review request
	 * @param principal the authenticated STOMP session probing its own destination
	 */
	@MessageMapping("/ai-review-progress/{jobId}/probe")
	public void probeSubscription(@DestinationVariable UUID jobId, Principal principal) {
		progressPublisher.publishSubscribed(principal.getName(), jobId);
	}
}
