package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.empay.user_management.dto.AuthProviderConfigDto;
import com.empay.user_management.dto.AuthProviderDto;
import com.empay.user_management.dto.SaveAuthProviderConfigRequest;
import com.empay.user_management.entities.AuthProviderConfigEntity;
import com.empay.user_management.repository.AuthProviderConfigRepository;
import com.empay.user_management.repository.UserAuthMethodRepository;

@ExtendWith(MockitoExtension.class)
class AuthProviderConfigServiceImplTest {

	@Mock
	private AuthProviderConfigRepository configRepository;

	@Mock
	private UserAuthMethodRepository userAuthMethodRepository;

	@InjectMocks
	private AuthProviderConfigServiceImpl service;

	private AuthProviderConfigEntity localPassEntity;
	private AuthProviderConfigEntity gitHubEntity;
	private AuthProviderConfigEntity googleEntity;

	@BeforeEach
	void setUp() {
		localPassEntity = AuthProviderConfigEntity.builder()
				.providerId("LOCAL_PASSWORD")
				.name("Local Password")
				.type("PASSWORD")
				.isEnabled('Y')
				.displayOrder(1)
				.createdAt(LocalDateTime.now())
				.build();

		gitHubEntity = AuthProviderConfigEntity.builder()
				.providerId("GITHUB")
				.name("GitHub")
				.type("OAUTH2")
				.clientId("gh-client-id")
				.clientSecret("enc:gh-raw-secret")
				.authorizationUrl("https://github.com/login/oauth/authorize")
				.tokenUrl("https://github.com/login/oauth/access_token")
				.scopes("read:user")
				.isEnabled('Y')
				.displayOrder(2)
				.createdAt(LocalDateTime.now())
				.build();

		googleEntity = AuthProviderConfigEntity.builder()
				.providerId("GOOGLE")
				.name("Google")
				.type("OIDC")
				.clientId("google-client-id")
				.clientSecret("enc:google-raw-secret")
				.isEnabled('N')
				.displayOrder(3)
				.createdAt(LocalDateTime.now())
				.build();
	}

	@Test
	@DisplayName("refreshCache loads entities into memory cache and serves getEnabledCachedProviders")
	void testRefreshCacheAndGetEnabled() {
		when(configRepository.findAllByOrderByDisplayOrderAsc())
				.thenReturn(List.of(localPassEntity, gitHubEntity, googleEntity));

		service.refreshCache();

		List<AuthProviderConfigEntity> all = service.getAllCachedProviders();
		assertEquals(3, all.size());

		List<AuthProviderConfigEntity> enabled = service.getEnabledCachedProviders();
		assertEquals(2, enabled.size());
		assertTrue(enabled.stream().anyMatch(e -> e.getProviderId().equals("LOCAL_PASSWORD")));
		assertTrue(enabled.stream().anyMatch(e -> e.getProviderId().equals("GITHUB")));
		assertFalse(enabled.stream().anyMatch(e -> e.getProviderId().equals("GOOGLE")));
	}

	@Test
	@DisplayName("getPublicEnabledProviders strips client secret and returns only enabled methods")
	void testGetPublicEnabledProviders() {
		when(configRepository.findAllByOrderByDisplayOrderAsc())
				.thenReturn(List.of(localPassEntity, gitHubEntity, googleEntity));

		service.refreshCache();

		List<AuthProviderDto> publicList = service.getPublicEnabledProviders();
		assertEquals(2, publicList.size());
		assertTrue(publicList.stream().anyMatch(p -> p.getId().equals("GITHUB") && p.isEnabled()));
		assertFalse(publicList.stream().anyMatch(p -> p.getId().equals("GOOGLE")));
	}

	@Test
	@DisplayName("getAllProviderConfigs masks client secret in DTO")
	void testGetAllProviderConfigs_MasksSecret() {
		when(configRepository.findAllByOrderByDisplayOrderAsc())
				.thenReturn(List.of(localPassEntity, gitHubEntity));

		service.refreshCache();

		List<AuthProviderConfigDto> dtos = service.getAllProviderConfigs();
		assertEquals(2, dtos.size());

		AuthProviderConfigDto ghDto = dtos.stream().filter(d -> d.getProviderId().equals("GITHUB")).findFirst().orElseThrow();
		assertEquals("••••••••", ghDto.getClientSecretMasked());
		assertTrue(ghDto.isHasClientSecret());

		AuthProviderConfigDto localDto = dtos.stream().filter(d -> d.getProviderId().equals("LOCAL_PASSWORD")).findFirst().orElseThrow();
		assertNull(localDto.getClientSecretMasked());
		assertFalse(localDto.isHasClientSecret());
	}

	@Test
	@DisplayName("saveProviderConfig creates new provider and updates in-memory cache")
	void testSaveProviderConfig_CreateNew() {
		when(configRepository.findByProviderIdIgnoreCase("KEYCLOAK")).thenReturn(Optional.empty());
		when(configRepository.save(any(AuthProviderConfigEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(configRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(localPassEntity, gitHubEntity));

		SaveAuthProviderConfigRequest req = SaveAuthProviderConfigRequest.builder()
				.providerId("KEYCLOAK")
				.name("Enterprise Keycloak")
				.type("OIDC")
				.clientId("kc-client")
				.clientSecret("kc-secret")
				.isEnabled(true)
				.build();

		AuthProviderConfigDto result = service.saveProviderConfig(req, "admin_user");

		assertNotNull(result);
		assertEquals("KEYCLOAK", result.getProviderId());
		assertEquals("Enterprise Keycloak", result.getName());
		assertEquals("••••••••", result.getClientSecretMasked());
		assertTrue(result.isHasClientSecret());
		verify(configRepository).save(any(AuthProviderConfigEntity.class));
	}

	@Test
	@DisplayName("toggleProviderStatus toggles status and rejects disabling sole local password")
	void testToggleProviderStatus_GuardLocalPassword() {
		when(configRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(localPassEntity));
		service.refreshCache();

		when(configRepository.findByProviderIdIgnoreCase("LOCAL_PASSWORD")).thenReturn(Optional.of(localPassEntity));

		assertThrows(IllegalArgumentException.class, () -> service.toggleProviderStatus("LOCAL_PASSWORD", false, "admin"));
	}

	@Test
	@DisplayName("deleteProviderConfig rejects deleting LOCAL_PASSWORD")
	void testDeleteProviderConfig_RejectsLocalPassword() {
		assertThrows(IllegalArgumentException.class, () -> service.deleteProviderConfig("LOCAL_PASSWORD", "admin"));
	}

	@Test
	@DisplayName("deleteProviderConfig rejects deleting provider if users are actively linked")
	void testDeleteProviderConfig_RejectsWhenUsersLinked() {
		when(configRepository.findByProviderIdIgnoreCase("GITHUB")).thenReturn(Optional.of(gitHubEntity));
		when(userAuthMethodRepository.existsByProviderIgnoreCase("GITHUB")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> service.deleteProviderConfig("GITHUB", "admin"));
		verify(configRepository, never()).delete(any());
	}

	@Test
	@DisplayName("deleteProviderConfig deletes provider when no users are linked")
	void testDeleteProviderConfig_Success() {
		when(configRepository.findByProviderIdIgnoreCase("GITHUB")).thenReturn(Optional.of(gitHubEntity));
		when(userAuthMethodRepository.existsByProviderIgnoreCase("GITHUB")).thenReturn(false);
		when(configRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(localPassEntity));

		service.deleteProviderConfig("GITHUB", "admin");

		verify(configRepository).delete(gitHubEntity);
	}

	@Test
	@DisplayName("init refreshes cache from database without seeding (Finding T3)")
	void testInit_RefreshesCacheFromDatabase() {
		when(configRepository.findAllByOrderByDisplayOrderAsc())
				.thenReturn(List.of(localPassEntity));

		service.init();

		verify(configRepository).findAllByOrderByDisplayOrderAsc();
	}

	@Test
	@DisplayName("saveProviderConfig updating existing provider preserves secret if masked string is supplied")
	void testSaveProviderConfig_PreservesSecretWhenMasked() {
		when(configRepository.findByProviderIdIgnoreCase("GITHUB")).thenReturn(Optional.of(gitHubEntity));
		when(configRepository.save(any(AuthProviderConfigEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(configRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(gitHubEntity));

		SaveAuthProviderConfigRequest req = SaveAuthProviderConfigRequest.builder()
				.providerId("GITHUB")
				.name("GitHub Updated")
				.type("OAUTH2")
				.clientSecret("••••••••")
				.build();

		AuthProviderConfigDto result = service.saveProviderConfig(req, "admin");

		assertEquals("GitHub Updated", result.getName());
		assertEquals("enc:gh-raw-secret", gitHubEntity.getClientSecret());
	}

	@Test
	@DisplayName("toggleProviderStatus allows disabling LOCAL_PASSWORD when another provider is active")
	void testToggleProviderStatus_AllowsDisablingWhenOtherActive() {
		when(configRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(localPassEntity, gitHubEntity));
		service.refreshCache();

		when(configRepository.findByProviderIdIgnoreCase("LOCAL_PASSWORD")).thenReturn(Optional.of(localPassEntity));
		when(configRepository.save(any(AuthProviderConfigEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuthProviderConfigDto result = service.toggleProviderStatus("LOCAL_PASSWORD", false, "admin");

		assertFalse(result.isEnabled());
		assertEquals('N', localPassEntity.getIsEnabled());
	}

	@Test
	@DisplayName("getProviderConfig returns DTO and throws ResourceNotFoundException on unknown id")
	void testGetProviderConfig_LookupAndNotFound() {
		when(configRepository.findByProviderIdIgnoreCase("GITHUB")).thenReturn(Optional.of(gitHubEntity));
		AuthProviderConfigDto dto = service.getProviderConfig("GITHUB");
		assertNotNull(dto);
		assertEquals("GITHUB", dto.getProviderId());

		when(configRepository.findByProviderIdIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());
		assertThrows(com.empay.user_management.exceptions.ResourceNotFoundException.class,
				() -> service.getProviderConfig("UNKNOWN"));
		assertThrows(IllegalArgumentException.class, () -> service.getProviderConfig("   "));
	}

	@Test
	@DisplayName("saveProviderConfig creating new provider defaults isEnabled to false when omitted (Finding S11)")
	void testSaveProviderConfig_CreatingNewProvider_DefaultsToDisabledWhenOmitted() {
		when(configRepository.findByProviderIdIgnoreCase("NEW_OIDC")).thenReturn(Optional.empty());
		when(configRepository.save(any(AuthProviderConfigEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(configRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

		SaveAuthProviderConfigRequest req = SaveAuthProviderConfigRequest.builder()
				.providerId("NEW_OIDC")
				.name("New OIDC Provider")
				.type("OIDC")
				.isEnabled(null) // omitted
				.build();

		AuthProviderConfigDto result = service.saveProviderConfig(req, "admin");
		assertNotNull(result);
		assertFalse(result.isEnabled());
	}

	@Test
	@DisplayName("toggleProviderStatus with null isEnabled on cached provider does not throw NPE (Finding T6)")
	void testToggleProviderStatus_NullIsEnabledOnOtherProviderDoesNotThrow() {
		AuthProviderConfigEntity nullEnabledEntity = AuthProviderConfigEntity.builder()
				.providerId("GOOGLE")
				.name("Google")
				.type("OIDC")
				.isEnabled(null)
				.displayOrder(3)
				.build();

		when(configRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(localPassEntity, nullEnabledEntity));
		service.refreshCache();

		when(configRepository.findByProviderIdIgnoreCase("LOCAL_PASSWORD")).thenReturn(Optional.of(localPassEntity));

		assertThrows(IllegalArgumentException.class,
				() -> service.toggleProviderStatus("LOCAL_PASSWORD", false, "admin"));
	}

	@Test
	@DisplayName("refreshCache transparently migrates unencrypted client secrets (Finding S6)")
	void testRefreshCache_MigratesPlaintextSecrets() {
		AuthProviderConfigEntity unencryptedEntity = AuthProviderConfigEntity.builder()
				.providerId("AZURE")
				.name("Azure")
				.type("OIDC")
				.clientSecret("plain-text-secret-value")
				.isEnabled('Y')
				.displayOrder(6)
				.build();

		when(configRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(unencryptedEntity));
		when(configRepository.save(any(AuthProviderConfigEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.refreshCache();

		verify(configRepository).save(unencryptedEntity);
	}
}

