package de.tum.cit.aet.thesis.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.tum.cit.aet.thesis.core.dto.ErrorDto;
import de.tum.cit.aet.thesis.core.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceAlreadyExistsException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.core.JacksonException;

import java.text.ParseException;

class ResponseExceptionHandlerTest {

	private ResponseExceptionHandler handler;
	private ServletWebRequest request;

	@BeforeEach
	void setUp() {
		handler = new ResponseExceptionHandler();
		request = new ServletWebRequest(new MockHttpServletRequest());
	}

	private void assertErrorBody(ResponseEntity<Object> response, HttpStatus expectedStatus, String expectedMessage) {
		assertNotNull(response);
		assertEquals(expectedStatus, response.getStatusCode());
		assertInstanceOf(ErrorDto.class, response.getBody());
		ErrorDto body = (ErrorDto) response.getBody();
		assertEquals(expectedMessage, body.message());
		assertNotNull(body.timestamp());
	}

	@Test
	void handleNotFound_returns404() {
		ResponseEntity<Object> response = handler.handleNotFound(new ResourceNotFoundException("missing"), request);
		assertErrorBody(response, HttpStatus.NOT_FOUND, "missing");
	}

	@Test
	void handleAlreadyExists_returns409() {
		ResponseEntity<Object> response = handler.handleAlreadyExists(new ResourceAlreadyExistsException("dup"), request);
		assertErrorBody(response, HttpStatus.CONFLICT, "dup");
	}

	@Test
	void handleBadRequest_resourceInvalidParameters_returns400() {
		ResponseEntity<Object> response = handler.handleBadRequest(new ResourceInvalidParametersException("bad"), request);
		assertErrorBody(response, HttpStatus.BAD_REQUEST, "bad");
	}

	@Test
	void handleBadRequest_parseException_returns400() {
		ResponseEntity<Object> response = handler.handleBadRequest(new ParseException("parse error", 0), request);
		assertErrorBody(response, HttpStatus.BAD_REQUEST, "parse error");
	}

	@Test
	void handleBadRequest_jacksonException_returns400() {
		JacksonException jacksonException = new JacksonException("jackson error") {
		};
		ResponseEntity<Object> response = handler.handleBadRequest(jacksonException, request);
		assertErrorBody(response, HttpStatus.BAD_REQUEST, "jackson error");
	}

	@Test
	void handleAccessDenied_returns403() {
		ResponseEntity<Object> response = handler.handleAccessDenied(new AccessDeniedException("nope"), request);
		assertErrorBody(response, HttpStatus.FORBIDDEN, "nope");
	}

	@Test
	void handleServerError_mailingException_returns500() {
		ResponseEntity<Object> response = handler.handleServerError(new MailingException("mail failure"), request);
		assertErrorBody(response, HttpStatus.INTERNAL_SERVER_ERROR, "mail failure");
	}

	@Test
	void handleServerError_uploadException_returns500() {
		ResponseEntity<Object> response = handler.handleServerError(new UploadException("upload failed"), request);
		assertErrorBody(response, HttpStatus.INTERNAL_SERVER_ERROR, "upload failed");
	}

	@Test
	void handleNotFound_nullMessage_usesFallback() {
		ResponseEntity<Object> response = handler.handleNotFound(new ResourceNotFoundException(null), request);
		assertErrorBody(response, HttpStatus.NOT_FOUND, "An unexpected error occurred");
	}
}
