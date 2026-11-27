package de.tum.cit.aet.thesis.core.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class MailDeliveryServiceTest {
	private static final List<Duration> DELAYS = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15));

	private JavaMailSender mailSender;
	private MailConfig config;
	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> scheduledFuture;
	private MimeMessage message;
	private ListAppender<ILoggingEvent> logs;

	@BeforeEach
	void setUp() {
		mailSender = mock(JavaMailSender.class);
		config = mock(MailConfig.class);
		when(config.isEnabled()).thenReturn(true);
		scheduler = mock(ScheduledExecutorService.class);
		scheduledFuture = mock(ScheduledFuture.class);
		doReturnFuture();
		message = new MimeMessage(Session.getInstance(new Properties()));

		logs = new ListAppender<>();
		logs.start();
		((Logger) LoggerFactory.getLogger(MailDeliveryService.class)).addAppender(logs);
	}

	@AfterEach
	void tearDown() {
		((Logger) LoggerFactory.getLogger(MailDeliveryService.class)).detachAppender(logs);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void doReturnFuture() {
		when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn((ScheduledFuture) scheduledFuture);
	}

	private MailDeliveryService newService(List<Duration> delays, int capacity) {
		return new MailDeliveryService(mailSender, config, new MailRetryProperties(delays, capacity), scheduler);
	}

	private Runnable lastScheduledRetry() {
		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(scheduler, atLeastOnce()).schedule(captor.capture(), anyLong(), eq(TimeUnit.MILLISECONDS));
		return captor.getValue();
	}

	private static MailSendException connectionFailure() {
		return new MailSendException("Mail server connection failed", new MessagingException("Could not connect", new ConnectException("Connection refused")));
	}

	private static MailSendException smtpReply(int code) {
		return new MailSendException("Failed messages", new SMTPSendFailedException("RCPT TO", code, code + " reply", null, new Address[0], new Address[0], new Address[0]));
	}

	private List<ILoggingEvent> warnings() {
		return logs.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
	}

	@Test
	void firstAttemptSucceeds_NothingQueued() {
		MailDeliveryService service = newService(DELAYS, 10);

		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.SENT);

		verify(mailSender, times(1)).send(message);
		assertThat(service.pendingCount()).isZero();
		verifyNoInteractions(scheduler);
	}

	@Test
	void temporaryFailureThenSuccessOnRetry() {
		doThrow(connectionFailure()).doNothing().when(mailSender).send(message);
		MailDeliveryService service = newService(DELAYS, 10);

		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.QUEUED);

		assertThat(service.pendingCount()).isEqualTo(1);
		verify(scheduler).schedule(any(Runnable.class), eq(Duration.ofMinutes(1).toMillis()), eq(TimeUnit.MILLISECONDS));

		lastScheduledRetry().run();

		verify(mailSender, times(2)).send(message);
		assertThat(service.pendingCount()).isZero();
		verify(scheduler, times(1)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
	}

	@Test
	void retriesUseConfiguredDelaysInOrderAndStopAfterMaximum() {
		doThrow(connectionFailure()).when(mailSender).send(message);
		MailDeliveryService service = newService(DELAYS, 10);

		service.send(message, "Subject");
		for (int retry = 0; retry < DELAYS.size(); retry++) {
			lastScheduledRetry().run();
		}

		// first attempt + one attempt per configured delay
		verify(mailSender, times(1 + DELAYS.size())).send(message);

		ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
		verify(scheduler, times(DELAYS.size())).schedule(any(Runnable.class), delays.capture(), eq(TimeUnit.MILLISECONDS));
		assertThat(delays.getAllValues()).containsExactly(
				Duration.ofMinutes(1).toMillis(), Duration.ofMinutes(5).toMillis(), Duration.ofMinutes(15).toMillis());

		// gave up: nothing queued, nothing scheduled any more, a warning was logged
		assertThat(service.pendingCount()).isZero();
		assertThat(warnings()).anyMatch(event -> event.getFormattedMessage().contains("giving up"));
	}

	@Test
	void permanentFailureIsRethrownAndNotRetried() {
		doThrow(smtpReply(550)).when(mailSender).send(message);
		MailDeliveryService service = newService(DELAYS, 10);

		assertThatThrownBy(() -> service.send(message, "Subject")).isInstanceOf(MailSendException.class);

		verify(mailSender, times(1)).send(message);
		assertThat(service.pendingCount()).isZero();
		verifyNoInteractions(scheduler);
	}

	@Test
	void permanentFailureOnRetryStopsRetrying() {
		doThrow(connectionFailure()).doThrow(smtpReply(554)).when(mailSender).send(message);
		MailDeliveryService service = newService(DELAYS, 10);

		service.send(message, "Subject");
		lastScheduledRetry().run();

		verify(mailSender, times(2)).send(message);
		verify(scheduler, times(1)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
		assertThat(service.pendingCount()).isZero();
		assertThat(warnings()).anyMatch(event -> event.getFormattedMessage().contains("permanent"));
	}

	@Test
	void retryDisabledWithoutDelays_FailureIsRethrown() {
		doThrow(connectionFailure()).when(mailSender).send(message);
		MailDeliveryService service = newService(List.of(), 10);

		assertThatThrownBy(() -> service.send(message, "Subject")).isInstanceOf(MailSendException.class);

		assertThat(service.pendingCount()).isZero();
		verifyNoInteractions(scheduler);
	}

	@Test
	void queueOverflowDropsOldest() {
		MimeMessage second = new MimeMessage(Session.getInstance(new Properties()));
		MimeMessage third = new MimeMessage(Session.getInstance(new Properties()));
		doThrow(connectionFailure()).when(mailSender).send(any(MimeMessage.class));
		MailDeliveryService service = newService(DELAYS, 2);

		service.send(message, "First");
		Runnable firstRetry = lastScheduledRetry();
		service.send(second, "Second");
		service.send(third, "Third");

		assertThat(service.pendingCount()).isEqualTo(2);
		verify(scheduledFuture, times(1)).cancel(false);
		assertThat(warnings()).anyMatch(event -> event.getFormattedMessage().contains("dropped the oldest") && event.getFormattedMessage().contains("First"));

		// a retry that was already due for the dropped mail does nothing
		clearInvocations(mailSender);
		firstRetry.run();
		verify(mailSender, never()).send(any(MimeMessage.class));
	}

	@Test
	void schedulerAlreadyShutDown_FailureIsRethrownAndNothingStaysQueued() {
		doThrow(connectionFailure()).when(mailSender).send(message);
		when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenThrow(new RejectedExecutionException("shut down"));
		MailDeliveryService service = newService(DELAYS, 10);

		assertThatThrownBy(() -> service.send(message, "Subject")).isInstanceOf(MailSendException.class);

		assertThat(service.pendingCount()).isZero();
	}

	@Test
	void mailDisabled_OnlyLogs() throws Exception {
		message.setFrom("sender@example.com");
		message.setSubject("Subject");
		message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("someone@example.com"));
		message.setContent("body", "text/plain");
		when(config.isEnabled()).thenReturn(false);
		MailDeliveryService service = newService(DELAYS, 10);

		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.SENT);

		verify(mailSender, never()).send(any(MimeMessage.class));
		verifyNoInteractions(scheduler);
		assertThat(service.pendingCount()).isZero();
	}

	@Test
	void shutdownStopsSchedulerAndWarnsAboutPendingMails() {
		doThrow(connectionFailure()).when(mailSender).send(message);
		MailDeliveryService service = newService(DELAYS, 10);
		service.send(message, "Subject");

		service.shutdown();

		verify(scheduler).shutdownNow();
		assertThat(warnings()).anyMatch(event -> event.getFormattedMessage().contains("1 email(s) still waiting"));
	}

	@Test
	void logsNeverContainTheMessageBody() throws Exception {
		message.setContent("secret-body-text", "text/plain");
		message.setSubject("Subject");
		message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("someone@example.com"));
		doThrow(connectionFailure()).when(mailSender).send(message);
		MailDeliveryService service = newService(List.of(Duration.ofMinutes(1)), 10);

		service.send(message, "Subject");
		lastScheduledRetry().run();
		service.shutdown();

		assertThat(logs.list).noneMatch(event -> event.getFormattedMessage().contains("secret-body-text"));
	}

	@Test
	void classifiesTemporaryFailures() {
		assertThat(MailDeliveryService.isTemporary(connectionFailure())).isTrue();
		assertThat(MailDeliveryService.isTemporary(new MailSendException("x", new MessagingException("x", new SocketTimeoutException("Read timed out"))))).isTrue();
		assertThat(MailDeliveryService.isTemporary(smtpReply(421))).isTrue();
		assertThat(MailDeliveryService.isTemporary(smtpReply(451))).isTrue();
		// Spring keeps the per-message exceptions in a map and sets no cause in this case
		assertThat(MailDeliveryService.isTemporary(new MailSendException(Map.of("message", new MessagingException("x", new ConnectException()))))).isTrue();
	}

	@Test
	void classifiesPermanentFailures() throws Exception {
		assertThat(MailDeliveryService.isTemporary(smtpReply(550))).isFalse();
		assertThat(MailDeliveryService.isTemporary(smtpReply(553))).isFalse();
		assertThat(MailDeliveryService.isTemporary(new MailAuthenticationException("bad credentials"))).isFalse();
		assertThat(MailDeliveryService.isTemporary(new IllegalStateException("bug"))).isFalse();
		assertThat(MailDeliveryService.isTemporary(new MailSendException("x", new AddressException("not an address")))).isFalse();
		Address[] invalid = {new InternetAddress("nobody@example.com")};
		SendFailedException invalidAddresses = new SendFailedException("Invalid Addresses", null, new Address[0], new Address[0], invalid);
		assertThat(MailDeliveryService.isTemporary(new MailSendException("x", invalidAddresses))).isFalse();
	}
}
