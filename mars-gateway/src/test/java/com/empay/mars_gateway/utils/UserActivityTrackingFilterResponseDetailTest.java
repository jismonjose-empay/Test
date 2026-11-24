package com.empay.mars_gateway.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class UserActivityTrackingFilterResponseDetailTest {

	private UserActivityTrackingFilter filter;

	@BeforeEach
	void setUp() {
		WebClient webClient = mock(WebClient.class);
		filter = new UserActivityTrackingFilter(webClient);
	}

	@Test
	void resolveResponseDetail_200Ok() {
		String detail = filter.resolveResponseDetail(HttpStatus.OK);
		assertEquals("HTTP 200 OK", detail);
	}

	@Test
	void resolveResponseDetail_400BadRequest() {
		String detail = filter.resolveResponseDetail(HttpStatus.BAD_REQUEST);
		assertEquals("HTTP 400 Bad Request", detail);
	}

	@Test
	void resolveResponseDetail_401Unauthorized() {
		String detail = filter.resolveResponseDetail(HttpStatus.UNAUTHORIZED);
		assertEquals("HTTP 401 Unauthorized", detail);
	}

	@Test
	void resolveResponseDetail_403Forbidden() {
		String detail = filter.resolveResponseDetail(HttpStatus.FORBIDDEN);
		assertEquals("HTTP 403 Forbidden", detail);
	}

	@Test
	void resolveResponseDetail_404NotFound() {
		String detail = filter.resolveResponseDetail(HttpStatus.NOT_FOUND);
		assertEquals("HTTP 404 Not Found", detail);
	}

	@Test
	void resolveResponseDetail_500InternalServerError() {
		String detail = filter.resolveResponseDetail(HttpStatus.INTERNAL_SERVER_ERROR);
		assertEquals("HTTP 500 Internal Server Error", detail);
	}

	@Test
	void resolveResponseDetail_503ServiceUnavailable() {
		String detail = filter.resolveResponseDetail(HttpStatus.SERVICE_UNAVAILABLE);
		assertEquals("HTTP 503 Service Unavailable", detail);
	}

	@Test
	void resolveResponseDetail_nullStatusCodeDefaultsTo200Ok() {
		String detail = filter.resolveResponseDetail(null);
		assertEquals("HTTP 200 OK", detail);
	}
}
