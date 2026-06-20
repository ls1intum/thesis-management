package de.tum.cit.aet.thesis.core.exception.request;

public class ResourceNotFoundException extends RuntimeException {
	public ResourceNotFoundException(String message) {
		super(message);
	}
}
