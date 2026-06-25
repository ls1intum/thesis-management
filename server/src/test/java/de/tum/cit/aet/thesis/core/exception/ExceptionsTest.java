package de.tum.cit.aet.thesis.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import de.tum.cit.aet.thesis.core.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceAlreadyExistsException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

	@Test
	void mailingException_storesMessageAndCause() {
		Throwable cause = new RuntimeException("root");
		MailingException ex = new MailingException("smtp", cause);
		assertEquals("smtp", ex.getMessage());
		assertSame(cause, ex.getCause());
	}

	@Test
	void mailingException_messageOnly() {
		MailingException ex = new MailingException("smtp");
		assertEquals("smtp", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void uploadException_storesMessageAndCause() {
		Throwable cause = new RuntimeException("disk");
		UploadException ex = new UploadException("upload", cause);
		assertEquals("upload", ex.getMessage());
		assertSame(cause, ex.getCause());
	}

	@Test
	void uploadException_messageOnly() {
		UploadException ex = new UploadException("upload");
		assertEquals("upload", ex.getMessage());
	}

	@Test
	void accessDeniedException_storesMessage() {
		AccessDeniedException ex = new AccessDeniedException("denied");
		assertEquals("denied", ex.getMessage());
		assertNotNull(ex);
	}

	@Test
	void resourceNotFoundException_storesMessage() {
		assertEquals("missing", new ResourceNotFoundException("missing").getMessage());
	}

	@Test
	void resourceAlreadyExistsException_storesMessage() {
		assertEquals("dup", new ResourceAlreadyExistsException("dup").getMessage());
	}

	@Test
	void resourceInvalidParametersException_storesMessage() {
		assertEquals("invalid", new ResourceInvalidParametersException("invalid").getMessage());
	}
}
