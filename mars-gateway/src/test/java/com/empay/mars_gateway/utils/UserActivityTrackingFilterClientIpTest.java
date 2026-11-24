package com.empay.mars_gateway.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class UserActivityTrackingFilterClientIpTest {

	private UserActivityTrackingFilter filter;

	@BeforeEach
	void setUp() {
		WebClient webClient = mock(WebClient.class);
		filter = new UserActivityTrackingFilter(webClient);
	}

	@Test
	void resolveClientAddress_extractsHostFromRemoteAddress() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
				.remoteAddress(new InetSocketAddress("203.0.113.195", 54321))
				.build();

		String ip = filter.resolveClientAddress(request);
		assertEquals("203.0.113.195", ip);
	}

	@Test
	void resolveClientAddress_returnsNullWhenRemoteAddressNull() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();

		String ip = filter.resolveClientAddress(request);
		assertNull(ip);
	}

	@Test
	void filter_stripsInboundSpoofedHeaders_andInjectsRealClientAddress() {
		// Untrusted client attempts to spoof X-Forwarded-For and X-Real-IP
		MockServerHttpRequest incomingRequest = MockServerHttpRequest.get("/api/users/v2/get-users")
				.header("X-Forwarded-For", "spoofed.client.ip")
				.header("X-Real-IP", "spoofed.real.ip")
				.remoteAddress(new InetSocketAddress("203.0.113.50", 12345))
				.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(incomingRequest);

		AtomicReference<ServerWebExchange> downstreamExchangeRef = new AtomicReference<>();
		GatewayFilterChain chain = mutatedExchange -> {
			downstreamExchangeRef.set(mutatedExchange);
			return Mono.empty();
		};

		filter.filter(exchange, chain).block();

		ServerWebExchange downstream = downstreamExchangeRef.get();
		// Inbound spoofed values must be stripped and replaced with genuine TCP remote address
		assertEquals("203.0.113.50", downstream.getRequest().getHeaders().getFirst(UserActivityTrackingFilter.X_FORWARDED_FOR));
		assertNull(downstream.getRequest().getHeaders().getFirst(UserActivityTrackingFilter.X_REAL_IP));
	}
}
