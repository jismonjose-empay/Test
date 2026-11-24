package com.empay.user_management.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.user_management.dto.AuthProviderConfigDto;
import com.empay.user_management.dto.AuthProviderDto;
import com.empay.user_management.dto.SaveAuthProviderConfigRequest;
import com.empay.user_management.entities.AuthProviderConfigEntity;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.AuthProviderConfigRepository;
import com.empay.user_management.repository.UserAuthMethodRepository;
import com.empay.user_management.service.AuthProviderConfigService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@DependsOn("secretCryptoConverter")
public class AuthProviderConfigServiceImpl implements AuthProviderConfigService {

	private static final Logger log = LoggerFactory.getLogger(AuthProviderConfigServiceImpl.class);

	public static final String PROVIDER_LOCAL_PASSWORD = "LOCAL_PASSWORD";
	public static final String MASKED_SECRET = "••••••••";
	private static final String DEFAULT_OIDC_SCOPES = "openid email profile";
	private static final String DEFAULT_ACTOR = "ADMIN";

	private final AuthProviderConfigRepository configRepository;
	private final UserAuthMethodRepository userAuthMethodRepository;

	// In-memory cache for ultra-fast startup and runtime lookup
	private final Map<String, AuthProviderConfigEntity> cache = new ConcurrentHashMap<>();
	private final List<AuthProviderConfigEntity> orderedCache = new CopyOnWriteArrayList<>();

	@PostConstruct
	public void init() {
		refreshCache();
		log.info("Loaded {} authentication provider configurations into in-memory cache.", cache.size());
	}

	@Override
	public synchronized void refreshCache() {
		List<AuthProviderConfigEntity> allEntities = configRepository.findAllByOrderByDisplayOrderAsc();
		cache.clear();
		orderedCache.clear();
		for (AuthProviderConfigEntity entity : allEntities) {
			// Finding S6: Transparently migrate plain-text secrets to AES-256-GCM encrypted format
			if (entity.getClientSecret() != null && !entity.getClientSecret().isBlank()
					&& !entity.getClientSecret().startsWith("enc:")) {
				try {
					entity = configRepository.save(entity);
				} catch (Exception ex) {
					log.warn("Could not migrate plain-text secret for provider {}: {}", entity.getProviderId(), ex.getMessage());
				}
			}
			cache.put(entity.getProviderId().toUpperCase(), entity);
			orderedCache.add(entity);
		}
		log.debug("Refreshed auth provider in-memory cache with {} items.", orderedCache.size());
	}

	@Override
	public List<AuthProviderConfigEntity> getAllCachedProviders() {
		return Collections.unmodifiableList(orderedCache);
	}

	@Override
	public List<AuthProviderConfigEntity> getEnabledCachedProviders() {
		return orderedCache.stream()
				.filter(p -> p.getIsEnabled() != null && p.getIsEnabled() == 'Y')
				.toList();
	}

	@Override
	public Optional<AuthProviderConfigEntity> getCachedProvider(String providerId) {
		if (providerId == null) return Optional.empty();
		return Optional.ofNullable(cache.get(providerId.trim().toUpperCase()));
	}

	@Override
	public List<AuthProviderDto> getPublicEnabledProviders() {
		return getEnabledCachedProviders().stream()
				.map(entity -> AuthProviderDto.builder()
						.id(entity.getProviderId())
						.name(entity.getName())
						.type(entity.getType())
						.icon(entity.getIcon() != null ? entity.getIcon() : "security")
						.isEnabled(true)
						.description(entity.getDescription())
						.authorizationUrl("/api/auth/oauth/" + entity.getProviderId().toLowerCase() + "/authorize")
						.build())
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuthProviderConfigDto> getAllProviderConfigs() {
		return getAllCachedProviders().stream()
				.map(this::toDto)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public AuthProviderConfigDto getProviderConfig(String providerId) {
		AuthProviderConfigEntity entity = findEntityOrThrow(providerId);
		return toDto(entity);
	}

	@Override
	@Transactional
	public AuthProviderConfigDto saveProviderConfig(SaveAuthProviderConfigRequest request, String actor) {
		String providerId = request.getProviderId().trim().toUpperCase();
		Optional<AuthProviderConfigEntity> existingOpt = configRepository.findByProviderIdIgnoreCase(providerId);

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		String effectiveActor = actor != null ? actor : DEFAULT_ACTOR;

		AuthProviderConfigEntity entity = existingOpt.isPresent()
				? updateExistingEntity(existingOpt.get(), request, effectiveActor, now)
				: createNewEntity(providerId, request, effectiveActor, now);

		AuthProviderConfigEntity saved = configRepository.save(entity);
		refreshCache();
		log.info("Saved auth provider config [{}] by actor [{}]", providerId, actor);
		return toDto(saved);
	}

	private AuthProviderConfigEntity updateExistingEntity(AuthProviderConfigEntity entity,
			SaveAuthProviderConfigRequest request, String actor, LocalDateTime now) {
		entity.setName(request.getName().trim());
		entity.setType(request.getType().trim().toUpperCase());
		entity.setClientId(trimToNull(request.getClientId()));

		String secret = request.getClientSecret();
		if (secret != null && !secret.isBlank() && !MASKED_SECRET.equals(secret.trim())) {
			entity.setClientSecret(secret.trim());
		}

		entity.setAuthorizationUrl(trimToNull(request.getAuthorizationUrl()));
		entity.setTokenUrl(trimToNull(request.getTokenUrl()));
		entity.setUserInfoUrl(trimToNull(request.getUserInfoUrl()));
		entity.setIssuerUrl(trimToNull(request.getIssuerUrl()));
		entity.setScopes(trimToNull(request.getScopes()));
		entity.setRedirectUri(trimToNull(request.getRedirectUri()));
		if (request.getIsEnabled() != null) {
			entity.setIsEnabled(request.getIsEnabled().booleanValue() ? 'Y' : 'N');
		}
		entity.setIcon(trimToNull(request.getIcon()));
		entity.setDescription(trimToNull(request.getDescription()));
		if (request.getDisplayOrder() != null) {
			entity.setDisplayOrder(request.getDisplayOrder());
		}
		entity.setUpdatedAt(now);
		entity.setUpdatedBy(actor);
		return entity;
	}

	private AuthProviderConfigEntity createNewEntity(String providerId,
			SaveAuthProviderConfigRequest request, String actor, LocalDateTime now) {
		String secret = trimToNull(request.getClientSecret());
		String initialSecret = (secret != null && !MASKED_SECRET.equals(secret)) ? secret : null;
		// Finding S11: Invert create default so an omitted isEnabled defaults to disabled ('N')
		char isEnabled = Boolean.TRUE.equals(request.getIsEnabled()) ? 'Y' : 'N';
		String icon = request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon().trim() : "security";
		int order = request.getDisplayOrder() != null ? request.getDisplayOrder() : (orderedCache.size() + 1);

		return AuthProviderConfigEntity.builder()
				.providerId(providerId)
				.name(request.getName().trim())
				.type(request.getType().trim().toUpperCase())
				.clientId(trimToNull(request.getClientId()))
				.clientSecret(initialSecret)
				.authorizationUrl(trimToNull(request.getAuthorizationUrl()))
				.tokenUrl(trimToNull(request.getTokenUrl()))
				.userInfoUrl(trimToNull(request.getUserInfoUrl()))
				.issuerUrl(trimToNull(request.getIssuerUrl()))
				.scopes(trimToNull(request.getScopes()))
				.redirectUri(trimToNull(request.getRedirectUri()))
				.isEnabled(isEnabled)
				.icon(icon)
				.description(trimToNull(request.getDescription()))
				.displayOrder(order)
				.createdAt(now)
				.updatedAt(now)
				.updatedBy(actor)
				.build();
	}

	private static String trimToNull(String s) {
		return (s != null && !s.isBlank()) ? s.trim() : null;
	}

	@Override
	@Transactional
	public AuthProviderConfigDto toggleProviderStatus(String providerId, boolean isEnabled, String actor) {
		String cleanId = providerId.trim().toUpperCase();
		AuthProviderConfigEntity entity = findEntityOrThrow(cleanId);

		// Guard: Cannot disable LOCAL_PASSWORD if no other method is enabled
		if (PROVIDER_LOCAL_PASSWORD.equals(cleanId) && !isEnabled) {
			// Finding T6: Null-guard the Character comparison to prevent NullPointerException
			boolean hasOtherEnabled = orderedCache.stream()
					.anyMatch(p -> !PROVIDER_LOCAL_PASSWORD.equals(p.getProviderId())
							&& p.getIsEnabled() != null && Character.toUpperCase(p.getIsEnabled()) == 'Y');
			if (!hasOtherEnabled) {
				throw new IllegalArgumentException("Cannot disable Local Password when no other authentication method is currently active.");
			}
		}

		entity.setIsEnabled(isEnabled ? 'Y' : 'N');
		entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
		entity.setUpdatedBy(actor != null ? actor : DEFAULT_ACTOR);

		AuthProviderConfigEntity saved = configRepository.save(entity);
		refreshCache();
		log.info("Toggled auth provider [{}] status to [{}] by actor [{}]", cleanId, isEnabled, actor);
		return toDto(saved);
	}

	@Override
	@Transactional
	public void deleteProviderConfig(String providerId, String actor) {
		String cleanId = providerId.trim().toUpperCase();
		if (PROVIDER_LOCAL_PASSWORD.equals(cleanId)) {
			throw new IllegalArgumentException("Cannot delete default Local Password authentication method.");
		}

		AuthProviderConfigEntity entity = findEntityOrThrow(cleanId);

		// Check if any users are actively linked to this provider
		if (userAuthMethodRepository.existsByProviderIgnoreCase(cleanId)) {
			throw new IllegalArgumentException("Cannot delete provider [" + cleanId + "] because active users are linked to it. Please unlink them first or disable the provider.");
		}

		configRepository.delete(entity);
		refreshCache();
		log.info("Deleted auth provider config [{}] by actor [{}]", cleanId, actor);
	}

	private AuthProviderConfigEntity findEntityOrThrow(String providerId) {
		if (providerId == null || providerId.isBlank()) {
			throw new IllegalArgumentException("Provider ID must not be blank");
		}
		return configRepository.findByProviderIdIgnoreCase(providerId.trim())
				.orElseThrow(() -> new ResourceNotFoundException("AUTH_PROVIDER_CONFIG", "providerId", "Provider configuration not found: " + providerId));
	}

	private AuthProviderConfigDto toDto(AuthProviderConfigEntity entity) {
		boolean hasSecret = entity.getClientSecret() != null && !entity.getClientSecret().isBlank();
		return AuthProviderConfigDto.builder()
				.providerId(entity.getProviderId())
				.name(entity.getName())
				.type(entity.getType())
				.clientId(entity.getClientId())
				.clientSecretMasked(hasSecret ? MASKED_SECRET : null)
				.hasClientSecret(hasSecret)
				.authorizationUrl(entity.getAuthorizationUrl())
				.tokenUrl(entity.getTokenUrl())
				.userInfoUrl(entity.getUserInfoUrl())
				.issuerUrl(entity.getIssuerUrl())
				.scopes(entity.getScopes())
				.redirectUri(entity.getRedirectUri())
				.isEnabled(entity.getIsEnabled() != null && entity.getIsEnabled() == 'Y')
				.icon(entity.getIcon())
				.description(entity.getDescription())
				.displayOrder(entity.getDisplayOrder() != null ? entity.getDisplayOrder() : 1)
				.updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt())
				.updatedBy(entity.getUpdatedBy())
				.build();
	}
}

