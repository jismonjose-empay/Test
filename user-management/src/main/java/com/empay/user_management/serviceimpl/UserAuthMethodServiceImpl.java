package com.empay.user_management.serviceimpl;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.user_management.dto.AuthProviderDto;
import com.empay.user_management.dto.ConfigureAuthMethodRequest;
import com.empay.user_management.dto.OAuthLoginRequest;
import com.empay.user_management.dto.UserAuthMethodDto;
import com.empay.user_management.dto.UserAuthMethodsOverviewResponse;
import com.empay.user_management.entities.AuthProviderConfigEntity;
import com.empay.user_management.entities.LoginAttemptEntity;
import com.empay.user_management.entities.UserAuthMethodEntity;
import com.empay.user_management.entities.UserCredentialEntity;
import com.empay.user_management.entities.UserRoleEntityV2;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.LoginAttemptRepository;
import com.empay.user_management.repository.UserAuthMethodRepository;
import com.empay.user_management.repository.UserCredentialRepository;
import com.empay.user_management.repository.UserRoleRepositoryV2;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.service.AuthProviderConfigService;
import com.empay.user_management.service.UserAuthMethodService;
import com.empay.user_management.utils.APIConstants;
import com.empay.user_management.utils.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAuthMethodServiceImpl implements UserAuthMethodService {

	private static final Logger log = LoggerFactory.getLogger(UserAuthMethodServiceImpl.class);

	public static final String PROVIDER_LOCAL_PASSWORD = "LOCAL_PASSWORD";
	public static final String PROVIDER_GITHUB = "GITHUB";

	private static final String DEFAULT_ACTOR = "ADMIN";
	private static final String MESSAGE = "message";
	private static final String AUTHENTICATED = "authenticated";
	private static final String FIELD_EMAIL = "email";

	private final UsersRepositoryV2 usersRepository;
	private final UserAuthMethodRepository authMethodRepository;
	private final UserCredentialRepository credentialRepository;
	private final AuthProviderConfigService authProviderConfigService;
	private final LoginAttemptRepository loginAttemptRepository;
	private final JwtUtils jwtUtils;
	private final UserRoleRepositoryV2 userRoleRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${server.servlet.session.cookie.secure:true}")
	private boolean cookieSecure = true;

	// Finding S1: Change inline default to false so absence means off
	@Value("${app.oauth.direct-login.enabled:false}")
	private boolean directLoginEnabled = false;

	@Override
	@Transactional(readOnly = true)
	public UserAuthMethodsOverviewResponse getUserAuthMethods(String userId) {
		return fetchUserAuthMethodsOverview(userId);
	}

	private UserAuthMethodsOverviewResponse fetchUserAuthMethodsOverview(String userId) {
		UsersEntityV2 user = findUserOrThrow(userId);
		List<UserAuthMethodEntity> configuredEntities = authMethodRepository.findByUserSerialNo(user.getSerialNumber());
		Map<String, UserAuthMethodEntity> entityMap = configuredEntities.stream()
				.collect(Collectors.toMap(e -> e.getProvider().toUpperCase(), e -> e, (a, b) -> a));

		UserCredentialEntity credential = credentialRepository.findByUserSerialNo(user.getSerialNumber());
		boolean hasLocalPassword = credential != null && credential.getPasswordHash() != null && !credential.getPasswordHash().isBlank();

		List<AuthProviderConfigEntity> systemProviders = authProviderConfigService.getAllCachedProviders();
		if (systemProviders == null || systemProviders.isEmpty()) {
			authProviderConfigService.refreshCache();
			systemProviders = authProviderConfigService.getAllCachedProviders();
		}

		List<UserAuthMethodDto> dtos = new ArrayList<>();
		for (AuthProviderConfigEntity sysProv : systemProviders) {
			if (sysProv.getIsEnabled() != null && sysProv.getIsEnabled() == 'Y') {
				dtos.add(buildUserAuthMethodDto(sysProv, entityMap, user, hasLocalPassword));
			}
		}

		ensurePrimaryMethod(dtos);

		String primaryMethod = dtos.stream()
				.filter(UserAuthMethodDto::isPrimary)
				.map(UserAuthMethodDto::getProvider)
				.findFirst()
				.orElse(PROVIDER_LOCAL_PASSWORD);

		return UserAuthMethodsOverviewResponse.builder()
				.userId(user.getUserId())
				.userSerialNumber(user.getSerialNumber())
				.displayName(user.getDisplayName() != null ? user.getDisplayName() : user.getUserId())
				.email(user.getEmail())
				.status(user.getStatus() != null ? user.getStatus() : "ACTIVE")
				.isBreakGlass(false)
				.primaryMethod(primaryMethod)
				.methods(dtos)
				.build();
	}

	private UserAuthMethodDto buildUserAuthMethodDto(AuthProviderConfigEntity sysProv,
			Map<String, UserAuthMethodEntity> entityMap, UsersEntityV2 user, boolean hasLocalPassword) {
		String pId = sysProv.getProviderId().toUpperCase();
		UserAuthMethodEntity userConfig = entityMap.get(pId);

		boolean isLinked = userConfig != null || (PROVIDER_LOCAL_PASSWORD.equals(pId) && hasLocalPassword);
		boolean isMethodEnabled = userConfig == null || userConfig.getIsEnabled() == 'Y';
		boolean isPrimary = userConfig != null && userConfig.getIsPrimary() == 'Y';

		String extSubject = userConfig != null ? userConfig.getExternalSubject() : null;
		String extUsername = userConfig != null ? userConfig.getExternalUsername() : null;
		String extEmail = resolveExternalEmail(userConfig, pId, user);
		LocalDateTime linkedAt = userConfig != null ? userConfig.getLinkedAt() : null;
		LocalDateTime lastLoginAt = userConfig != null ? userConfig.getLastLoginAt() : null;

		return UserAuthMethodDto.builder()
				.provider(pId)
				.providerName(sysProv.getName())
				.providerType(sysProv.getType())
				.isLinked(isLinked)
				.isEnabled(isMethodEnabled)
				.isPrimary(isPrimary)
				.externalSubject(extSubject)
				.externalUsername(extUsername)
				.externalEmail(extEmail)
				.linkedAt(linkedAt)
				.lastLoginAt(lastLoginAt)
				.icon(sysProv.getIcon() != null ? sysProv.getIcon() : pId.toLowerCase())
				.build();
	}

	private String resolveExternalEmail(UserAuthMethodEntity userConfig, String providerId, UsersEntityV2 user) {
		if (userConfig != null) {
			return userConfig.getExternalEmail();
		}
		if (PROVIDER_LOCAL_PASSWORD.equals(providerId)) {
			return user.getEmail();
		}
		return null;
	}

	private void ensurePrimaryMethod(List<UserAuthMethodDto> dtos) {
		boolean hasConfiguredPrimary = dtos.stream().anyMatch(UserAuthMethodDto::isPrimary);
		if (!hasConfiguredPrimary && !dtos.isEmpty()) {
			dtos.stream()
					.filter(d -> d.isLinked() && d.isEnabled())
					.findFirst()
					.ifPresent(d -> d.setPrimary(true));
		}
	}

	@Override
	@Transactional
	public UserAuthMethodsOverviewResponse configureUserAuthMethod(String userId, ConfigureAuthMethodRequest request, String actor) {
		if (request == null || request.getProvider() == null || request.getProvider().isBlank()) {
			throw new IllegalArgumentException("Provider must be specified");
		}

		UsersEntityV2 user = findUserOrThrow(userId);
		String provider = request.getProvider().trim().toUpperCase();
		validateSystemProviderExists(provider);

		String cleanSubject = (request.getExternalSubject() != null && !request.getExternalSubject().isBlank())
				? request.getExternalSubject().trim() : null;
		String cleanUsername = (request.getExternalUsername() != null && !request.getExternalUsername().isBlank())
				? request.getExternalUsername().trim() : cleanSubject;

		validateExternalIdentityUnique(provider, cleanSubject, cleanUsername, user.getSerialNumber());

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		String effectiveActor = actor != null ? actor : DEFAULT_ACTOR;

		UserAuthMethodEntity entity = authMethodRepository.findByUserSerialNoAndProvider(user.getSerialNumber(), provider)
				.map(existing -> updateUserAuthMethodEntity(existing, cleanSubject, cleanUsername, request, effectiveActor))
				.orElseGet(() -> createUserAuthMethodEntity(user, provider, cleanSubject, cleanUsername, request, effectiveActor, now));

		if (Boolean.TRUE.equals(request.getIsPrimary())) {
			promoteToPrimary(user, provider, cleanSubject, entity);
		}

		authMethodRepository.save(entity);
		log.info("Configured auth method [{}] for user [{}] by actor [{}]", provider, userId, actor);

		return fetchUserAuthMethodsOverview(userId);
	}

	private void validateSystemProviderExists(String provider) {
		Optional<AuthProviderConfigEntity> sysProv = authProviderConfigService.getCachedProvider(provider);
		if (sysProv.isEmpty()) {
			throw new IllegalArgumentException("Authentication provider [" + provider + "] is not configured in system.");
		}
	}

	private void validateExternalIdentityUnique(String provider, String cleanSubject, String cleanUsername, Integer userSerialNo) {
		if (cleanSubject != null && !cleanSubject.isBlank() && !PROVIDER_LOCAL_PASSWORD.equals(provider)
				&& authMethodRepository.existsByProviderAndExternalSubjectAndUserSerialNoNot(provider, cleanSubject, userSerialNo)) {
			throw new IllegalArgumentException("The " + provider + " identity [" + cleanSubject + "] is already linked to another MARS user.");
		}
		if (cleanUsername != null && !cleanUsername.isBlank() && !PROVIDER_LOCAL_PASSWORD.equals(provider)
				&& authMethodRepository.existsByProviderAndExternalUsernameAndUserSerialNoNot(provider, cleanUsername, userSerialNo)) {
			throw new IllegalArgumentException("The " + provider + " handle [" + cleanUsername + "] is already linked to another MARS user.");
		}
	}

	private UserAuthMethodEntity updateUserAuthMethodEntity(UserAuthMethodEntity entity, String cleanSubject,
			String cleanUsername, ConfigureAuthMethodRequest request, String actor) {
		if (cleanSubject != null) entity.setExternalSubject(cleanSubject);
		if (cleanUsername != null) entity.setExternalUsername(cleanUsername);
		if (request.getExternalEmail() != null) entity.setExternalEmail(request.getExternalEmail().trim());
		if (request.getIsEnabled() != null) entity.setIsEnabled(request.getIsEnabled().booleanValue() ? 'Y' : 'N');
		if (request.getMetadataJson() != null) entity.setMetadataJson(request.getMetadataJson());
		entity.setLinkedBy(actor);
		return entity;
	}

	private UserAuthMethodEntity createUserAuthMethodEntity(UsersEntityV2 user, String provider, String cleanSubject,
			String cleanUsername, ConfigureAuthMethodRequest request, String actor, LocalDateTime now) {
		return UserAuthMethodEntity.builder()
				.userSerialNo(user.getSerialNumber())
				.provider(provider)
				.externalSubject(cleanSubject != null ? cleanSubject : cleanUsername)
				.externalUsername(cleanUsername != null ? cleanUsername : cleanSubject)
				.externalEmail(request.getExternalEmail() != null ? request.getExternalEmail().trim() : user.getEmail())
				.isEnabled(Boolean.FALSE.equals(request.getIsEnabled()) ? 'N' : 'Y')
				.isPrimary('N')
				.linkedBy(actor)
				.linkedAt(now)
				.metadataJson(request.getMetadataJson())
				.build();
	}

	private void promoteToPrimary(UsersEntityV2 user, String provider, String cleanSubject, UserAuthMethodEntity entity) {
		List<UserAuthMethodEntity> allMethods = authMethodRepository.findByUserSerialNo(user.getSerialNumber());
		for (UserAuthMethodEntity other : allMethods) {
			if (!other.getProvider().equalsIgnoreCase(provider)) {
				other.setIsPrimary('N');
				authMethodRepository.save(other);
			}
		}
		entity.setIsPrimary('Y');
		if (!PROVIDER_LOCAL_PASSWORD.equals(provider) && cleanSubject != null) {
			user.setIdpSubject(cleanSubject);
			usersRepository.save(user);
		}
	}

	@Override
	@Transactional
	public UserAuthMethodsOverviewResponse unlinkUserAuthMethod(String userId, String provider, String actor) {
		UsersEntityV2 user = findUserOrThrow(userId);
		String targetProvider = provider.trim().toUpperCase();

		Optional<UserAuthMethodEntity> existingOpt = authMethodRepository.findByUserSerialNoAndProvider(user.getSerialNumber(), targetProvider);
		if (existingOpt.isEmpty()) {
			throw new IllegalArgumentException("Auth method [" + targetProvider + "] is not linked to user [" + userId + "].");
		}

		// Check if primary
		if (existingOpt.get().getIsPrimary() == 'Y') {
			List<UserAuthMethodEntity> remaining = authMethodRepository.findByUserSerialNo(user.getSerialNumber()).stream()
					.filter(m -> !m.getProvider().equalsIgnoreCase(targetProvider) && m.getIsEnabled() == 'Y')
					.toList();

			if (!remaining.isEmpty()) {
				UserAuthMethodEntity newPrimary = remaining.get(0);
				newPrimary.setIsPrimary('Y');
				authMethodRepository.save(newPrimary);
				if (!PROVIDER_LOCAL_PASSWORD.equals(newPrimary.getProvider())) {
					user.setIdpSubject(newPrimary.getExternalSubject());
					usersRepository.save(user);
				}
			}
		}

		authMethodRepository.delete(existingOpt.get());

		if (user.getIdpSubject() != null && user.getIdpSubject().equals(existingOpt.get().getExternalSubject())) {
			user.setIdpSubject(null);
			usersRepository.save(user);
		}

		log.info("Unlinked auth method [{}] from user [{}] by actor [{}]", targetProvider, userId, actor);
		return fetchUserAuthMethodsOverview(userId);
	}

	@Override
	public List<AuthProviderDto> getSystemAuthProviders() {
		return authProviderConfigService.getPublicEnabledProviders();
	}

	@Override
	@Transactional
	public ResponseEntity<Object> authenticateWithOAuth(OAuthLoginRequest request, HttpServletRequest httpRequest) {
		if (request == null || request.getProvider() == null || request.getProvider().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Provider is required (e.g. GITHUB, GOOGLE, MICROSOFT)"));
		}

		String provider = request.getProvider().trim().toUpperCase();

		// 1. Verify system provider configuration is active
		Optional<AuthProviderConfigEntity> configOpt = authProviderConfigService.getCachedProvider(provider);
		if (configOpt.isEmpty() || configOpt.get().getIsEnabled() == null || configOpt.get().getIsEnabled() != 'Y') {
			return ResponseEntity.badRequest().body(Map.of(
					AUTHENTICATED, false,
					MESSAGE, "Provider [" + provider + "] is not enabled in system configuration."
			));
		}
		AuthProviderConfigEntity config = configOpt.get();

		// 2. Resolve identity from OAuth request (exchange code or direct login)
		OAuthProfile profile;
		try {
			profile = resolveOAuthIdentity(request, config, provider);
		} catch (OAuthAuthenticationException ex) {
			return ResponseEntity.status(ex.getStatus()).body(Map.of(
					AUTHENTICATED, false,
					MESSAGE, ex.getMessage()
			));
		}

		String resolvedSubject = profile.subject();
		if (resolvedSubject == null || resolvedSubject.isBlank()) {
			recordLoginAttempt("UNKNOWN", null, 'N', "OAUTH_SUBJECT_MISSING", httpRequest);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					AUTHENTICATED, false,
					MESSAGE, "Identity provider did not return a valid subject identifier."
			));
		}

		// 3. Resolve linked MARS user from USER_AUTH_METHOD strictly by subject (Finding T1)
		Optional<UserAuthMethodEntity> authMethodOpt = lookupLinkedAuthMethod(provider, resolvedSubject);

		if (authMethodOpt.isEmpty()) {
			recordLoginAttempt(resolvedSubject, null, 'N', "OAUTH_USER_NOT_LINKED", httpRequest);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					AUTHENTICATED, false,
					MESSAGE, "No MARS account is linked to the " + provider + " identity (" + resolvedSubject + "). Please create the user in MARS and link this method in User Management settings."
			));
		}

		UserAuthMethodEntity authMethod = authMethodOpt.get();

		// 4. Verify linked MARS user exists in MARS app and account is eligible
		UsersEntityV2 user = usersRepository.findBySerialNumber(authMethod.getUserSerialNo());
		ResponseEntity<Object> eligibilityError = validateUserAccountEligibility(user, authMethod, provider, resolvedSubject, httpRequest);
		if (eligibilityError != null) {
			return eligibilityError;
		}

		// 5. Update last login timestamp
		authMethod.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
		authMethodRepository.save(authMethod);

		recordLoginAttempt(user.getUserId(), user.getSerialNumber(), 'Y', null, httpRequest);

		// 6. Mint JWT session token and secure cookies
		return buildOAuthSuccessResponse(user, provider);
	}

	private ResponseEntity<Object> validateUserAccountEligibility(UsersEntityV2 user, UserAuthMethodEntity authMethod,
			String provider, String resolvedSubject, HttpServletRequest httpRequest) {
		if (user == null) {
			recordLoginAttempt(resolvedSubject, null, 'N', "MARS_USER_NOT_FOUND", httpRequest);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					AUTHENTICATED, false,
					MESSAGE, "Linked MARS user account not found in database."
			));
		}

		if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
			recordLoginAttempt(user.getUserId(), user.getSerialNumber(), 'N', "USER_NOT_ACTIVE", httpRequest);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					AUTHENTICATED, false,
					MESSAGE, "Your MARS user account is " + (user.getStatus() != null ? user.getStatus().toLowerCase() : "inactive") + ". Please contact your system administrator."
			));
		}

		// Finding S2: Treat null is_enabled as disabled!
		if (authMethod.getIsEnabled() == null || Character.toUpperCase(authMethod.getIsEnabled()) != 'Y') {
			recordLoginAttempt(user.getUserId(), user.getSerialNumber(), 'N', "OAUTH_METHOD_DISABLED", httpRequest);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					AUTHENTICATED, false,
					MESSAGE, "The " + provider + " authentication method has been disabled for your account."
			));
		}

		// Finding S2: Early guard enforcing auth_mode on user
		String authMode = user.getAuthMode();
		if (authMode != null && !authMode.isBlank()) {
			String cleanMode = authMode.trim().toUpperCase(java.util.Locale.ROOT);
			if ("LOCAL".equals(cleanMode) || PROVIDER_LOCAL_PASSWORD.equals(cleanMode) || "PASSWORD_ONLY".equals(cleanMode)) {
				recordLoginAttempt(user.getUserId(), user.getSerialNumber(), 'N', "OAUTH_AUTH_DISABLED", httpRequest);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
						AUTHENTICATED, false,
						MESSAGE, "OAuth login is disabled for your account."
				));
			}
		}

		UserCredentialEntity credential = credentialRepository.findByUserSerialNo(user.getSerialNumber());
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		if (credential != null && credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(now)) {
			recordLoginAttempt(user.getUserId(), user.getSerialNumber(), 'N', "ACCOUNT_LOCKED", httpRequest);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					AUTHENTICATED, false,
					MESSAGE, "Your account is temporarily locked. Please try again later or contact your administrator."
			));
		}

		return null;
	}

	private OAuthProfile resolveOAuthIdentity(OAuthLoginRequest request, AuthProviderConfigEntity config, String provider) throws OAuthAuthenticationException {
		if (request.getCode() != null && !request.getCode().isBlank()) {
			try {
				return exchangeCodeWithProvider(config, request.getCode(), request.getRedirectUri());
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				log.warn("OAuth token exchange interrupted for provider [{}]: {}", provider, ex.getMessage());
				throw new OAuthAuthenticationException(HttpStatus.UNAUTHORIZED, "OAuth exchange interrupted: " + ex.getMessage());
			} catch (IOException | RuntimeException ex) {
				log.warn("OAuth server-side token exchange failed for provider [{}]: {}", provider, ex.getMessage());
				throw new OAuthAuthenticationException(HttpStatus.UNAUTHORIZED, "OAuth token exchange with " + provider + " failed: " + ex.getMessage());
			}
		}

		if (request.getIdentity() != null && !request.getIdentity().isBlank()) {
			if (!directLoginEnabled) {
				throw new OAuthAuthenticationException(HttpStatus.FORBIDDEN, "Direct OAuth identity login is disabled in production. Full OAuth authorization code flow is required.");
			}
			String identity = request.getIdentity().trim();
			return new OAuthProfile(identity, identity, null);
		}

		throw new OAuthAuthenticationException(HttpStatus.BAD_REQUEST, "Either authorization code or linked identity must be supplied");
	}

	private Optional<UserAuthMethodEntity> lookupLinkedAuthMethod(String provider, String resolvedSubject) {
		if (resolvedSubject == null || resolvedSubject.isBlank()) {
			return Optional.empty();
		}
		String cleanSubject = resolvedSubject.trim();
		// 1. the authoritative mapping
		Optional<UserAuthMethodEntity> byMethod =
				authMethodRepository.findByProviderAndExternalSubject(provider, cleanSubject);
		if (byMethod.isPresent()) {
			return byMethod;
		}

		// 2. legacy USERS.idp_subject, READ ONLY, for the migration window only.
		//    No save, no auto-create, no auth-mode change: whatever is stored is what
		//    the eligibility check below must see.
		UsersEntityV2 idpUser = usersRepository.findByIdpSubject(cleanSubject);
		if (idpUser == null) {
			idpUser = usersRepository.findByIdpSubject(provider + ":" + cleanSubject);
		}
		if (idpUser == null) {
			return Optional.empty();
		}
		return authMethodRepository.findByUserSerialNoAndProvider(idpUser.getSerialNumber(), provider);
	}

	private ResponseEntity<Object> buildOAuthSuccessResponse(UsersEntityV2 user, String provider) {
		String userRole = resolveUserRole(user);
		String token = jwtUtils.generateToken(user.getUserId(), user.getSerialNumber(), userRole);

		ResponseCookie accessCookie = createAuthCookie("ACCESS_TOKEN", token);
		ResponseCookie authCookie = createAuthCookie("AUTH_TOKEN", token);

		Map<String, Object> responseBody = new LinkedHashMap<>();
		responseBody.put(AUTHENTICATED, true);
		responseBody.put(MESSAGE, "OAuth login successful via " + provider);
		// Finding S5: Do NOT echo session token into response body. The HttpOnly cookie is the session.
		responseBody.put("userId", user.getUserId());
		responseBody.put("userName", user.getDisplayName() != null ? user.getDisplayName() : user.getUserId());
		responseBody.put("name", user.getDisplayName() != null ? user.getDisplayName() : user.getUserId());
		responseBody.put(FIELD_EMAIL, user.getEmail());
		responseBody.put("status", user.getStatus());
		responseBody.put("roles", userRole != null ? List.of(userRole.split(",")) : List.of());

		return ResponseEntity.ok()
				// Finding S5: Do NOT set Authorization header in response. The HttpOnly cookie is the session.
				.header(HttpHeaders.SET_COOKIE, accessCookie.toString())
				.header(HttpHeaders.SET_COOKIE, authCookie.toString())
				.body(responseBody);
	}

	private ResponseCookie createAuthCookie(String name, String value) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(cookieSecure)
				.path("/")
				.sameSite("Strict")
				.maxAge(Duration.ofMinutes(APIConstants.ACCESS_TOKEN_EXPIRY_IN_MINUTES))
				.build();
	}

	private record OAuthProfile(String subject, String username, String email) {}

	private static class OAuthAuthenticationException extends Exception {
		private final HttpStatus status;

		OAuthAuthenticationException(HttpStatus status, String message) {
			super(message);
			this.status = status;
		}

		HttpStatus getStatus() {
			return status;
		}
	}

	private OAuthProfile exchangeCodeWithProvider(AuthProviderConfigEntity config, String code, String clientRedirectUri) throws IOException, InterruptedException {
		if (config.getTokenUrl() == null || config.getTokenUrl().isBlank()) {
			throw new IllegalStateException("Token URL is not configured for provider: " + config.getProviderId());
		}
		if (config.getClientId() == null || config.getClientId().isBlank()) {
			throw new IllegalStateException("Client ID is not configured for provider: " + config.getProviderId());
		}

		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();

		String accessToken = requestAccessToken(config, code, clientRedirectUri, httpClient);
		return fetchUserProfile(config, accessToken, httpClient);
	}

	private String resolveRedirectUri(AuthProviderConfigEntity config, String clientRedirectUri) {
		if (clientRedirectUri != null && !clientRedirectUri.isBlank()) {
			return clientRedirectUri;
		}
		return config.getRedirectUri() != null ? config.getRedirectUri() : "";
	}

	private String buildTokenRequestBody(AuthProviderConfigEntity config, String code, String clientRedirectUri) {
		String redirectUri = resolveRedirectUri(config, clientRedirectUri);

		StringBuilder form = new StringBuilder();
		if (!PROVIDER_GITHUB.equalsIgnoreCase(config.getProviderId())) {
			form.append("grant_type=authorization_code&");
		}
		form.append("code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));
		form.append("&client_id=").append(URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8));
		if (config.getClientSecret() != null && !config.getClientSecret().isBlank()) {
			form.append("&client_secret=").append(URLEncoder.encode(config.getClientSecret(), StandardCharsets.UTF_8));
		}
		if (!redirectUri.isBlank()) {
			form.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
		}
		return form.toString();
	}

	private String extractAccessToken(JsonNode tokenJson, String providerId, String responseBody) {
		String accessToken = tokenJson.hasNonNull("access_token") ? tokenJson.get("access_token").asText() : null;
		if (accessToken != null && !accessToken.isBlank()) {
			return accessToken;
		}
		String err = tokenJson.hasNonNull("error") ? tokenJson.get("error").asText() : null;
		String desc = tokenJson.hasNonNull("error_description") ? tokenJson.get("error_description").asText() : null;
		if (err != null) {
			throw new IllegalStateException("Identity Provider error [" + err + "]: " + (desc != null ? desc : ""));
		}
		throw new IllegalStateException("No access_token returned by " + providerId + ": " + responseBody);
	}

	private String requestAccessToken(AuthProviderConfigEntity config, String code, String clientRedirectUri, HttpClient httpClient) throws IOException, InterruptedException {
		String formBody = buildTokenRequestBody(config, code, clientRedirectUri);

		HttpRequest tokenRequest = HttpRequest.newBuilder()
				.uri(URI.create(config.getTokenUrl()))
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(formBody))
				.build();

		HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
		if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
			throw new IllegalStateException("Token exchange returned status " + tokenResponse.statusCode() + ": " + tokenResponse.body());
		}

		JsonNode tokenJson = objectMapper.readTree(tokenResponse.body());
		return extractAccessToken(tokenJson, config.getProviderId(), tokenResponse.body());
	}

	private OAuthProfile fetchUserProfile(AuthProviderConfigEntity config, String accessToken, HttpClient httpClient) throws IOException, InterruptedException {
		String userInfoUrl = config.getUserInfoUrl();
		if (userInfoUrl == null || userInfoUrl.isBlank()) {
			throw new IllegalStateException("User info URL is not configured for provider: " + config.getProviderId());
		}

		HttpRequest userRequest = HttpRequest.newBuilder()
				.uri(URI.create(userInfoUrl))
				.timeout(Duration.ofSeconds(10))
				.header("Authorization", "Bearer " + accessToken)
				.header("Accept", "application/json")
				.header("User-Agent", "MARS3-Platform")
				.GET()
				.build();

		HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
		if (userResponse.statusCode() < 200 || userResponse.statusCode() >= 300) {
			throw new IllegalStateException("User info endpoint returned status " + userResponse.statusCode() + ": " + userResponse.body());
		}

		JsonNode userNode = objectMapper.readTree(userResponse.body());
		String subject = null;
		if (userNode.hasNonNull("id")) {
			subject = userNode.get("id").asText();
		} else if (userNode.hasNonNull("sub")) {
			subject = userNode.get("sub").asText();
		}

		if (subject == null || subject.isBlank()) {
			throw new IllegalStateException("Identity Provider userinfo response did not return a valid subject/id claim");
		}

		String username = null;
		if (userNode.hasNonNull("login")) {
			username = userNode.get("login").asText();
		} else if (userNode.hasNonNull("preferred_username")) {
			username = userNode.get("preferred_username").asText();
		}
		// Finding T1: Do not fallback to user display name; display name can be chosen/forged!

		String email = userNode.hasNonNull(FIELD_EMAIL) ? userNode.get(FIELD_EMAIL).asText() : null;

		return new OAuthProfile(subject, username != null ? username : subject, email);
	}

	private String resolveUserRole(UsersEntityV2 user) {
		if (user == null) return null;
		List<UserRoleEntityV2> userRoles = user.getUserRoles();
		if ((userRoles == null || userRoles.isEmpty()) && user.getSerialNumber() != null && userRoleRepository != null) {
			userRoles = userRoleRepository.findBySerialNumber(user.getSerialNumber());
		}
		if (userRoles == null || userRoles.isEmpty()) return null;
		return userRoles.stream()
				.map(UserRoleEntityV2::getRoleCode)
				.filter(r -> r != null && !r.isBlank())
				.distinct()
				.collect(Collectors.joining(","));
	}

	private void recordLoginAttempt(String userId, Integer serialNo, char success, String failureReason, HttpServletRequest req) {
		if (loginAttemptRepository == null) return;
		try {
			LoginAttemptEntity attempt = new LoginAttemptEntity();
			attempt.setUserId(userId != null ? userId : "UNKNOWN");
			attempt.setUserSerialNumber(serialNo);
			attempt.setSucceeded(success);
			attempt.setFailureReason(failureReason);
			if (req != null) {
				attempt.setSourceIp(req.getRemoteAddr());
				attempt.setUserAgent(req.getHeader("User-Agent"));
				if (req.getSession(false) != null) {
					attempt.setSessionId(req.getSession(false).getId());
				}
			}
			loginAttemptRepository.save(attempt);
		} catch (Exception ex) {
			log.warn("Failed to record OAuth login attempt: {}", ex.getMessage());
		}
	}

	private UsersEntityV2 findUserOrThrow(String userId) {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("User ID must not be blank");
		}
		UsersEntityV2 user = usersRepository.findByUserId(userId.trim());
		if (user == null) {
			throw new ResourceNotFoundException("MARS_USER", "userId", "User not found with ID: " + userId);
		}
		return user;
	}
}
