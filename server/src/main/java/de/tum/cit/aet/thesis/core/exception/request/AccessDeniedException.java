package de.tum.cit.aet.thesis.core.exception.request;

public class AccessDeniedException extends RuntimeException {

	public AccessDeniedException(String message) {
		super(message);
	}
}
