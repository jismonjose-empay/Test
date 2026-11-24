package com.empay.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

	@Mock
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Test
	@DisplayName("corsConfigurationSource configures default allowed origins when custom property is empty")
	void testCorsConfigurationSource_DefaultOrigins() {
		SecurityConfig config = new SecurityConfig(jwtAuthenticationFilter);
		ReflectionTestUtils.setField(config, "customCorsOrigins", null);

		UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
		assertNotNull(source);

		CorsConfiguration corsConfig = source.getCorsConfigurations().get("/**");
		assertNotNull(corsConfig);
		List<String> originPatterns = corsConfig.getAllowedOriginPatterns();
		assertNotNull(originPatterns);
		assertTrue(originPatterns.contains("http://localhost"));
		assertTrue(originPatterns.contains("http://localhost:*"));
		assertTrue(originPatterns.contains("https://127.0.0.1:*"));
		assertTrue(corsConfig.getAllowCredentials());
		assertTrue(corsConfig.getAllowedMethods().contains("GET"));
		assertTrue(corsConfig.getAllowedMethods().contains("POST"));
		assertTrue(corsConfig.getExposedHeaders().contains("X-XSRF-TOKEN"));
		assertEquals(3600L, corsConfig.getMaxAge());
	}

	@Test
	@DisplayName("corsConfigurationSource parses custom origins when provided")
	void testCorsConfigurationSource_CustomOrigins() {
		SecurityConfig config = new SecurityConfig(jwtAuthenticationFilter);
		ReflectionTestUtils.setField(config, "customCorsOrigins", "http://empay.com, https://secure.empay.com,  ");

		UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
		assertNotNull(source);

		CorsConfiguration corsConfig = source.getCorsConfigurations().get("/**");
		assertNotNull(corsConfig);
		List<String> originPatterns = corsConfig.getAllowedOriginPatterns();
		assertNotNull(originPatterns);
		assertEquals(2, originPatterns.size());
		assertTrue(originPatterns.contains("http://empay.com"));
		assertTrue(originPatterns.contains("https://secure.empay.com"));
	}

	@Test
	@DisplayName("CsrfCookieFilter loads deferred CSRF token when present")
	void testCsrfCookieFilter_LoadsDeferredToken() throws ServletException, IOException {
		SecurityConfig.CsrfCookieFilter filter = new SecurityConfig.CsrfCookieFilter();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		CsrfToken csrfToken = mock(CsrfToken.class);
		request.setAttribute(CsrfToken.class.getName(), csrfToken);

		filter.doFilter(request, response, filterChain);

		verify(csrfToken).getToken();
	}

	@Test
	@DisplayName("CsrfCookieFilter passes request when CSRF token attribute is missing")
	void testCsrfCookieFilter_WithoutToken() throws ServletException, IOException {
		SecurityConfig.CsrfCookieFilter filter = new SecurityConfig.CsrfCookieFilter();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		filter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
	}
}

