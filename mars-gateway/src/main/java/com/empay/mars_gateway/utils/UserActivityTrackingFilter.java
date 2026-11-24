package com.empay.mars_gateway.utils;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.empay.mars_gateway.valueobjects.UserActivityTrackingVO;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserActivityTrackingFilter implements GlobalFilter, Ordered {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserActivityTrackingFilter.class);

	private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
			"(?i).*(password|passwd|pwd|secret|token|hash|key|credential|pin|cvv|auth).*"
	);

	public static final String X_FORWARDED_FOR = "X-Forwarded-For";
	public static final String X_REAL_IP = "X-Real-IP";

	private final WebClient webClient;

	@Value("${user.activity.url}")
	private String activityUrl;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getURI().getPath();
		if (path != null && path.contains("save-user-activity")) {
			return chain.filter(exchange);
		}

		String clientIp = resolveClientAddress(request);

		// Strip inbound spoofable client IP headers and inject genuine client IP
		ServerHttpRequest mutatedRequest = request.mutate()
				.headers(headers -> {
					headers.remove(X_FORWARDED_FOR);
					headers.remove(X_REAL_IP);
					if (clientIp != null && !clientIp.isBlank()) {
						headers.set(X_FORWARDED_FOR, clientIp);
					}
				})
				.build();

		ServerWebExchange mutatedExchange = exchange.mutate()
				.request(mutatedRequest)
				.build();

		return chain.filter(mutatedExchange)
				.then(Mono.fromRunnable(() -> trackActivity(mutatedExchange, clientIp)));
	}

	private void trackActivity(ServerWebExchange exchange, String clientIp) {
		try {
			ServerHttpRequest request = exchange.getRequest();
			String token = resolveToken(request);

			if (token == null || token.isBlank()) {
				return;
			}

			String path = request.getURI().getPath();
			String method = request.getMethod().name();
			String permissionCode = resolvePermissionCode(method, path);
			String eventType = (permissionCode != null) ? permissionCode : method;
			String resourceType = resolveResourceType(permissionCode);
			String resourceId = resolveResourceId(request, resourceType);
			String detail = resolveDetail(request, resourceId);

			HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
			int code = (statusCode != null) ? statusCode.value() : 200;
			HttpStatus httpStatus = HttpStatus.resolve(code);
			String reasonPhrase = (httpStatus != null) ? httpStatus.getReasonPhrase() : "";
			String responseDetail = "HTTP " + code + (reasonPhrase.isBlank() ? "" : " " + reasonPhrase);

			if (code >= 400) {
				LOGGER.warn("HTTP {} on {} {}", code, method, path);
			}

			UserActivityTrackingVO vo = new UserActivityTrackingVO();
			vo.setCorrelationId(UUID.randomUUID().toString());
			vo.setEventType(eventType);
			vo.setResourceType(resourceType);
			vo.setResourceId(resourceId);
			vo.setDetail(detail);
			vo.setSourceIp(clientIp);
			vo.setResponseStatus(code);
			vo.setResponse(truncate(responseDetail, 1000));
			vo.setResponseDetail(truncate(responseDetail, 1000));

			var requestSpec = webClient.post()
					.uri(activityUrl)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.cookie(APIConstants.ACCESS_TOKEN_COOKIE, token);

			if (clientIp != null && !clientIp.isBlank()) {
				requestSpec.header(X_FORWARDED_FOR, clientIp);
			}

			requestSpec.bodyValue(vo)
					.retrieve()
					.bodyToMono(Void.class)
					.onErrorResume(e -> {
						LOGGER.error("Failed to save user activity: {}", e.getMessage());
						return Mono.empty();
					})
					.subscribe();

		} catch (Exception e) {
			LOGGER.error("Failed to track user activity", e);
		}
	}

	String resolveResponseDetail(HttpStatusCode statusCode) {
		int code = (statusCode != null) ? statusCode.value() : 200;
		HttpStatus httpStatus = HttpStatus.resolve(code);
		String reasonPhrase = (httpStatus != null) ? httpStatus.getReasonPhrase() : "";
		return "HTTP " + code + (reasonPhrase.isBlank() ? "" : " " + reasonPhrase);
	}

	String resolveClientAddress(ServerHttpRequest request) {
		if (request == null) {
			return null;
		}
		InetSocketAddress remoteAddress = request.getRemoteAddress();
		if (remoteAddress != null && remoteAddress.getAddress() != null) {
			return remoteAddress.getAddress().getHostAddress();
		}
		return null;
	}

	String resolvePermissionCode(String method, String path) {
		if (path == null || path.isBlank()) {
			return null;
		}
		if (path.contains("/api/params/")) {
			return ParameterPermissionUtils.resolvePermissionCode(method, path);
		}
		return UserManagementPermissionUtils.resolvePermissionCode(method, path);
	}

	String resolveResourceType(String permissionCode) {
		if (permissionCode != null && !permissionCode.isBlank()) {
			String[] parts = permissionCode.split("\\.");
			if (parts.length >= 2) {
				return parts[1];
			}
		}
		return "ENDPOINT";
	}

	private String resolveResourceId(ServerHttpRequest request, String resourceType) {
		MultiValueMap<String, String> queryParams = request.getQueryParams();

		if ("USER".equalsIgnoreCase(resourceType) || "UM_USERS".equalsIgnoreCase(resourceType)) {
			for (String param : new String[]{"userId", "user_id", "username", "targetUserId", "user", "id"}) {
				String val = queryParams.getFirst(param);
				if (val != null && !val.isBlank()) {
					return truncate(val.trim(), 100);
				}
			}
		} else if ("ROLE".equalsIgnoreCase(resourceType) || "UM_ROLES".equalsIgnoreCase(resourceType)) {
			for (String param : new String[]{"roleId", "role_id", "role", "profileId", "profile_id", "profileName", "id", "code"}) {
				String val = queryParams.getFirst(param);
				if (val != null && !val.isBlank()) {
					return truncate(val.trim(), 100);
				}
			}
		} else if ("POLICY".equalsIgnoreCase(resourceType) || "UM_PASSWORD_POLICY".equalsIgnoreCase(resourceType)) {
			for (String param : new String[]{"policyId", "policyCode", "policy_code", "policy", "code", "id"}) {
				String val = queryParams.getFirst(param);
				if (val != null && !val.isBlank()) {
					return truncate(val.trim(), 100);
				}
			}
		}

		for (String param : new String[]{"id", "resourceId", "code", "name"}) {
			String val = queryParams.getFirst(param);
			if (val != null && !val.isBlank()) {
				return truncate(val.trim(), 100);
			}
		}

		String path = request.getURI().getPath();
		if (path != null) {
			String[] segments = path.split("/");
			if (segments.length > 0) {
				String last = segments[segments.length - 1].trim();
				if (!last.isBlank() && !isActionVerb(last)) {
					return truncate(last, 100);
				}
			}
		}

		if ("ENDPOINT".equals(resourceType) && path != null) {
			return truncate(path, 100);
		}

		return null;
	}

	private String resolveDetail(ServerHttpRequest request, String resourceId) {
		String method = request.getMethod().name();
		String path = request.getURI().getPath();

		StringBuilder detail = new StringBuilder();
		detail.append(method).append(" ").append(path);

		String sanitizedQuery = sanitizeQueryParams(request.getQueryParams());
		if (!sanitizedQuery.isBlank()) {
			detail.append("?").append(sanitizedQuery);
		}

		if (resourceId != null && !resourceId.isBlank() && (path == null || !path.contains(resourceId))) {
			detail.append(" [target=").append(resourceId).append("]");
		}

		String sanitized = sanitizeText(detail.toString());
		return truncate(sanitized, 1000);
	}

	String sanitizeQueryParams(MultiValueMap<String, String> queryParams) {
		if (queryParams == null || queryParams.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		queryParams.forEach((key, values) -> {
			if (sb.length() > 0) sb.append("&");
			sb.append(key).append("=");
			if (SENSITIVE_KEY_PATTERN.matcher(key).matches()) {
				sb.append("[REDACTED]");
			} else {
				sb.append(String.join(",", values));
			}
		});
		return sb.toString();
	}

	String sanitizeText(String text) {
		if (text == null) return "";
		// Never include raw bearer tokens
		text = text.replaceAll("(?i)(bearer\\s+)[a-zA-Z0-9._\\-]+", "$1[REDACTED]");
		// Never include raw JWT tokens
		text = text.replaceAll("ey[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+", "[REDACTED_JWT]");
		return text;
	}

	private boolean isActionVerb(String segment) {
		String lower = segment.toLowerCase();
		return lower.startsWith("get-") || lower.startsWith("add-") || lower.startsWith("edit-")
				|| lower.startsWith("delete-") || lower.startsWith("save-") || lower.startsWith("update-")
				|| lower.startsWith("find-") || lower.equals("login") || lower.equals("logout")
				|| lower.equals("users") || lower.equals("params") || lower.equals("auth");
	}

	private String truncate(String val, int maxLen) {
		if (val == null) return null;
		return val.length() <= maxLen ? val : val.substring(0, maxLen);
	}

	String resolveToken(ServerHttpRequest request) {
		String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (auth != null && !auth.isBlank()) {
			return auth.replaceFirst("(?i)^Bearer ", "").replace("\"", "").trim();
		}

		for (String cookieName : new String[]{APIConstants.ACCESS_TOKEN_COOKIE, "access_token", "accessToken", "token"}) {
			HttpCookie cookie = request.getCookies().getFirst(cookieName);
			if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
				String token = cookie.getValue().replace("\"", "").trim();
				return token.replaceFirst("(?i)^Bearer ", "").trim();
			}
		}
		return null;
	}

	@Override
	public int getOrder() {
		return -100;
	}
}
