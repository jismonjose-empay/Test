package com.empay.user_management.exceptions;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.empay.user_management.valueobjects.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

	private static final String SYSTEM_ERROR = "System Error";

	private static final String VALIDATION_ERROR = "Validation Error";

	private static final String CONSTRAINT_ERROR = "Constraint Error";

	private static final String EXPECTATION_ERROR = "Expectation Failed";

	private static final String INVALID_REQUEST_ERROR = "Invalid Request";

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest request, ResourceNotFoundException ex) {
		log.warn("Resource not found at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null, EXPECTATION_ERROR);
	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleAlreadyExists(HttpServletRequest request,
			ResourceAlreadyExistsException ex) {
		log.warn("Resource already exists at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
		return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null, EXPECTATION_ERROR);
	}

	@ExceptionHandler(SodConflictException.class)
	public ResponseEntity<ErrorResponse> handleSodConflict(HttpServletRequest request, SodConflictException ex) {
		log.warn("SoD Conflict violation at [{}]: {}", request.getRequestURI(), ex.getMessage());
		List<Map<String, String>> errors = new ArrayList<>();
		if (ex.getConflicts() != null) {
			for (var conflict : ex.getConflicts()) {
				Map<String, String> err = new HashMap<>();
				err.put("conflictType", conflict.conflictType());
				err.put("codeA", conflict.codeA());
				err.put("codeB", conflict.codeB());
				err.put("severity", conflict.severity());
				err.put("rationale", conflict.rationale());
				if (conflict.derivedRoleConflict() != null) {
					err.put("derivedRoleConflict", conflict.derivedRoleConflict());
				}
				errors.add(err);
			}
		}
		return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), errors, "SoD Conflict Error");
	}

	@ExceptionHandler({ DataIntegrityViolationException.class, org.springframework.dao.DataAccessException.class, jakarta.persistence.PersistenceException.class })
	public ResponseEntity<ErrorResponse> handleDataIntegrity(HttpServletRequest request, Exception ex) {
		log.error("Database or persistence violation at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
		String message = ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Database operation failed";
		Throwable root = ex;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		if (root.getMessage() != null && !root.getMessage().isBlank()) {
			message = root.getMessage();
		}
		return build(HttpStatus.CONFLICT, message, request.getRequestURI(), null, CONSTRAINT_ERROR);
	}


	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		log.warn("Validation failed at [{}]", request.getRequestURI(), ex);
		List<Map<String, String>> errors = new ArrayList<>();

		ex.getBindingResult().getFieldErrors().forEach(error -> {
			Map<String, String> fieldError = new HashMap<>();
			fieldError.put("fieldName", error.getField());
			fieldError.put("fieldError", error.getDefaultMessage());

			errors.add(fieldError);
		});

		return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), errors, VALIDATION_ERROR);
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, IllegalArgumentException.class })
	public ResponseEntity<ErrorResponse> handleBadRequest(HttpServletRequest request, Exception ex) {
		log.warn("Bad request at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
		String message = (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage()
				: "Invalid request parameters";
		return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null, INVALID_REQUEST_ERROR);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
		log.warn("No handler found for [{} {}]", ex.getHttpMethod(), ex.getRequestURL());
		return build(HttpStatus.NOT_FOUND, "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
				request.getRequestURI(), null, INVALID_REQUEST_ERROR);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(HttpServletRequest request, Exception ex) {
		log.error("Unhandled exception at [{}]", request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please contact support.",
				request.getRequestURI(), null, SYSTEM_ERROR);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(
			HttpServletRequest request,
			AccessDeniedException ex) {

		log.warn("Access denied at [{}]: {}",
				request.getRequestURI(), ex.getMessage());

		return build(
				HttpStatus.FORBIDDEN,
				"You do not have permission to perform this action.",
				request.getRequestURI(),
				null,
				"ACCESS_DENIED"
		);
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path,
			List<Map<String, String>> errors, String type) {
		var response = ErrorResponse.builder().timestamp(LocalDateTime.now(ZoneOffset.UTC)).status(status.value()).type(type)
				.message(message).path(path).errors(errors).build();
		return new ResponseEntity<>(response, status);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleResponseStatusException(HttpServletRequest request,
			ResponseStatusException ex) {

		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

		return build(status, ex.getReason() != null ? ex.getReason() : "Request failed", request.getRequestURI(), null,
				status == HttpStatus.CONFLICT ? "Conflict Error" : INVALID_REQUEST_ERROR);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequestException(HttpServletRequest request, BadRequestException ex) {

		

		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null, INVALID_REQUEST_ERROR);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequestBody(
	        HttpServletRequest request,
	        HttpMessageNotReadableException ex) {

	    log.warn("Invalid request body at [{}]: {}",
	            request.getRequestURI(), ex.getMessage());

	    return build(
	            HttpStatus.BAD_REQUEST,
	            "Invalid request body. Please check the JSON format and field values.",
	            request.getRequestURI(),
	            null,
	            INVALID_REQUEST_ERROR);
	}
}
