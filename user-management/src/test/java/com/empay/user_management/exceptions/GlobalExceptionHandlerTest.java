package com.empay.user_management.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.empay.user_management.dto.SodConflictDetail;
import com.empay.user_management.valueobjects.ErrorResponse;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	@DisplayName("handleNotFound returns 404 with error payload")
	void testHandleNotFound() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/123");
		ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", "User not found");

		ResponseEntity<ErrorResponse> response = handler.handleNotFound(request, ex);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("User not found", response.getBody().getMessage());
	}

	@Test
	@DisplayName("handleAlreadyExists returns 409 with error payload")
	void testHandleAlreadyExists() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
		ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException("User", "id", "User exists");

		ResponseEntity<ErrorResponse> response = handler.handleAlreadyExists(request, ex);
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("User exists", response.getBody().getMessage());
	}

	@Test
	@DisplayName("handleSodConflict returns 409 with conflict details and derived role")
	void testHandleSodConflict_WithDerivedRoleConflict() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/roles");
		SodConflictDetail detail = new SodConflictDetail("ROLE_CONFLICT", "ROLE_A", "ROLE_B", "BLOCK",
				"Conflict rationale", 10L, "DERIVED_CONFLICT");
		SodConflictException ex = new SodConflictException("SoD violation", List.of(detail));

		ResponseEntity<ErrorResponse> response = handler.handleSodConflict(request, ex);
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("SoD violation", response.getBody().getMessage());
		assertEquals(1, response.getBody().getErrors().size());
		assertEquals("DERIVED_CONFLICT", response.getBody().getErrors().get(0).get("derivedRoleConflict"));
	}

	@Test
	@DisplayName("handleSodConflict handles null conflicts gracefully")
	void testHandleSodConflict_NullConflicts() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/roles");
		SodConflictException ex = new SodConflictException("Empty conflict list", null);

		ResponseEntity<ErrorResponse> response = handler.handleSodConflict(request, ex);
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().getErrors().isEmpty());
	}

	@Test
	@DisplayName("handleDataIntegrity returns 409 and extracts root cause")
	void testHandleDataIntegrity_WithCause() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
		RuntimeException rootCause = new RuntimeException("ORA-00001: unique constraint violated");
		DataIntegrityViolationException ex = new DataIntegrityViolationException("wrapper message", rootCause);

		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(request, ex);
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("ORA-00001: unique constraint violated", response.getBody().getMessage());
	}

	@Test
	@DisplayName("handleDataIntegrity fallback when message is blank and no cause")
	void testHandleDataIntegrity_BlankMessageNoCause() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
		DataIntegrityViolationException ex = new DataIntegrityViolationException("");

		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(request, ex);
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Database operation failed", response.getBody().getMessage());
	}

	@Test
	@DisplayName("handleValidation returns 400 with field errors")
	void testHandleValidation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
		java.lang.reflect.Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("testHandleValidation");
		org.springframework.core.MethodParameter parameter = new org.springframework.core.MethodParameter(method, -1);
		org.springframework.validation.BeanPropertyBindingResult bindingResult =
				new org.springframework.validation.BeanPropertyBindingResult(new Object(), "userDto");
		bindingResult.addError(new FieldError("userDto", "username", "Username is required"));

		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

		ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Validation failed", response.getBody().getMessage());
		assertEquals(1, response.getBody().getErrors().size());
		assertEquals("username", response.getBody().getErrors().get(0).get("fieldName"));
		assertEquals("Username is required", response.getBody().getErrors().get(0).get("fieldError"));
	}

	@Test
	@DisplayName("handleBadRequest returns 400 with message or default")
	void testHandleBadRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/items");

		ResponseEntity<ErrorResponse> responseWithMsg = handler.handleBadRequest(request,
				new IllegalArgumentException("Invalid sort param"));
		assertEquals(HttpStatus.BAD_REQUEST, responseWithMsg.getStatusCode());
		assertEquals("Invalid sort param", responseWithMsg.getBody().getMessage());

		ResponseEntity<ErrorResponse> responseBlank = handler.handleBadRequest(request,
				new IllegalArgumentException(""));
		assertEquals(HttpStatus.BAD_REQUEST, responseBlank.getStatusCode());
		assertEquals("Invalid request parameters", responseBlank.getBody().getMessage());
	}

	@Test
	@DisplayName("handleNoHandlerFound returns 404 with endpoint details")
	void testHandleNoHandlerFound() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/unknown");
		NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/unknown", new HttpHeaders());

		ResponseEntity<ErrorResponse> response = handler.handleNoHandlerFound(ex, request);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("No endpoint found for GET /unknown", response.getBody().getMessage());
	}

	@Test
	@DisplayName("handleGeneric returns 500 with generic error message")
	void testHandleGeneric() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/crash");
		ResponseEntity<ErrorResponse> response = handler.handleGeneric(request, new RuntimeException("NPE"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Something went wrong. Please contact support.", response.getBody().getMessage());
	}

	@Test
	@DisplayName("handleAccessDenied returns 403 with permission message")
	void testHandleAccessDenied() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secure");
		ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(request,
				new AccessDeniedException("Access is denied"));

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("You do not have permission to perform this action.", response.getBody().getMessage());
		assertEquals("ACCESS_DENIED", response.getBody().getType());
	}

	@Test
	@DisplayName("handleResponseStatusException handles conflict and bad request variants")
	void testHandleResponseStatusException() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");

		ResponseStatusException conflictEx = new ResponseStatusException(HttpStatus.CONFLICT, "Item duplicate");
		ResponseEntity<ErrorResponse> conflictResp = handler.handleResponseStatusException(request, conflictEx);
		assertEquals(HttpStatus.CONFLICT, conflictResp.getStatusCode());
		assertEquals("Conflict Error", conflictResp.getBody().getType());
		assertEquals("Item duplicate", conflictResp.getBody().getMessage());

		ResponseStatusException defaultReasonEx = new ResponseStatusException(HttpStatus.BAD_REQUEST);
		ResponseEntity<ErrorResponse> badReqResp = handler.handleResponseStatusException(request, defaultReasonEx);
		assertEquals(HttpStatus.BAD_REQUEST, badReqResp.getStatusCode());
		assertEquals("Request failed", badReqResp.getBody().getMessage());
	}

	@Test
	@DisplayName("handleBadRequestException returns 400")
	void testHandleBadRequestException() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
		ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(request,
				new BadRequestException("Malformed request"));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Malformed request", response.getBody().getMessage());
	}

	@Test
	@DisplayName("handleInvalidRequestBody returns 400 with readable message")
	void testHandleInvalidRequestBody() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
		HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
		when(ex.getMessage()).thenReturn("Could not read document");

		ResponseEntity<ErrorResponse> response = handler.handleInvalidRequestBody(request, ex);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Invalid request body. Please check the JSON format and field values.",
				response.getBody().getMessage());
	}
}
