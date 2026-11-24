package com.empay.user_management.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

class JwtUtilsTest {

	private static final String TEST_SECRET = "V7cExLF9Q9VHbpiPofCdRW3nDBj264cPOZ87pmrIb+KKloxi2zSezBqnRip5D7M9ridG8beE6KFG5jc7JFT/4A==";

	private JwtUtils jwtUtils;

	@BeforeEach
	void setUp() {
		jwtUtils = new JwtUtils();
		ReflectionTestUtils.setField(jwtUtils, "secret", TEST_SECRET);
	}

	@ParameterizedTest(name = "Header: {0} -> Expected: {1}")
	@CsvSource({
		"Bearer test.jwt.token, test.jwt.token",
		"Bearer \"quoted.jwt.token\", quoted.jwt.token",
		"eyJhbGciOiJIUzI1NiJ9.sample.signature, eyJhbGciOiJIUzI1NiJ9.sample.signature"
	})
	@DisplayName("Should extract token from Authorization header variants")
	void testGetAccessTokenFromRequest_AuthorizationHeaderVariants(String headerValue, String expectedToken) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(headerValue);

		String token = JwtUtils.getAccessTokenFromRequest(request);

		assertEquals(expectedToken, token);
	}

	@Test
	@DisplayName("Should extract token from X-Auth-Token header when Authorization header is absent")
	void testGetAccessTokenFromRequest_XAuthTokenHeader() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
		when(request.getHeader("X-Auth-Token")).thenReturn("x.auth.token.value");

		String token = JwtUtils.getAccessTokenFromRequest(request);

		assertEquals("x.auth.token.value", token);
	}

	@ParameterizedTest(name = "Cookie: {0}={1} -> Expected: {2}")
	@CsvSource({
		"ACCESS_TOKEN, cookie.jwt.token, cookie.jwt.token",
		"ACCESS_TOKEN, '\"cookie.jwt.token\"', cookie.jwt.token",
		"AUTH_TOKEN, legacy.auth.cookie, legacy.auth.cookie"
	})
	@DisplayName("Should extract token from Cookie variants")
	void testGetAccessTokenFromRequest_CookieVariants(String cookieName, String cookieValue, String expectedToken) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
		when(request.getHeader("X-Auth-Token")).thenReturn(null);
		Cookie cookie = new Cookie(cookieName, cookieValue);
		when(request.getCookies()).thenReturn(new Cookie[]{cookie});

		String token = JwtUtils.getAccessTokenFromRequest(request);

		assertEquals(expectedToken, token);
	}

	@Test
	@DisplayName("Should return null when request or tokens are absent")
	void testGetAccessTokenFromRequest_NullCases() {
		assertNull(JwtUtils.getAccessTokenFromRequest(null));

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
		when(request.getHeader("X-Auth-Token")).thenReturn(null);
		when(request.getCookies()).thenReturn(null);

		assertNull(JwtUtils.getAccessTokenFromRequest(request));
	}

	@Test
	@DisplayName("Should generate, validate, and parse token for user")
	void testGenerateValidateAndExtractToken() {
		String userId = "admin";
		String token = jwtUtils.generateToken(userId);

		assertNotNull(token);
		assertTrue(jwtUtils.validateToken(token));
		assertEquals(userId, jwtUtils.getUserIdFromToken(token));
	}

	@Test
	@DisplayName("Should extract userId from request containing Authorization Bearer token")
	void testGetUserId_FromRequestHeader() {
		String userId = "superadmin";
		String token = jwtUtils.generateToken(userId);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

		String extractedUserId = jwtUtils.getUserId(request);

		assertEquals(userId, extractedUserId);
	}

	@Test
	@DisplayName("Should validateToken return false on invalid token")
	void testValidateToken_Invalid() {
		assertFalse(jwtUtils.validateToken(null));
		assertFalse(jwtUtils.validateToken(""));
		assertFalse(jwtUtils.validateToken("invalid.token.here"));
	}

	@Test
	@DisplayName("Should generate token with userRole and userSerialNumber and extract them")
	void testGenerateTokenWithUserRoleAndSerialNo() {
		String userId = "officer_1";
		Integer serialNo = 205;
		String userRole = "LOAN_OFFICER";

		String token = jwtUtils.generateToken(userId, serialNo, userRole);

		assertNotNull(token);
		assertTrue(jwtUtils.validateToken(token));
		assertEquals(userId, jwtUtils.getUserIdFromToken(token));
		assertEquals(userRole, jwtUtils.getUserRoleFromToken(token));

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

		assertEquals(userId, jwtUtils.getUserId(request));
		assertEquals(userRole, jwtUtils.getUserRole(request));
		assertEquals(serialNo, jwtUtils.getUserSerNumber(request));
	}

	@Test
	@DisplayName("Should return null role when token generated without role")
	void testGenerateTokenWithoutRole_ReturnsNullRole() {
		String token = jwtUtils.generateToken("plain_user");

		assertNotNull(token);
		assertTrue(jwtUtils.validateToken(token));
		assertNull(jwtUtils.getUserRoleFromToken(token));

		String twoArgToken = jwtUtils.generateToken("plain_user", 101);
		assertNotNull(twoArgToken);
		assertTrue(jwtUtils.validateToken(twoArgToken));

		assertNull(jwtUtils.getUserIdFromToken(null));
		assertNull(jwtUtils.getUserIdFromToken(""));
		assertNull(jwtUtils.getUserRoleFromToken(null));
		assertNull(jwtUtils.getUserRoleFromToken(""));
		assertNull(jwtUtils.getUserSerNumber(mock(HttpServletRequest.class)));
		assertNull(jwtUtils.getUserRole(mock(HttpServletRequest.class)));
	}

	@Test
	@DisplayName("Should extract institution code from cookie or return null on invalid/missing")
	void testGetInstitutionCode() {
		HttpServletRequest nullCookiesRequest = mock(HttpServletRequest.class);
		when(nullCookiesRequest.getCookies()).thenReturn(null);
		assertNull(jwtUtils.getInstitutionCode(nullCookiesRequest));

		HttpServletRequest validRequest = mock(HttpServletRequest.class);
		when(validRequest.getCookies()).thenReturn(new Cookie[]{new Cookie(APIConstants.INS_CODE, "1001")});
		assertEquals(1001, jwtUtils.getInstitutionCode(validRequest));

		HttpServletRequest invalidNumberRequest = mock(HttpServletRequest.class);
		when(invalidNumberRequest.getCookies()).thenReturn(new Cookie[]{new Cookie(APIConstants.INS_CODE, "invalid")});
		assertNull(jwtUtils.getInstitutionCode(invalidNumberRequest));

		HttpServletRequest otherCookieRequest = mock(HttpServletRequest.class);
		when(otherCookieRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("OTHER_COOKIE", "123")});
		assertNull(jwtUtils.getInstitutionCode(otherCookieRequest));
	}

	@Test
	@DisplayName("Should extract group serial number from cookie or return null on invalid/missing")
	void testGetGrpSerialNumber() {
		HttpServletRequest nullCookiesRequest = mock(HttpServletRequest.class);
		when(nullCookiesRequest.getCookies()).thenReturn(null);
		assertNull(jwtUtils.getGrpSerialNumber(nullCookiesRequest));

		HttpServletRequest validRequest = mock(HttpServletRequest.class);
		when(validRequest.getCookies()).thenReturn(new Cookie[]{new Cookie(APIConstants.GRP_SER_NUMBER, "5002")});
		assertEquals(5002, jwtUtils.getGrpSerialNumber(validRequest));

		HttpServletRequest invalidNumberRequest = mock(HttpServletRequest.class);
		when(invalidNumberRequest.getCookies()).thenReturn(new Cookie[]{new Cookie(APIConstants.GRP_SER_NUMBER, "not_a_number")});
		assertNull(jwtUtils.getGrpSerialNumber(invalidNumberRequest));

		HttpServletRequest otherCookieRequest = mock(HttpServletRequest.class);
		when(otherCookieRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("OTHER_COOKIE", "5002")});
		assertNull(jwtUtils.getGrpSerialNumber(otherCookieRequest));
	}

	@Test
	@DisplayName("Should return null for literal 'null' and 'undefined' tokens in headers and cookies")
	void testStripSurroundingQuotes_NullAndUndefinedStrings() {
		HttpServletRequest request1 = mock(HttpServletRequest.class);
		when(request1.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer \"null\"");
		assertNull(JwtUtils.getAccessTokenFromRequest(request1));

		HttpServletRequest request2 = mock(HttpServletRequest.class);
		when(request2.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer \"undefined\"");
		assertNull(JwtUtils.getAccessTokenFromRequest(request2));

		HttpServletRequest request3 = mock(HttpServletRequest.class);
		when(request3.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");
		assertNull(JwtUtils.getAccessTokenFromRequest(request3));

		HttpServletRequest request4 = mock(HttpServletRequest.class);
		when(request4.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
		when(request4.getHeader("X-Auth-Token")).thenReturn(null);
		when(request4.getCookies()).thenReturn(new Cookie[]{new Cookie(APIConstants.ACCESS_TOKEN_COOKIE, "\"null\"")});
		assertNull(JwtUtils.getAccessTokenFromRequest(request4));
	}

	@Test
	@DisplayName("Should handle corrupted token when extracting userRole")
	void testGetUserRoleFromToken_CorruptedToken() {
		assertNull(jwtUtils.getUserRoleFromToken("corrupted.token.signature"));
	}
}

