package com.empay.mars_gateway.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class UserActivityTrackingFilterTokenResolutionTest {

	private UserActivityTrackingFilter filter;

	@BeforeEach
	void setUp() {
		WebClient webClient = mock(WebClient.class);
		filter = new UserActivityTrackingFilter(webClient);
	}

	@Test
	void resolveToken_readsFromAuthorizationHeader() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
				.header(HttpHeaders.AUTHORIZATION, "Bearer test-jwt-token-from-header")
				.build();

		String token = filter.resolveToken(request);
		assertEquals("test-jwt-token-from-header", token);
	}

	@Test
	void resolveToken_readsFromAccessTokenCookie_whenHeaderMissing() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
				.cookie(new HttpCookie(APIConstants.ACCESS_TOKEN_COOKIE, "test-jwt-token-from-cookie"))
				.build();

		String token = filter.resolveToken(request);
		assertEquals("test-jwt-token-from-cookie", token);
	}

	@Test
	void resolveToken_stripsQuotesAndBearerPrefixFromCookie() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
				.cookie(new HttpCookie(APIConstants.ACCESS_TOKEN_COOKIE, "\"Bearer quoted-token\""))
				.build();

		String token = filter.resolveToken(request);
		assertEquals("quoted-token", token);
	}

	@Test
	void resolveToken_prefersAuthorizationHeaderWhenBothPresent() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
				.header(HttpHeaders.AUTHORIZATION, "Bearer header-token")
				.cookie(new HttpCookie(APIConstants.ACCESS_TOKEN_COOKIE, "cookie-token"))
				.build();

		String token = filter.resolveToken(request);
		assertEquals("header-token", token);
	}

	@Test
	void resolveToken_returnsNullWhenNeitherHeaderNorCookiePresent() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();

		String token = filter.resolveToken(request);
		assertNull(token);
	}
}
