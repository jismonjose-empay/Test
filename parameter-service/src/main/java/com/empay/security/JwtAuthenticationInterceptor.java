package com.empay.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.empay.parameterservice.utils.JwtUtils;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

	private final JwtUtils jwtUtils;
	private final CustomUserDetailsService userDetailsService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {

		// Clear any previous authentication for safety
		SecurityContextHolder.clearContext();
		// Skip CORS preflight requests
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		// Only process controller methods
		if (!(handler instanceof HandlerMethod)) {
			return true;
		}
		// Extract JWT from cookies
		String token = jwtUtils.getAccessTokenFromRequest(request);
		if (token == null || token.isBlank()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token is missing");
			return false;
		}

		try {
			// Step 1: Validate JWT
			if (!jwtUtils.validateToken(token)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
				return false;
			}

			// Step 2: Extract user details from JWT
			String userId = jwtUtils.getUserIdFromToken(token);
			Integer serialNumber = jwtUtils.getUserSerNumberFromToken(token);
			if (userId == null || userId.isBlank() || serialNumber == null) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User information not found in JWT");
				return false;
			}

			// Step 3: Load user and permissions from database
			UserDetails userDetails = userDetailsService.loadUserBySerialNumber(serialNumber);
			// Step 4: Verify both JWT claims belong to the same user
			if (!userDetails.getUsername().equalsIgnoreCase(userId)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid user information in JWT");
				return false;
			}

			// Step 5: Create authenticated user
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
					null, userDetails.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			// Step 6: Store authentication for this request
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			return true;
		} catch (UsernameNotFoundException | JwtException e) {
			SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User authentication failed");
			return false;

		} catch (Exception e) {
			SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process authentication");
			return false;
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		// Remove authentication after request completion
		SecurityContextHolder.clearContext();
	}
}