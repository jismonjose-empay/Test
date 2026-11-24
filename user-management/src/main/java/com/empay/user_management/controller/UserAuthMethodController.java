package com.empay.user_management.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.dto.AuthProviderConfigDto;
import com.empay.user_management.dto.AuthProviderDto;
import com.empay.user_management.dto.ConfigureAuthMethodRequest;
import com.empay.user_management.dto.OAuthLoginRequest;
import com.empay.user_management.dto.SaveAuthProviderConfigRequest;
import com.empay.user_management.dto.UserAuthMethodsOverviewResponse;
import com.empay.user_management.entities.AuthProviderConfigEntity;
import com.empay.user_management.service.AuthProviderConfigService;
import com.empay.user_management.service.UserAuthMethodService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for managing system-wide Identity Provider / OAuth configurations,
 * user-specific authentication methods linking, and handling authentication endpoints.
 */
@RestController
@RequiredArgsConstructor
public class UserAuthMethodController {

	private static final Logger log = LoggerFactory.getLogger(UserAuthMethodController.class);

	private static final String DEFAULT_ACTOR = "ADMIN";
	private static final String IS_ENABLED = "isEnabled";
	private static final String MESSAGE = "message";
	private static final String PROVIDER = "provider";
	private static final String STATUS = "status";
	private static final String TARGET_URL = "targetUrl";

	private final UserAuthMethodService authMethodService;
	private final AuthProviderConfigService authProviderConfigService;

	// =========================================================================
	// 1. System-Level Auth Provider Configurations (Admin Only)
	// =========================================================================

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.VIEW')")
	@GetMapping("/api/admin/auth-providers")
	public ResponseEntity<List<AuthProviderConfigDto>> getAllProviderConfigs() {
		log.debug("GET /api/admin/auth-providers requested");
		return ResponseEntity.ok(authProviderConfigService.getAllProviderConfigs());
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.VIEW')")
	@GetMapping("/api/admin/auth-providers/{providerId}")
	public ResponseEntity<AuthProviderConfigDto> getProviderConfig(@PathVariable String providerId) {
		log.debug("GET /api/admin/auth-providers/{} requested", providerId);
		return ResponseEntity.ok(authProviderConfigService.getProviderConfig(providerId));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.CONFIG_PROVIDER')")
	@PostMapping("/api/admin/auth-providers")
	public ResponseEntity<AuthProviderConfigDto> createProviderConfig(
			@Valid @RequestBody SaveAuthProviderConfigRequest request,
			Principal principal) {
		String actor = principal != null ? principal.getName() : DEFAULT_ACTOR;
		log.info("POST /api/admin/auth-providers [{}] by actor [{}]", request.getProviderId(), actor);
		return ResponseEntity.ok(authProviderConfigService.saveProviderConfig(request, actor));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.CONFIG_PROVIDER')")
	@PutMapping("/api/admin/auth-providers/{providerId}")
	public ResponseEntity<AuthProviderConfigDto> updateProviderConfig(
			@PathVariable String providerId,
			@Valid @RequestBody SaveAuthProviderConfigRequest request,
			Principal principal) {
		String actor = principal != null ? principal.getName() : DEFAULT_ACTOR;
		request.setProviderId(providerId);
		log.info("PUT /api/admin/auth-providers/{} by actor [{}]", providerId, actor);
		return ResponseEntity.ok(authProviderConfigService.saveProviderConfig(request, actor));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.TOGGLE_PROVIDER')")
	@RequestMapping(value = "/api/admin/auth-providers/{providerId}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
	public ResponseEntity<AuthProviderConfigDto> toggleProviderStatus(
			@PathVariable String providerId,
			@RequestBody(required = false) Map<String, Object> body,
			@RequestParam(required = false) Boolean enabled,
			Principal principal) {
		String actor = principal != null ? principal.getName() : DEFAULT_ACTOR;
		if (enabled == null && (body == null || (!body.containsKey("enabled") && !body.containsKey(IS_ENABLED)))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required parameter 'enabled' or 'isEnabled' is missing");
		}
		boolean targetEnabled;
		if (enabled != null) {
			targetEnabled = enabled;
		} else if (body.containsKey(IS_ENABLED)) {
			targetEnabled = Boolean.TRUE.equals(body.get(IS_ENABLED));
		} else {
			targetEnabled = Boolean.TRUE.equals(body.get("enabled"));
		}
		log.info("Toggle /api/admin/auth-providers/{}/status to [{}] by actor [{}]", providerId, targetEnabled, actor);
		return ResponseEntity.ok(authProviderConfigService.toggleProviderStatus(providerId, targetEnabled, actor));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.DELETE_PROVIDER')")
	@DeleteMapping("/api/admin/auth-providers/{providerId}")
	public ResponseEntity<Map<String, Object>> deleteProviderConfig(
			@PathVariable String providerId,
			Principal principal) {
		String actor = principal != null ? principal.getName() : DEFAULT_ACTOR;
		log.info("DELETE /api/admin/auth-providers/{} by actor [{}]", providerId, actor);
		authProviderConfigService.deleteProviderConfig(providerId, actor);
		return ResponseEntity.ok(Map.of(MESSAGE, "Provider configuration [" + providerId + "] deleted successfully."));
	}

	// =========================================================================
	// 2. User-Specific Method Linking
	// =========================================================================

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.VIEW')")
	@GetMapping("/api/users/{userId}/auth-methods")
	public ResponseEntity<UserAuthMethodsOverviewResponse> getUserAuthMethods(@PathVariable String userId) {
		log.debug("GET /api/users/{}/auth-methods requested", userId);
		return ResponseEntity.ok(authMethodService.getUserAuthMethods(userId));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.CONFIGURE')")
	@PostMapping("/api/users/{userId}/auth-methods")
	public ResponseEntity<UserAuthMethodsOverviewResponse> configureUserAuthMethod(
			@PathVariable String userId,
			@Valid @RequestBody ConfigureAuthMethodRequest request,
			Principal principal) {
		String actor = principal != null ? principal.getName() : DEFAULT_ACTOR;
		log.info("POST /api/users/{}/auth-methods by actor [{}]", userId, actor);
		return ResponseEntity.ok(authMethodService.configureUserAuthMethod(userId, request, actor));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.CONFIGURE')")
	@PutMapping("/api/users/{userId}/auth-methods/{provider}")
	public ResponseEntity<UserAuthMethodsOverviewResponse> updateAuthMethod(
			@PathVariable String userId,
			@PathVariable String provider,
			@RequestBody ConfigureAuthMethodRequest request,
			Principal principal) {
		String actor = principal != null ? principal.getName() : DEFAULT_ACTOR;
		request.setProvider(provider);
		log.info("PUT /api/users/{}/auth-methods/{} by actor [{}]", userId, provider, actor);
		return ResponseEntity.ok(authMethodService.configureUserAuthMethod(userId, request, actor));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUTH_METHODS.UNLINK')")
	@DeleteMapping("/api/users/{userId}/auth-methods/{provider}")
	public ResponseEntity<UserAuthMethodsOverviewResponse> unlinkAuthMethod(
			@PathVariable String userId,
			@PathVariable String provider,
			Principal principal) {
		String actor = principal != null ? principal.getName() : DEFAULT_ACTOR;
		log.info("DELETE /api/users/{}/auth-methods/{} by actor [{}]", userId, provider, actor);
		return ResponseEntity.ok(authMethodService.unlinkUserAuthMethod(userId, provider, actor));
	}

	// =========================================================================
	// 3. Public & Login Authentication Endpoints
	// =========================================================================

	@PreAuthorize("permitAll()")
	@GetMapping("/api/auth/providers")
	public ResponseEntity<List<AuthProviderDto>> getSupportedProviders() {
		return ResponseEntity.ok(authMethodService.getSystemAuthProviders());
	}

	@PreAuthorize("permitAll()")
	@PostMapping("/api/auth/oauth/login")
	public ResponseEntity<Object> oauthLogin(
			@Valid @RequestBody OAuthLoginRequest request,
			HttpServletRequest httpRequest) {
		log.info("OAuth login attempt via provider: [{}]", request.getProvider());
		return authMethodService.authenticateWithOAuth(request, httpRequest);
	}

	@PreAuthorize("permitAll()")
	@GetMapping("/api/auth/oauth/{provider}/authorize")
	public ResponseEntity<Map<String, Object>> getOAuthAuthorizationUrl(
			@PathVariable String provider,
			@RequestParam(required = false) String redirectUri,
			@RequestParam(required = false) String state) {
		String cleanProvider = provider.toUpperCase();
		Optional<AuthProviderConfigEntity> configOpt = authProviderConfigService.getCachedProvider(cleanProvider);

		if (configOpt.isEmpty() || configOpt.get().getIsEnabled() == null || configOpt.get().getIsEnabled() != 'Y') {
			return ResponseEntity.badRequest().body(Map.of(
					PROVIDER, cleanProvider,
					STATUS, "DISABLED",
					MESSAGE, "Provider " + cleanProvider + " is not enabled in system configuration."
			));
		}

		AuthProviderConfigEntity config = configOpt.get();
		if (config.getClientId() == null || config.getClientId().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of(
					PROVIDER, cleanProvider,
					STATUS, "NOT_CONFIGURED",
					MESSAGE, "Provider " + cleanProvider + " is not configured with a Client ID. Please configure credentials in SSO Settings."
			));
		}

		String effectiveRedirectUri;
		if (redirectUri != null && !redirectUri.isBlank()) {
			effectiveRedirectUri = redirectUri;
		} else {
			effectiveRedirectUri = config.getRedirectUri() != null ? config.getRedirectUri() : "";
		}

		String authBase = config.getAuthorizationUrl() != null ? config.getAuthorizationUrl() : "";
		String scopes = config.getScopes() != null ? config.getScopes() : "read:user user:email";

		StringBuilder targetUrl = new StringBuilder(authBase);
		targetUrl.append(authBase.contains("?") ? "&" : "?");
		targetUrl.append("client_id=").append(URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8));
		if (!effectiveRedirectUri.isBlank()) {
			targetUrl.append("&redirect_uri=").append(URLEncoder.encode(effectiveRedirectUri, StandardCharsets.UTF_8));
		}
		targetUrl.append("&scope=").append(URLEncoder.encode(scopes, StandardCharsets.UTF_8));
		targetUrl.append("&response_type=code");
		if (state != null && !state.isBlank()) {
			targetUrl.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
		}

		return ResponseEntity.ok(Map.of(
				PROVIDER, cleanProvider,
				"name", config.getName(),
				"type", config.getType(),
				STATUS, "ACTIVE",
				"authorizationUrl", authBase,
				"clientId", config.getClientId(),
				"scopes", scopes,
				"redirectUri", effectiveRedirectUri,
				TARGET_URL, targetUrl.toString()
		));
	}

	@PreAuthorize("permitAll()")
	@GetMapping("/api/auth/oauth/{provider}/login")
	public ResponseEntity<Void> redirectToProviderLogin(
			@PathVariable String provider,
			@RequestParam(required = false) String redirectUri,
			@RequestParam(required = false) String state) {
		ResponseEntity<Map<String, Object>> response = getOAuthAuthorizationUrl(provider, redirectUri, state);
		Map<String, Object> body = response.getBody();
		if (!response.getStatusCode().is2xxSuccessful() || body == null || body.get(TARGET_URL) == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		String targetUrl = (String) body.get(TARGET_URL);
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(targetUrl)).build();
	}
}
