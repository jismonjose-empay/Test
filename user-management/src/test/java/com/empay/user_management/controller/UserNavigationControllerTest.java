package com.empay.user_management.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.empay.user_management.dto.ModuleMenuNode;
import com.empay.user_management.dto.UserNavigationResponse;
import com.empay.user_management.service.UserNavigationService;
import com.empay.user_management.utils.JwtUtils;

/**
 * Unit tests for UserNavigationController.
 */
@ExtendWith(MockitoExtension.class)
class UserNavigationControllerTest {

	@Mock
	private UserNavigationService userNavigationService;

	@Mock
	private JwtUtils jwtUtils;

	@InjectMocks
	private UserNavigationController userNavigationController;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void testGetMyMenu_Success() {
		String token = "sample_token";
		when(jwtUtils.getUserIdFromToken(token)).thenReturn("admin");

		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"admin",
				"System Administrator",
				List.of("SYS_ADMIN"),
				List.of(new ModuleMenuNode("ADMIN", "Admin", "settings", List.of()))
		);

		when(userNavigationService.getUserNavigationMenu("admin")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> responseEntity = userNavigationController.getMyMenu("Bearer " + token, null);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals("admin", responseEntity.getBody().userId());
		verify(userNavigationService).getUserNavigationMenu("admin");
	}

	@Test
	void testGetMyMenu_Unauthenticated_ReturnsUnauthorized() {
		ResponseEntity<UserNavigationResponse> responseEntity = userNavigationController.getMyMenu(null, null);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
	}

	@Test
	void testGetMyMenu_WithSecurityContextAuthentication() {
		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken("custom_officer", null, List.of());
		SecurityContextHolder.getContext().setAuthentication(auth);

		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"custom_officer",
				"Custom Officer",
				List.of("OFFICER_ROLE"),
				List.of()
		);

		when(userNavigationService.getUserNavigationMenu("custom_officer")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> responseEntity = userNavigationController.getMyMenu(null, null);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals("custom_officer", responseEntity.getBody().userId());
		verify(userNavigationService).getUserNavigationMenu("custom_officer");
	}

	@Test
	void testGetMyMenu_WithValidBearerToken() {
		String validToken = "valid.jwt.token";
		when(jwtUtils.getUserIdFromToken(validToken)).thenReturn("teller_user");

		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"teller_user",
				"Teller User",
				List.of("TELLER_ROLE"),
				List.of()
		);

		when(userNavigationService.getUserNavigationMenu("teller_user")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> responseEntity =
				userNavigationController.getMyMenu("Bearer " + validToken, null);

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals("teller_user", responseEntity.getBody().userId());
		verify(userNavigationService).getUserNavigationMenu("teller_user");
	}

	@Test
	void testGetUserMenuPreview_Success() {
		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"test_user",
				"Test User",
				List.of("SYS_ADMIN"),
				List.of()
		);

		when(userNavigationService.getUserNavigationMenu("test_user")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> responseEntity = userNavigationController.getUserMenuPreview("test_user");

		assertNotNull(responseEntity);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals("test_user", responseEntity.getBody().userId());
		verify(userNavigationService).getUserNavigationMenu("test_user");
	}

	@Test
	void testSingleArgConstructor() {
		UserNavigationController controller = new UserNavigationController(userNavigationService);
		assertNotNull(controller);
	}

	@Test
	void testGetMyMenu_WithDirectEyJToken() {
		String eyJToken = "eyJhbGciOiJIUzI1NiJ9.userPayload.signature";
		when(jwtUtils.getUserIdFromToken(eyJToken)).thenReturn("jwt_user");

		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"jwt_user", "JWT User", List.of("ROLE_USER"), List.of());
		when(userNavigationService.getUserNavigationMenu("jwt_user")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> response = userNavigationController.getMyMenu(eyJToken, null, null, null);
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("jwt_user", response.getBody().userId());
	}

	@Test
	void testGetMyMenu_WithAccessTokenCookie() {
		String cookieVal = "access_token_cookie_val";
		when(jwtUtils.getUserIdFromToken(cookieVal)).thenReturn("cookie_user");

		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"cookie_user", "Cookie User", List.of("ROLE_USER"), List.of());
		when(userNavigationService.getUserNavigationMenu("cookie_user")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> response = userNavigationController.getMyMenu(null, null, cookieVal, null);
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("cookie_user", response.getBody().userId());
	}

	@Test
	void testGetMyMenu_WithCookieToken() {
		String cookieVal = "auth_token_cookie_val";
		when(jwtUtils.getUserIdFromToken(cookieVal)).thenReturn("auth_cookie_user");

		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"auth_cookie_user", "Auth Cookie User", List.of("ROLE_USER"), List.of());
		when(userNavigationService.getUserNavigationMenu("auth_cookie_user")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> response = userNavigationController.getMyMenu(null, cookieVal, null, null);
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("auth_cookie_user", response.getBody().userId());
	}

	@Test
	void testGetMyMenu_WithRequestToken() {
		HttpServletRequest mockReq = mock(HttpServletRequest.class);
		when(mockReq.getHeader("Authorization")).thenReturn("Bearer token_from_req");
		when(jwtUtils.getUserIdFromToken("token_from_req")).thenReturn("req_user");

		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"req_user", "Req User", List.of("ROLE_USER"), List.of());
		when(userNavigationService.getUserNavigationMenu("req_user")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> response = userNavigationController.getMyMenu(null, null, null, mockReq);
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("req_user", response.getBody().userId());
	}

	@Test
	void testGetMyMenu_AnonymousUserInSecurityContext_FallsBackToToken() {
		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
		SecurityContextHolder.getContext().setAuthentication(auth);

		when(jwtUtils.getUserIdFromToken("token_anon")).thenReturn("real_user");
		UserNavigationResponse mockResponse = new UserNavigationResponse(
				"real_user", "Real User", List.of("ROLE_USER"), List.of());
		when(userNavigationService.getUserNavigationMenu("real_user")).thenReturn(mockResponse);

		ResponseEntity<UserNavigationResponse> response = userNavigationController.getMyMenu("Bearer token_anon", null);
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("real_user", response.getBody().userId());
	}
}

