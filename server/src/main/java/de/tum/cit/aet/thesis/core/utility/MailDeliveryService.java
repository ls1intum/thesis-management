package de.tum.cit.aet.thesis.core.utility;

import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Delivers prepared emails through the {@link JavaMailSender} and keeps a small in-memory retry queue for
 * temporary failures (relay unreachable, timeouts, SMTP 4xx replies).
 * <p>
 * The first attempt runs synchronously on the calling thread. If it fails temporarily, the email waits in a bounded
 * queue and a single background thread retries it after the configured delays. Permanent failures (SMTP 5xx replies,
 * invalid addresses, authentication errors) are rethrown to the caller and never retried. The queue lives in memory:
 * emails still waiting when the application stops are lost.
 */
@Component
@EnableConfigurationProperties(MailRetryProperties.class)
public class MailDeliveryService {
	private static final Logger log = LoggerFactory.getLogger(MailDeliveryService.class);

	/** Outcome of {@link #send(MimeMessage, String)} when no exception is thrown. */
	public enum Status {
		/** The email was handed to the mail server, or mail is disabled and the email was only logged. */
		SENT,
		/** The first attempt failed temporarily; the email waits for a retry in the background. */
		QUEUED
	}

	private final JavaMailSender mailSender;
	private final MailConfig config;
	private final List<Duration> delays;
	private final int queueCapacity;
	private final ScheduledExecutorService scheduler;

	private final Deque<PendingMail> pending = new ArrayDeque<>();

	/**
	 * Creates the service with its own single-threaded background scheduler.
	 *
	 * @param mailSender the Spring-managed mail sender
	 * @param config the mail configuration (provides the enabled flag)
	 * @param properties the retry settings
	 */
	@Autowired
	public MailDeliveryService(JavaMailSender mailSender, MailConfig config, MailRetryProperties properties) {
		this(mailSender, config, properties, Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "mail-retry");
			thread.setDaemon(true);
			return thread;
		}));
	}

	MailDeliveryService(JavaMailSender mailSender, MailConfig config, MailRetryProperties properties, ScheduledExecutorService scheduler) {
		this.mailSender = mailSender;
		this.config = config;
		this.delays = List.copyOf(properties.delays());
		this.queueCapacity = Math.max(1, properties.queueCapacity());
		this.scheduler = scheduler;
	}

	/**
	 * Creates an empty message for the underlying mail sender.
	 *
	 * @return a new, empty MIME message
	 */
	public MimeMessage createMimeMessage() {
		return mailSender.createMimeMessage();
	}

	/**
	 * Sends the message. A temporary failure queues it for a background retry, a permanent one is rethrown.
	 * When mail is disabled the message is only logged.
	 *
	 * @param message the prepared message
	 * @param subject the subject, used for logging only
	 * @return whether the message was sent or queued for retry
	 * @throws RuntimeException the failure of the first attempt if it is permanent or the retry is switched off
	 */
	public Status send(MimeMessage message, String subject) {
		if (!config.isEnabled()) {
			log.debug("Sending Mail (mail disabled)\n{}", MailLogger.getTextFromMimeMessage(message));
			return Status.SENT;
		}

		try {
			mailSender.send(message);
			return Status.SENT;
		} catch (RuntimeException exception) {
			if (delays.isEmpty() || !isRetryable(message, subject, exception)) {
				throw exception;
			}

			if (!enqueue(new PendingMail(message, subject))) {
				throw exception;
			}

			log.warn("Failed to send email '{}' temporarily, queued for retry: {}", subject, exception.getMessage());
			return Status.QUEUED;
		}
	}

	/**
	 * Returns the number of emails currently waiting for a retry.
	 *
	 * @return the number of queued emails
	 */
	public int pendingCount() {
		synchronized (pending) {
			return pending.size();
		}
	}

	/** Stops the background retry and reports how many emails are lost because they were still waiting. */
	@PreDestroy
	public void shutdown() {
		scheduler.shutdownNow();

		int count = pendingCount();
		if (count > 0) {
			log.warn("Mail retry stopped with {} email(s) still waiting; they are not delivered (the retry queue is in memory)", count);
		}
	}

	/** Returns false if the mail could not be queued because the scheduler is already shut down. */
	private boolean enqueue(PendingMail mail) {
		synchronized (pending) {
			if (pending.size() >= queueCapacity) {
				PendingMail oldest = pending.pollFirst();
				oldest.dropped = true;
				oldest.future.cancel(false);
				log.warn("Mail retry queue is full ({} emails), dropped the oldest email '{}'", queueCapacity, oldest.subject);
			}

			pending.addLast(mail);
			try {
				schedule(mail);
			} catch (RejectedExecutionException rejected) {
				pending.remove(mail);
				log.warn("Email '{}' could not be queued for retry, the mail retry is stopped", mail.subject);
				return false;
			}
			return true;
		}
	}

	private void schedule(PendingMail mail) {
		Duration delay = delays.get(mail.retries);
		mail.future = scheduler.schedule(() -> retry(mail), delay.toMillis(), TimeUnit.MILLISECONDS);
	}

	private void retry(PendingMail mail) {
		if (mail.dropped) {
			return;
		}

		mail.retries++;

		try {
			mailSender.send(mail.message);
			finish(mail);
			log.info("Sent email '{}' on retry {}", mail.subject, mail.retries);
		} catch (RuntimeException exception) {
			if (!isRetryable(mail.message, mail.subject, exception)) {
				finish(mail);
				log.warn("Failed to send email '{}' on retry {}, giving up (permanent failure)", mail.subject, mail.retries, exception);
			} else if (mail.retries >= delays.size()) {
				finish(mail);
				log.warn("Failed to send email '{}' after {} retries, giving up", mail.subject, mail.retries, exception);
			} else {
				log.warn("Failed to send email '{}' on retry {}, retrying in {}: {}", mail.subject, mail.retries, delays.get(mail.retries), exception.getMessage());
				synchronized (pending) {
					if (!mail.dropped) {
						try {
							schedule(mail);
						} catch (RejectedExecutionException rejected) {
							pending.remove(mail);
						}
					}
				}
			}
		}
	}

	private void finish(PendingMail mail) {
		synchronized (pending) {
			pending.remove(mail);
		}
	}

	/**
	 * Decides whether a failed attempt is worth repeating. If the server rejected some recipients permanently while
	 * others only failed temporarily, the rejected ones are removed from the message first, so the retry goes to the
	 * remaining recipients only.
	 */
	private boolean isRetryable(MimeMessage message, String subject, Throwable failure) {
		return removeRejectedRecipients(message, subject, failure) || isTemporary(failure);
	}

	/**
	 * Handles a failure in which the server accepted some recipients and refused others. JavaMail reports this as a
	 * {@link SendFailedException} with the invalid (permanently rejected), valid-unsent (temporarily failed) and
	 * valid-sent addresses. Without {@code mail.smtp.sendpartial} nothing is delivered in that case, so the
	 * valid-sent list is normally empty; if it is not, those addresses are removed too so nobody gets a duplicate.
	 *
	 * @return true if recipients were removed and valid-unsent recipients remain on the message, false if the failure
	 *         has to be classified as a whole
	 */
	private static boolean removeRejectedRecipients(MimeMessage message, String subject, Throwable failure) {
		Set<String> invalid = new HashSet<>();
		Set<String> sent = new HashSet<>();
		Set<String> unsent = new HashSet<>();

		for (Throwable cause : collectCauses(failure)) {
			if (cause instanceof SendFailedException sendFailed) {
				addAddresses(invalid, sendFailed.getInvalidAddresses());
				addAddresses(sent, sendFailed.getValidSentAddresses());
				addAddresses(unsent, sendFailed.getValidUnsentAddresses());
			}
		}

		unsent.removeAll(invalid);
		unsent.removeAll(sent);
		if (unsent.isEmpty() || invalid.isEmpty() && sent.isEmpty()) {
			return false;
		}

		try {
			Set<String> removed = new HashSet<>(invalid);
			removed.addAll(sent);

			Map<Message.RecipientType, Address[]> remaining = new LinkedHashMap<>();
			int remainingCount = 0;
			for (Message.RecipientType type : List.of(Message.RecipientType.TO, Message.RecipientType.CC, Message.RecipientType.BCC)) {
				Address[] current = message.getRecipients(type);
				Address[] kept = current == null ? null : Arrays.stream(current).filter(address -> !removed.contains(normalize(address))).toArray(Address[]::new);
				remaining.put(type, kept == null || kept.length == 0 ? null : kept);
				remainingCount += kept == null ? 0 : kept.length;
			}

			if (remainingCount == 0) {
				return false;
			}

			for (Map.Entry<Message.RecipientType, Address[]> entry : remaining.entrySet()) {
				message.setRecipients(entry.getKey(), entry.getValue());
			}

			log.warn("Email '{}': {} recipient(s) rejected permanently or already served were removed, retrying for the remaining {}",
					subject, removed.size(), remainingCount);
			return true;
		} catch (MessagingException exception) {
			return false;
		}
	}

	private static void addAddresses(Set<String> target, Address[] addresses) {
		if (addresses != null) {
			for (Address address : addresses) {
				target.add(normalize(address));
			}
		}
	}

	private static String normalize(Address address) {
		String value = address instanceof InternetAddress internetAddress ? internetAddress.getAddress() : address.toString();
		if (value == null) {
			return "";
		}

		// The domain is case-insensitive, the local part may be case-sensitive (RFC 5321), so keep its case.
		String trimmed = value.trim();
		int at = trimmed.lastIndexOf('@');
		return at < 0 ? trimmed : trimmed.substring(0, at) + trimmed.substring(at).toLowerCase(Locale.ROOT);
	}

	/**
	 * Decides whether a failed attempt is worth repeating. Connection problems, timeouts and SMTP 4xx replies are
	 * temporary; SMTP 5xx replies, invalid addresses, authentication errors and message construction errors are not.
	 * A failure that is none of these is temporary only if it is a mail or IO exception.
	 */
	static boolean isTemporary(Throwable failure) {
		boolean temporary = false;

		for (Throwable cause : collectCauses(failure)) {
			if (cause instanceof AuthenticationFailedException
					|| cause instanceof MailAuthenticationException
					|| cause instanceof MailParseException
					|| cause instanceof MailPreparationException
					|| cause instanceof AddressException
					|| cause instanceof NoSuchProviderException) {
				return false;
			}

			int code = smtpReturnCode(cause);
			if (code >= 500) {
				return false;
			}

			if (code >= 400
					|| cause instanceof IOException
					|| cause instanceof MailSendException
					|| cause instanceof MessagingException && !(cause instanceof SendFailedException)) {
				temporary = true;
			}

			if (cause instanceof SendFailedException sendFailed && code < 0 && sendFailed.getInvalidAddresses() != null
					&& sendFailed.getInvalidAddresses().length > 0) {
				return false;
			}
		}

		return temporary;
	}

	private static int smtpReturnCode(Throwable cause) {
		if (cause instanceof SMTPSendFailedException exception) {
			return exception.getReturnCode();
		}
		if (cause instanceof SMTPAddressFailedException exception) {
			return exception.getReturnCode();
		}
		if (cause instanceof SMTPSenderFailedException exception) {
			return exception.getReturnCode();
		}
		return -1;
	}

	/** Flattens the cause chain, including the per-message exceptions Spring keeps in a {@link MailSendException}. */
	private static List<Throwable> collectCauses(Throwable failure) {
		List<Throwable> causes = new ArrayList<>();
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		collectCauses(failure, causes, seen);
		return causes;
	}

	private static void collectCauses(Throwable failure, List<Throwable> causes, Set<Throwable> seen) {
		if (failure == null || !seen.add(failure)) {
			return;
		}

		causes.add(failure);
		collectCauses(failure.getCause(), causes, seen);

		if (failure instanceof MailSendException mailSendException) {
			for (Exception messageException : mailSendException.getMessageExceptions()) {
				collectCauses(messageException, causes, seen);
			}
		}
	}

	private static final class PendingMail {
		private final MimeMessage message;
		private final String subject;
		private int retries;
		private volatile boolean dropped;
		private ScheduledFuture<?> future;

		private PendingMail(MimeMessage message, String subject) {
			this.message = message;
			this.subject = subject;
		}
	}
}
