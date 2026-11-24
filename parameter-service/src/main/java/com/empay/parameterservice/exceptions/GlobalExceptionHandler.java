package com.empay.parameterservice.exceptions;

import com.empay.parameterservice.valueobjects.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Map<String, String>> errors = new ArrayList<>();
        Map<String, String> err = Map.of(
                "fieldName", ex.getField() == null ? "error" : ex.getField(),
                "fieldError", ex.getMessage()
        );
        errors.add(err);
        return build(HttpStatus.NOT_FOUND,  ex.getResourceName() + " not found", request.getRequestURI(), EXPECTATION_ERROR, errors);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(HttpServletRequest request, ResourceAlreadyExistsException ex) {
        log.warn("Resource already exists at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        List<Map<String, String>> errors = new ArrayList<>();
        Map<String, String> err = Map.of(
                "fieldName", ex.getField() == null ? "error" : ex.getField(),
                "fieldError", ex.getMessage()
        );
        errors.add(err);
        return build(HttpStatus.CONFLICT,  ex.getResourceName() + " already exists", request.getRequestURI(), EXPECTATION_ERROR, errors);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(HttpServletRequest request, ServiceException ex) {
        log.error("Service error at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                request.getRequestURI(),
                SYSTEM_ERROR,
                null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(HttpServletRequest request, DataIntegrityViolationException ex) {
        log.error("Data integrity violation at [{}]", request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "Duplicate or invalid data", request.getRequestURI(), CONSTRAINT_ERROR, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        log.warn("Validation failed at [{}]", request.getRequestURI(), ex);
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
                        Map<String, String> err = new HashMap<>();
                            err.put("fieldName", error.getField());
                            err.put("fieldError", error.getDefaultMessage());
                            errors.add(err);
                }
        );

        return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), VALIDATION_ERROR, errors);
    }

    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, IllegalArgumentException.class })
    public ResponseEntity<ErrorResponse> handleBadRequest(HttpServletRequest request, Exception ex) {
        log.warn("Bad request at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, "Invalid request parameters", request.getRequestURI(), INVALID_REQUEST_ERROR, null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        log.warn("No handler found for [{} {}]", ex.getHttpMethod(), ex.getRequestURL());
        List<Map<String, String>> errors = new ArrayList<>();
        Map<String, String> err = Map.of(
                "error", "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL()
        );
        errors.add(err);

        return build(
                HttpStatus.NOT_FOUND,
                "Endpoint not found",
                request.getRequestURI(),
                SYSTEM_ERROR,
                errors
        );
    }
    
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Object> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Forbidden",
                        "message", "Access Denied"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(HttpServletRequest request, Exception ex) {
        log.error("Unhandled exception at [{}]", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please contact support.",
                request.getRequestURI(),
                SYSTEM_ERROR,
                null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                String path, String type,
                                                List<Map<String, String>> errors) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .type(type)
                .message(message)
                .path(path)
                .errors(errors)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
