package de.tum.cit.aet.thesis.core.exception;

public class MailingException extends RuntimeException {
	public MailingException(String message) {
		super(message);
	}

	public MailingException(String message, Throwable cause) {
		super(message, cause);
	}
}
