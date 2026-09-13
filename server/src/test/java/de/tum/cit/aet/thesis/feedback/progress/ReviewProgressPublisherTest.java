package de.tum.cit.aet.thesis.feedback.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ReviewProgressPublisherTest {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private ReviewProgressPublisher publisher;
	private final UUID jobId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		publisher = new ReviewProgressPublisher(messagingTemplate);
	}

	@Test
	void stepStartedPublishesToTheUsersJobQueueWithTheLabel() {
		publisher.reporterFor("ga12abc", jobId).stepStarted("structure", "Structure & Completeness", 1, 10);

		ArgumentCaptor<ReviewProgressEvent> event = ArgumentCaptor.forClass(ReviewProgressEvent.class);
		verify(messagingTemplate).convertAndSendToUser(eq("ga12abc"), eq("/queue/ai-review-progress/" + jobId),
				event.capture());
		assertThat(event.getValue()).isEqualTo(
				new ReviewProgressEvent(jobId, "structure", "Structure & Completeness",
						ReviewProgressStatus.STARTED, 1, 10, null));
	}

	@Test
	void stepCompletedPublishesWithoutALabel() {
		publisher.reporterFor("ga12abc", jobId).stepCompleted("structure", 1, 10);

		verify(messagingTemplate).convertAndSendToUser(eq("ga12abc"), eq("/queue/ai-review-progress/" + jobId),
				eq(new ReviewProgressEvent(jobId, "structure", null, ReviewProgressStatus.COMPLETED, 1, 10, null)));
	}

	@Test
	void stepFailedPublishesTheFailureMessage() {
		publisher.reporterFor("ga12abc", jobId).stepFailed("merge", 10, 10, "Timed out");

		verify(messagingTemplate).convertAndSendToUser(eq("ga12abc"), eq("/queue/ai-review-progress/" + jobId),
				eq(new ReviewProgressEvent(jobId, "merge", null, ReviewProgressStatus.FAILED, 10, 10, "Timed out")));
	}

	@Test
	void aBrokenPublishNeverEscapesTheReporter() {
		// Progress is a side channel: a disconnected client (or any other messaging failure) must
		// never surface as an exception to the review pipeline that is reporting progress.
		doThrow(new RuntimeException("no active session")).when(messagingTemplate)
				.convertAndSendToUser(any(), any(), any());

		assertThatCode(() -> publisher.reporterFor("ga12abc", jobId)
				.stepStarted("structure", "Structure & Completeness", 1, 10))
				.doesNotThrowAnyException();
	}
}
