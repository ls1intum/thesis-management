package de.tum.cit.aet.thesis.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import de.tum.cit.aet.thesis.dto.ErrorDto;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceAlreadyExistsException;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;

import java.text.ParseException;

@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler({ ResourceNotFoundException.class })
    protected ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, ErrorDto.fromRuntimeException(ex), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({ ResourceAlreadyExistsException.class })
    protected ResponseEntity<Object> handleAlreadyExists(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, ErrorDto.fromRuntimeException(ex), new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({
            ParseException.class,
            ResourceInvalidParametersException.class,
            JsonParseException.class,
            JsonProcessingException.class,
    })
    protected ResponseEntity<Object> handleBadRequest(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, ErrorDto.fromRuntimeException(ex), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ AccessDeniedException.class })
    protected ResponseEntity<Object> handleAccessDenied(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, ErrorDto.fromRuntimeException(ex), new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler({ MailingException.class, UploadException.class })
    protected ResponseEntity<Object> handleServerError(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, ErrorDto.fromRuntimeException(ex), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
