package de.tum.cit.aet.thesis.core.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs {@link MailDeliveryService} against the real JavaMail/Angus SMTP client and a tiny scripted SMTP server, to
 * verify how the client reports recipients that the server rejects permanently (5xx) or temporarily (4xx).
 */
class MailDeliveryServiceSmtpTest {
	private ScriptedSmtpServer server;
	private ScheduledExecutorService scheduler;
	private MailDeliveryService service;
	private JavaMailSenderImpl mailSender;

	@BeforeEach
	@SuppressWarnings({"unchecked", "rawtypes"})
	void setUp() throws Exception {
		server = new ScriptedSmtpServer();

		mailSender = new JavaMailSenderImpl();
		mailSender.setHost("127.0.0.1");
		mailSender.setPort(server.port());
		mailSender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "5000");
		mailSender.getJavaMailProperties().put("mail.smtp.timeout", "5000");

		MailConfig config = mock(MailConfig.class);
		when(config.isEnabled()).thenReturn(true);

		scheduler = mock(ScheduledExecutorService.class);
		when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn((ScheduledFuture) mock(ScheduledFuture.class));

		service = new MailDeliveryService(mailSender, config, new MailRetryProperties(List.of(Duration.ofMinutes(1), Duration.ofMinutes(5)), 10), scheduler);
	}

	@AfterEach
	void tearDown() throws Exception {
		server.close();
	}

	private MimeMessage message(String to, String cc, String bcc) throws Exception {
		MimeMessage message = mailSender.createMimeMessage();
		message.setFrom("sender@example.com");
		message.setSubject("Subject");
		message.setText("Body");
		if (to != null) {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		}
		if (cc != null) {
			message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
		}
		if (bcc != null) {
			message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
		}
		return message;
	}

	private Runnable scheduledRetry() {
		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(scheduler, atLeastOnce()).schedule(captor.capture(), anyLong(), any(TimeUnit.class));
		return captor.getAllValues().getLast();
	}

	@Test
	void permanentlyRejectedCcIsRemovedAndTemporarilyRejectedToIsRetried() throws Exception {
		MimeMessage message = message("defer-me@example.com", "reject-me@example.com", null);

		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.QUEUED);

		assertThat(server.deliveries).isEmpty();
		assertThat(message.getRecipients(Message.RecipientType.CC)).isNull();
		assertThat(message.getRecipients(Message.RecipientType.TO)).hasSize(1);

		server.deferred = false;
		scheduledRetry().run();

		assertThat(server.deliveries).hasSize(1);
		assertThat(server.deliveries.getFirst()).containsExactly("defer-me@example.com");
		assertThat(service.pendingCount()).isZero();
	}

	@Test
	void mixedCaseDomainRejectionOfToAndBccOnlyRecipientIsRetried() throws Exception {
		MimeMessage message = message("reject-me@EXAMPLE.com", null, "defer-bcc@example.com");

		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.QUEUED);

		assertThat(message.getRecipients(Message.RecipientType.TO)).isNull();
		assertThat(message.getRecipients(Message.RecipientType.BCC)).hasSize(1);

		server.deferred = false;
		scheduledRetry().run();

		assertThat(server.deliveries).hasSize(1);
		assertThat(server.deliveries.getFirst()).containsExactly("defer-bcc@example.com");
	}

	@Test
	void acceptedRecipientsStayOnTheMessageAndAreDeliveredOnRetry() throws Exception {
		MimeMessage message = message("ok@example.com", "reject-me@example.com", "defer-me@example.com");

		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.QUEUED);

		// nothing was delivered by the failed attempt (sendpartial is off), so both valid recipients are still due
		assertThat(server.deliveries).isEmpty();

		server.deferred = false;
		scheduledRetry().run();

		assertThat(server.deliveries).hasSize(1);
		assertThat(server.deliveries.getFirst()).containsExactlyInAnyOrder("ok@example.com", "defer-me@example.com");
	}

	@Test
	void onlyPermanentlyRejectedRecipientsAreNotRetried() throws Exception {
		MimeMessage message = message("reject-me@example.com", "Reject-Other@example.com", null);

		assertThatThrownBy(() -> service.send(message, "Subject")).isInstanceOf(RuntimeException.class);

		assertThat(service.pendingCount()).isZero();
		assertThat(server.deliveries).isEmpty();
	}

	@Test
	void recipientAcceptedNextToAPermanentlyRejectedOneStillGetsTheMailOnRetry() throws Exception {
		// the real client reports an accepted-but-unsent address as "valid unsent", exactly like a temporary failure
		MimeMessage message = message("reject-me@example.com", "ok@example.com", null);

		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.QUEUED);

		assertThat(message.getRecipients(Message.RecipientType.TO)).isNull();
		scheduledRetry().run();

		assertThat(server.deliveries).hasSize(1);
		assertThat(server.deliveries.getFirst()).containsExactly("ok@example.com");
		assertThat(service.pendingCount()).isZero();
	}

	@Test
	void secondRejectionDuringARetryRemovesTheNewlyRejectedRecipientAndKeepsRetrying() throws Exception {
		MimeMessage message = message("defer-me@example.com", "defer-too@example.com", "ok@example.com");
		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.QUEUED);

		// by the time of the retry the server rejects one of the formerly deferred addresses for good
		server.rejectAlso = "defer-too";
		scheduledRetry().run();

		assertThat(message.getRecipients(Message.RecipientType.CC)).isNull();
		assertThat(message.getAllRecipients()).hasSize(2);
		assertThat(service.pendingCount()).isEqualTo(1);

		server.deferred = false;
		scheduledRetry().run();

		assertThat(server.deliveries).hasSize(1);
		assertThat(server.deliveries.getFirst()).containsExactlyInAnyOrder("defer-me@example.com", "ok@example.com");
		assertThat(service.pendingCount()).isZero();
	}

	@Test
	void onlyTemporaryRejectionIsRetriedUnchanged() throws Exception {
		MimeMessage message = message("defer-me@example.com", "ok@example.com", null);

		assertThat(service.send(message, "Subject")).isEqualTo(MailDeliveryService.Status.QUEUED);

		assertThat(message.getRecipients(Message.RecipientType.TO)).hasSize(1);
		assertThat(message.getRecipients(Message.RecipientType.CC)).hasSize(1);
	}

	/** Accepts every RCPT except addresses containing "reject" (550) or, while {@code deferred}, "defer" (450). */
	private static final class ScriptedSmtpServer implements AutoCloseable {
		private final ServerSocket socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
		private final List<List<String>> deliveries = new CopyOnWriteArrayList<>();
		private volatile boolean deferred = true;
		private volatile String rejectAlso;

		private ScriptedSmtpServer() throws IOException {
			Thread thread = new Thread(this::acceptLoop, "scripted-smtp");
			thread.setDaemon(true);
			thread.start();
		}

		private int port() {
			return socket.getLocalPort();
		}

		private void acceptLoop() {
			while (!socket.isClosed()) {
				try (Socket client = socket.accept()) {
					serve(client);
				} catch (IOException ignored) {
					// closed or client gone
				}
			}
		}

		private void serve(Socket client) throws IOException {
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
			Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.US_ASCII);
			reply(out, "220 scripted ESMTP");

			List<String> accepted = new ArrayList<>();
			String line;
			while ((line = in.readLine()) != null) {
				String upper = line.toUpperCase(Locale.ROOT);
				if (upper.startsWith("MAIL FROM")) {
					accepted.clear();
					reply(out, "250 ok");
				} else if (upper.startsWith("RCPT TO")) {
					String address = line.substring(line.indexOf('<') + 1, line.indexOf('>')).toLowerCase(Locale.ROOT);
					if (address.contains("reject") || rejectAlso != null && address.contains(rejectAlso)) {
						reply(out, "550 5.1.1 no such user");
					} else if (deferred && address.contains("defer")) {
						reply(out, "450 4.2.0 try again later");
					} else {
						accepted.add(address);
						reply(out, "250 ok");
					}
				} else if (upper.equals("DATA")) {
					reply(out, "354 go ahead");
					while ((line = in.readLine()) != null && !line.equals(".")) {
						// consume the message
					}
					deliveries.add(new ArrayList<>(accepted));
					reply(out, "250 queued");
				} else if (upper.equals("RSET")) {
					accepted.clear();
					reply(out, "250 ok");
				} else if (upper.equals("QUIT")) {
					reply(out, "221 bye");
					return;
				} else {
					reply(out, "250 ok");
				}
			}
		}

		private static void reply(Writer out, String text) throws IOException {
			out.write(text + "\r\n");
			out.flush();
		}

		@Override
		public void close() throws IOException {
			socket.close();
		}
	}
}
