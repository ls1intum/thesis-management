package de.tum.cit.aet.thesis.feedback.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes {@link ReviewProgressEvent}s to the user who requested a review, over STOMP. A review
 * runs synchronously on the request thread; this is purely a side channel so the client can show
 * live per-call progress while it waits for the HTTP response.
 */
@Component
public class ReviewProgressPublisher {
	private static final Logger log = LoggerFactory.getLogger(ReviewProgressPublisher.class);

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * Creates the publisher.
	 *
	 * @param messagingTemplate used to send events to a specific user's STOMP destination
	 */
	public ReviewProgressPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	/**
	 * Builds a {@link ProgressReporter} that publishes every event to the given user's
	 * {@code /queue/ai-review-progress/{jobId}} destination.
	 *
	 * @param username the STOMP principal name (the user's university id) to publish to
	 * @param jobId    the client-supplied id correlating events to one review request
	 * @return a reporter bound to that user and job
	 */
	public ProgressReporter reporterFor(String username, UUID jobId) {
		String destination = "/queue/ai-review-progress/" + jobId;

		return new ProgressReporter() {
			@Override
			public void stepStarted(String stepId, String label, int index, int total) {
				publish(new ReviewProgressEvent(jobId, stepId, label, ReviewProgressStatus.STARTED, index, total, null));
			}

			@Override
			public void stepCompleted(String stepId, int index, int total) {
				publish(new ReviewProgressEvent(jobId, stepId, null, ReviewProgressStatus.COMPLETED, index, total, null));
			}

			@Override
			public void stepFailed(String stepId, int index, int total, String message) {
				publish(new ReviewProgressEvent(jobId, stepId, null, ReviewProgressStatus.FAILED, index, total, message));
			}

			private void publish(ReviewProgressEvent event) {
				try {
					messagingTemplate.convertAndSendToUser(username, destination, event);
				} catch (RuntimeException e) {
					// A disconnected or never-subscribed client must never fail the review itself —
					// progress is a side channel, not a required part of the request.
					log.debug("Failed to publish review progress event {} to {}", event, username, e);
				}
			}
		};
	}
}
