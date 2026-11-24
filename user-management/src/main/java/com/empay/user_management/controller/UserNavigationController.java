package com.empay.user_management.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.dto.UserNavigationResponse;
import com.empay.user_management.service.UserNavigationService;
import com.empay.user_management.utils.APIConstants;
import com.empay.user_management.utils.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for retrieving permission-driven dynamic navigation menus.
 * Implements the Zero-Table-Addition Menu Generation Architecture.
 */
@RestController
@RequestMapping("/api/navigation")
public class UserNavigationController {

	private static final Logger log = LoggerFactory.getLogger(UserNavigationController.class);
	private static final String BEARER_PREFIX = "Bearer ";

	private final UserNavigationService userNavigationService;
	private final JwtUtils jwtUtils;

	/**
	 * Explicit constructor for dependency injection and tests.
	 *
	 * @param userNavigationService navigation business service
	 */
	public UserNavigationController(UserNavigationService userNavigationService) {
		this(userNavigationService, null);
	}

	/**
	 * Constructor with optional JwtUtils injection.
	 *
	 * @param userNavigationService navigation business service
	 * @param jwtUtils JWT utility service
	 */
	@Autowired
	public UserNavigationController(UserNavigationService userNavigationService,
			@Autowired(required = false) JwtUtils jwtUtils) {
		this.userNavigationService = userNavigationService;
		this.jwtUtils = jwtUtils;
	}

	/**
	 * Retrieves the dynamic navigation menu hierarchy for the authenticated user.
	 * Requires authentication; extracts caller identity strictly from the verified token or SecurityContext.
	 *
	 * @param authHeader optional Bearer token header
	 * @param cookieToken optional AUTH_TOKEN cookie
	 * @param accessTokenCookie optional ACCESS_TOKEN cookie
	 * @param request optional HTTP servlet request
	 * @return dynamic navigation response
	 */
	@PreAuthorize("isAuthenticated()")
	@GetMapping({"/menu", "/menus"})
	public ResponseEntity<UserNavigationResponse> getMyMenu(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
			@CookieValue(value = "AUTH_TOKEN", required = false) String cookieToken,
			@CookieValue(value = APIConstants.ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
			HttpServletRequest request) {

		log.debug("GET /api/navigation/menu requested. AuthHeader present: {}, Cookie present: {}",
				authHeader != null, (cookieToken != null || accessTokenCookie != null));

		String effectiveUserId = resolveEffectiveUserId(authHeader, cookieToken, accessTokenCookie, request);

		if (effectiveUserId == null || effectiveUserId.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		return ResponseEntity.ok(userNavigationService.getUserNavigationMenu(effectiveUserId));
	}

	/**
	 * Overloaded getMyMenu method for backwards compatibility with existing unit tests.
	 */
	public ResponseEntity<UserNavigationResponse> getMyMenu(String authHeader, String cookieToken) {
		return getMyMenu(authHeader, cookieToken, null, null);
	}

	private String resolveEffectiveUserId(String authHeader, String cookieToken, String accessTokenCookie,
			HttpServletRequest request) {
		// 1. Check Spring SecurityContextHolder (populated by JwtAuthenticationFilter)
		String contextUser = resolveUserFromSecurityContext();
		if (contextUser != null) {
			return contextUser;
		}

		// 2. Resolve token from header, cookies, or request
		String token = resolveTokenFromInputs(authHeader, cookieToken, accessTokenCookie, request);
		if (token != null && jwtUtils != null) {
			String tokenUser = jwtUtils.getUserIdFromToken(token);
			if (tokenUser != null && !tokenUser.isBlank()) {
				return tokenUser;
			}
		}

		return null;
	}

	private String resolveUserFromSecurityContext() {
		try {
			var auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
				return auth.getName();
			}
		} catch (Exception ex) {
			log.debug("Could not resolve user from SecurityContextHolder: {}", ex.getMessage());
		}
		return null;
	}

	private String resolveTokenFromInputs(String authHeader, String cookieToken, String accessTokenCookie,
			HttpServletRequest request) {
		String token = extractTokenFromHeader(authHeader);
		if (token != null) {
			return token;
		}

		if (accessTokenCookie != null && !accessTokenCookie.isBlank()) {
			return accessTokenCookie.trim();
		}
		if (cookieToken != null && !cookieToken.isBlank()) {
			return cookieToken.trim();
		}

		if (request != null) {
			return JwtUtils.getAccessTokenFromRequest(request);
		}

		return null;
	}

	private String extractTokenFromHeader(String authHeader) {
		if (authHeader == null || authHeader.isBlank()) {
			return null;
		}
		String trimmed = authHeader.trim();
		if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return trimmed.substring(BEARER_PREFIX.length()).trim();
		}
		if (trimmed.startsWith("eyJ")) {
			return trimmed;
		}
		return null;
	}

	/**
	 * Admin user preview endpoint: retrieves menu hierarchy for a specified user or role.
	 *
	 * @param userId target user identifier
	 * @return dynamic navigation response
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_USERS.VIEW')")
	@GetMapping({"/menu/users/{userId}", "/menus/users/{userId}"})
	public ResponseEntity<UserNavigationResponse> getUserMenuPreview(@PathVariable String userId) {
		log.info("Admin preview requested for user navigation: {}", userId);
		return ResponseEntity.ok(userNavigationService.getUserNavigationMenu(userId));
	}
}
