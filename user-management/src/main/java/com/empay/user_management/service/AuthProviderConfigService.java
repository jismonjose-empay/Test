package com.empay.user_management.service;

import java.util.List;
import java.util.Optional;

import com.empay.user_management.dto.AuthProviderConfigDto;
import com.empay.user_management.dto.AuthProviderDto;
import com.empay.user_management.dto.SaveAuthProviderConfigRequest;
import com.empay.user_management.entities.AuthProviderConfigEntity;

public interface AuthProviderConfigService {

	/**
	 * Refreshes the in-memory cache from the database.
	 */
	void refreshCache();

	/**
	 * Fast memory lookup of all configured providers in display order.
	 */
	List<AuthProviderConfigEntity> getAllCachedProviders();

	/**
	 * Fast memory lookup of only enabled authentication providers.
	 */
	List<AuthProviderConfigEntity> getEnabledCachedProviders();

	/**
	 * Fast memory lookup of a specific provider.
	 */
	Optional<AuthProviderConfigEntity> getCachedProvider(String providerId);

	/**
	 * Returns sanitized public providers for login page and user linking.
	 */
	List<AuthProviderDto> getPublicEnabledProviders();

	/**
	 * Admin: returns full configurations with masked secrets.
	 */
	List<AuthProviderConfigDto> getAllProviderConfigs();

	/**
	 * Admin: returns a single provider configuration with masked secret.
	 */
	AuthProviderConfigDto getProviderConfig(String providerId);

	/**
	 * Admin: creates or updates an authentication provider configuration.
	 */
	AuthProviderConfigDto saveProviderConfig(SaveAuthProviderConfigRequest request, String actor);

	/**
	 * Admin: quick toggle provider enabled/disabled.
	 */
	AuthProviderConfigDto toggleProviderStatus(String providerId, boolean isEnabled, String actor);

	/**
	 * Admin: deletes a custom provider configuration.
	 */
	void deleteProviderConfig(String providerId, String actor);
}

