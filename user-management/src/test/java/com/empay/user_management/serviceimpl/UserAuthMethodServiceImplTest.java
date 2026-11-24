package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.empay.user_management.dto.ConfigureAuthMethodRequest;
import com.empay.user_management.dto.OAuthLoginRequest;
import com.empay.user_management.dto.UserAuthMethodsOverviewResponse;
import com.empay.user_management.entities.AuthProviderConfigEntity;
import com.empay.user_management.entities.UserAuthMethodEntity;
import com.empay.user_management.entities.UserCredentialEntity;
import com.empay.user_management.entities.UserRoleEntityV2;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.repository.LoginAttemptRepository;
import com.empay.user_management.repository.UserAuthMethodRepository;
import com.empay.user_management.repository.UserCredentialRepository;
import com.empay.user_management.repository.UserRoleRepositoryV2;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.service.AuthProviderConfigService;
import com.empay.user_management.utils.JwtUtils;

@ExtendWith(MockitoExtension.class)
class UserAuthMethodServiceImplTest {

	@Mock
	private UsersRepositoryV2 usersRepository;

	@Mock
	private UserAuthMethodRepository authMethodRepository;

	@Mock
	private UserCredentialRepository credentialRepository;

	@Mock
	private AuthProviderConfigService authProviderConfigService;

	@Mock
	private LoginAttemptRepository loginAttemptRepository;

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private UserRoleRepositoryV2 userRoleRepository;

	@InjectMocks
	private UserAuthMethodServiceImpl service;

	private UsersEntityV2 testUser;
	private List<AuthProviderConfigEntity> defaultSystemProviders;

	@BeforeEach
	void setUp() {
		testUser = new UsersEntityV2();
		testUser.setSerialNumber(101);
		testUser.setUserId("dev_officer");
		testUser.setDisplayName("Developer Officer");
		testUser.setEmail("dev@empaysoft.com");
		testUser.setStatus("ACTIVE");
		testUser.setIsBreakGlass('N');

		defaultSystemProviders = List.of(
				AuthProviderConfigEntity.builder().providerId("LOCAL_PASSWORD").name("Local Password").type("PASSWORD").isEnabled('Y').build(),
				AuthProviderConfigEntity.builder().providerId("GITHUB").name("GitHub").type("OAUTH2").isEnabled('Y').build(),
				AuthProviderConfigEntity.builder().providerId("GOOGLE").name("Google").type("OIDC").isEnabled('Y').build(),
				AuthProviderConfigEntity.builder().providerId("MICROSOFT").name("Microsoft Azure AD").type("OIDC").isEnabled('Y').build(),
				AuthProviderConfigEntity.builder().providerId("KEYCLOAK").name("Corporate Keycloak SSO").type("OIDC").isEnabled('Y').build()
		);

            ReflectionTestUtils.setField(service, "directLoginEnabled", true);
	}

	@Test
	@DisplayName("Should return user auth methods overview with proper linking flags")
	void testGetUserAuthMethods_Success() {
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authProviderConfigService.getAllCachedProviders()).thenReturn(defaultSystemProviders);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setPasswordHash("hashed_pass");
		when(credentialRepository.findByUserSerialNo(101)).thenReturn(cred);

		UserAuthMethodEntity githubLink = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalUsername("octocat")
				.isEnabled('Y')
				.isPrimary('N')
				.build();

		when(authMethodRepository.findByUserSerialNo(101)).thenReturn(List.of(githubLink));

		UserAuthMethodsOverviewResponse res = service.getUserAuthMethods("dev_officer");

		assertNotNull(res);
		assertEquals("dev_officer", res.getUserId());
		assertEquals(5, res.getMethods().size());

		var localMethod = res.getMethods().stream().filter(m -> "LOCAL_PASSWORD".equals(m.getProvider())).findFirst().orElseThrow();
		assertTrue(localMethod.isLinked());
		assertTrue(localMethod.isPrimary());

		var githubMethod = res.getMethods().stream().filter(m -> "GITHUB".equals(m.getProvider())).findFirst().orElseThrow();
		assertTrue(githubMethod.isLinked());
		assertEquals("octocat", githubMethod.getExternalUsername());
	}

	@Test
	@DisplayName("Should configure new auth method for user and update primary selection")
	void testConfigureUserAuthMethod_NewMethod_Success() {
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").name("GitHub").isEnabled('Y').build()));
		when(authMethodRepository.findByUserSerialNoAndProvider(101, "GITHUB")).thenReturn(Optional.empty());
		when(authProviderConfigService.getAllCachedProviders()).thenReturn(defaultSystemProviders);

		ConfigureAuthMethodRequest req = ConfigureAuthMethodRequest.builder()
				.provider("GITHUB")
				.externalSubject("github_sub_999")
				.externalUsername("octocat")
				.isPrimary(true)
				.isEnabled(true)
				.build();

		UserAuthMethodsOverviewResponse res = service.configureUserAuthMethod("dev_officer", req, "ADMIN");

		assertNotNull(res);
		verify(authMethodRepository).save(any(UserAuthMethodEntity.class));
		verify(usersRepository).save(testUser);
		assertEquals("github_sub_999", testUser.getIdpSubject());
	}

	@Test
	@DisplayName("Should unlink user auth method and clear idp subject if linked")
	void testUnlinkUserAuthMethod_Success() {
		testUser.setIdpSubject("github_sub_999");
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authProviderConfigService.getAllCachedProviders()).thenReturn(defaultSystemProviders);

		UserAuthMethodEntity githubLink = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalSubject("github_sub_999")
				.isPrimary('Y')
				.build();

		when(authMethodRepository.findByUserSerialNoAndProvider(101, "GITHUB"))
				.thenReturn(Optional.of(githubLink));
		when(authMethodRepository.findByUserSerialNo(101)).thenReturn(List.of());

		UserAuthMethodsOverviewResponse res = service.unlinkUserAuthMethod("dev_officer", "GITHUB", "ADMIN");

		assertNotNull(res);
		verify(authMethodRepository).delete(githubLink);
		verify(usersRepository).save(testUser);
		assertEquals(null, testUser.getIdpSubject());
	}

	@Test
	@DisplayName("Should reject configuring method when provider does not exist")
	void testConfigureUserAuthMethod_UnknownProvider_ThrowsException() {
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authProviderConfigService.getCachedProvider("UNKNOWN"))
				.thenReturn(Optional.empty());

		ConfigureAuthMethodRequest req = ConfigureAuthMethodRequest.builder()
				.provider("UNKNOWN")
				.build();

		assertThrows(IllegalArgumentException.class, () ->
				service.configureUserAuthMethod("dev_officer", req, "ADMIN"));
	}

	@Test
	@DisplayName("Should reject linking identity if already bound to a different user")
	void testConfigureUserAuthMethod_DuplicateSubject_ThrowsException() {
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
		when(authMethodRepository.existsByProviderAndExternalSubjectAndUserSerialNoNot("GITHUB", "dup_subject", 101))
				.thenReturn(true);

		ConfigureAuthMethodRequest req = ConfigureAuthMethodRequest.builder()
				.provider("GITHUB")
				.externalSubject("dup_subject")
				.build();

		assertThrows(IllegalArgumentException.class, () ->
				service.configureUserAuthMethod("dev_officer", req, "ADMIN"));
	}

	@Test
	@DisplayName("Should reject linking handle if already bound to a different user")
	void testConfigureUserAuthMethod_DuplicateUsername_ThrowsException() {
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
		when(authMethodRepository.existsByProviderAndExternalUsernameAndUserSerialNoNot("GITHUB", "dup_user", 101))
				.thenReturn(true);

		ConfigureAuthMethodRequest req = ConfigureAuthMethodRequest.builder()
				.provider("GITHUB")
				.externalUsername("dup_user")
				.build();

		assertThrows(IllegalArgumentException.class, () ->
				service.configureUserAuthMethod("dev_officer", req, "ADMIN"));
	}

	@Test
	@DisplayName("Should update existing auth method fields when already present")
	void testConfigureUserAuthMethod_UpdateExistingMethod() {
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

		UserAuthMethodEntity existing = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalSubject("old_sub")
				.externalUsername("old_user")
				.isEnabled('Y')
				.build();

		when(authMethodRepository.findByUserSerialNoAndProvider(101, "GITHUB"))
				.thenReturn(Optional.of(existing));
		when(authProviderConfigService.getAllCachedProviders()).thenReturn(defaultSystemProviders);

		ConfigureAuthMethodRequest req = ConfigureAuthMethodRequest.builder()
				.provider("GITHUB")
				.externalSubject("new_sub")
				.externalUsername("new_user")
				.externalEmail("new@empay.com")
				.isEnabled(false)
				.metadataJson("{\"scope\":\"read\"}")
				.build();

		UserAuthMethodsOverviewResponse res = service.configureUserAuthMethod("dev_officer", req, "ADMIN");
		assertNotNull(res);
		assertEquals("new_sub", existing.getExternalSubject());
		assertEquals("new_user", existing.getExternalUsername());
		assertEquals("new@empay.com", existing.getExternalEmail());
		assertEquals('N', existing.getIsEnabled());
		assertEquals("{\"scope\":\"read\"}", existing.getMetadataJson());
	}

	@Test
	@DisplayName("Should promote remaining method to primary when primary method is unlinked")
	void testUnlinkUserAuthMethod_PromotesRemainingPrimary() {
		testUser.setIdpSubject("github_sub");
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authProviderConfigService.getAllCachedProviders()).thenReturn(defaultSystemProviders);

		UserAuthMethodEntity primaryMethod = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalSubject("github_sub")
				.isPrimary('Y')
				.isEnabled('Y')
				.build();

		UserAuthMethodEntity nextMethod = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GOOGLE")
				.externalSubject("google_sub_123")
				.isPrimary('N')
				.isEnabled('Y')
				.build();

		when(authMethodRepository.findByUserSerialNoAndProvider(101, "GITHUB"))
				.thenReturn(Optional.of(primaryMethod));
		when(authMethodRepository.findByUserSerialNo(101))
				.thenReturn(List.of(primaryMethod, nextMethod));

		UserAuthMethodsOverviewResponse res = service.unlinkUserAuthMethod("dev_officer", "GITHUB", "ADMIN");
		assertNotNull(res);
		assertEquals('Y', nextMethod.getIsPrimary());
		assertEquals("google_sub_123", testUser.getIdpSubject());
	}

	@Test
	@DisplayName("Should resolve linked auth method via usersRepository idpSubject")
	void testAuthenticateWithOAuth_ResolvedViaIdpSubject() {
		when(authProviderConfigService.getCachedProvider("GOOGLE"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GOOGLE").isEnabled('Y').build()));

		when(authMethodRepository.findByProviderAndExternalSubject("GOOGLE", "google_idp_123"))
                    .thenReturn(Optional.empty());

		testUser.setIdpSubject("google_idp_123");
		when(usersRepository.findByIdpSubject("google_idp_123")).thenReturn(testUser);

		UserAuthMethodEntity link = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GOOGLE")
				.externalSubject("google_idp_123")
				.isEnabled('Y')
				.build();
		when(authMethodRepository.findByUserSerialNoAndProvider(101, "GOOGLE")).thenReturn(Optional.of(link));
		when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);
		when(credentialRepository.findByUserSerialNo(101)).thenReturn(null);

		UserRoleEntityV2 role = new UserRoleEntityV2();
		role.setSerialNumber(101);
		role.setRoleCode("OFFICER");
		when(userRoleRepository.findBySerialNumber(101)).thenReturn(List.of(role));
		when(jwtUtils.generateToken("dev_officer", 101, "OFFICER")).thenReturn("officer_token");

		OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GOOGLE")
				.identity("google_idp_123")
				.build();

		ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.OK, res.getStatusCode());
	}

    @Test
	@DisplayName("Should resolve via usersRepository idpSubject migration read-only lookup when method row already exists")
    void testAuthenticateWithOAuth_ResolvedViaIdpSubject_ReadOnlyLookup() {
    	when(authProviderConfigService.getCachedProvider("GITHUB"))
    			.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
        
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "256100996"))
				.thenReturn(Optional.empty());
        
        testUser.setIdpSubject("256100996");
		testUser.setAuthMode("BOTH");
        when(usersRepository.findByIdpSubject("256100996")).thenReturn(testUser);

        UserAuthMethodEntity existingMethod = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalSubject("256100996")
				.isEnabled('Y')
				.build();
        when(authMethodRepository.findByUserSerialNoAndProvider(101, "GITHUB")).thenReturn(Optional.of(existingMethod));
        when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);
        when(credentialRepository.findByUserSerialNo(101)).thenReturn(null);
        
        UserRoleEntityV2 role = new UserRoleEntityV2();
		role.setSerialNumber(101);
        role.setRoleCode("OFFICER");
        when(userRoleRepository.findBySerialNumber(101)).thenReturn(List.of(role));
        when(jwtUtils.generateToken("dev_officer", 101, "OFFICER")).thenReturn("officer_token");
        
        OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GITHUB")
        		.identity("256100996")
                .build();
                
        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("BOTH", testUser.getAuthMode());
        // Only lastLoginAt timestamp is updated on successful authentication
        verify(authMethodRepository, times(1)).save(existingMethod);
        verify(usersRepository, never()).save(any(UsersEntityV2.class));
    }

    @Test
	@DisplayName("Should resolve via prefixed usersRepository idpSubject (provider:cleanSubject) when exact match is absent")
    void testAuthenticateWithOAuth_ResolvedViaPrefixedIdpSubject() {
    	when(authProviderConfigService.getCachedProvider("GITHUB"))
    			.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
        
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "256100996"))
				.thenReturn(Optional.empty());
        
        testUser.setIdpSubject("GITHUB:256100996");
		testUser.setAuthMode("OIDC");
        when(usersRepository.findByIdpSubject("256100996")).thenReturn(null);
        when(usersRepository.findByIdpSubject("GITHUB:256100996")).thenReturn(testUser);

        UserAuthMethodEntity existingMethod = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalSubject("256100996")
				.isEnabled('Y')
				.build();
        when(authMethodRepository.findByUserSerialNoAndProvider(101, "GITHUB")).thenReturn(Optional.of(existingMethod));
        when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);
        when(credentialRepository.findByUserSerialNo(101)).thenReturn(null);
        
        UserRoleEntityV2 role = new UserRoleEntityV2();
		role.setSerialNumber(101);
        role.setRoleCode("OFFICER");
        when(userRoleRepository.findBySerialNumber(101)).thenReturn(List.of(role));
        when(jwtUtils.generateToken("dev_officer", 101, "OFFICER")).thenReturn("officer_token");
        
        OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GITHUB")
        		.identity("256100996")
                .build();
                
        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(authMethodRepository, times(1)).save(existingMethod);
    }

    @Test
	@DisplayName("Should reject with 401 when idpSubject matches user but no method row exists (never auto-create)")
    void testAuthenticateWithOAuth_ResolvedViaIdpSubject_NoMethodRow_ReturnsUnauthorized() {
    	when(authProviderConfigService.getCachedProvider("GITHUB"))
    			.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
        
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "256100996"))
				.thenReturn(Optional.empty());
        
        testUser.setIdpSubject("256100996");
		testUser.setAuthMode("BOTH");
        when(usersRepository.findByIdpSubject("256100996")).thenReturn(testUser);
        when(authMethodRepository.findByUserSerialNoAndProvider(101, "GITHUB")).thenReturn(Optional.empty());

        OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GITHUB")
        		.identity("256100996")
                .build();

        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        verify(authMethodRepository, never()).save(any(UserAuthMethodEntity.class));
    }

    @Test
	@DisplayName("Should reject with 401 when user is inactive or suspended")
    void testAuthenticateWithOAuth_UserInactive_ReturnsUnauthorized() {
    	when(authProviderConfigService.getCachedProvider("GITHUB"))
    			.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
        
        UserAuthMethodEntity existingMethod = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalSubject("256100996")
				.isEnabled('Y')
				.build();
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "256100996"))
				.thenReturn(Optional.of(existingMethod));
        testUser.setStatus("SUSPENDED");
        when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);

        OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GITHUB")
        		.identity("256100996")
                .build();

        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        verify(authMethodRepository, never()).save(existingMethod);
    }

    @Test
	@DisplayName("Should reject with 401 when auth method is disabled (isEnabled = 'N' or null)")
    void testAuthenticateWithOAuth_MethodDisabled_ReturnsUnauthorized() {
    	when(authProviderConfigService.getCachedProvider("GITHUB"))
    			.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
        
        UserAuthMethodEntity existingMethod = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalSubject("256100996")
				.isEnabled('N')
				.build();
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "256100996"))
				.thenReturn(Optional.of(existingMethod));
        testUser.setStatus("ACTIVE");
        when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);

        OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GITHUB")
        		.identity("256100996")
                .build();

        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        verify(authMethodRepository, never()).save(existingMethod);
    }

    @Test
	@DisplayName("Should reject with 401 when user authMode is LOCAL (never escalated to BOTH)")
    void testAuthenticateWithOAuth_UserLocalAuthMode_ReturnsUnauthorized() {
    	when(authProviderConfigService.getCachedProvider("GITHUB"))
    			.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
        
        UserAuthMethodEntity existingMethod = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalSubject("256100996")
				.isEnabled('Y')
				.build();
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "256100996"))
				.thenReturn(Optional.of(existingMethod));
        testUser.setStatus("ACTIVE");
        testUser.setAuthMode("LOCAL");
        when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);

        OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GITHUB")
        		.identity("256100996")
                .build();

        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        verify(authMethodRepository, never()).save(existingMethod);
    }

	@Test
	@DisplayName("Should reject authentication when no MARS user is linked to external identity")
	void testAuthenticateWithOAuth_UnlinkedIdentity_ReturnsUnauthorized() {
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));
		when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "unlinked_id"))
                    .thenReturn(Optional.empty());
		when(usersRepository.findByIdpSubject("unlinked_id")).thenReturn(null);

		OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GITHUB")
				.identity("unlinked_id")
				.build();

		ResponseEntity<Object> response = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertEquals(false, body.get("authenticated"));
		assertTrue(body.get("message").toString().contains("No MARS account is linked"));
	}

	@Test
	@DisplayName("Should authenticate successfully when MARS user is created and method is linked and active")
	void testAuthenticateWithOAuth_LinkedAndActiveUser_Success() {
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

		UserAuthMethodEntity link = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
                        .externalSubject("octocat")
				.isEnabled('Y')
				.build();

            when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "octocat"))
				.thenReturn(Optional.of(link));
		when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);
		when(credentialRepository.findByUserSerialNo(101)).thenReturn(null);
		when(jwtUtils.generateToken(eq("dev_officer"), eq(101), any())).thenReturn("mocked_jwt_token");

		OAuthLoginRequest req = OAuthLoginRequest.builder()
				.provider("GITHUB")
				.identity("octocat")
				.build();

		ResponseEntity<Object> response = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.OK, response.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertEquals(true, body.get("authenticated"));
            assertFalse(body.containsKey("token"), "Session token must not be in OAuth login response body (Finding S5)");
            assertFalse(body.containsKey("accessToken"), "Session accessToken must not be in OAuth login response body (Finding S5)");
            assertFalse(body.containsKey("authToken"), "Session authToken must not be in OAuth login response body (Finding S5)");
            assertNull(response.getHeaders().getFirst(org.springframework.http.HttpHeaders.AUTHORIZATION), "Authorization header must not be set on OAuth login response (Finding S5)");
            assertNotNull(response.getHeaders().get(org.springframework.http.HttpHeaders.SET_COOKIE), "Session must be set via HttpOnly cookies");
                      assertEquals("dev_officer", body.get("userId"));
	}

	@Test
	@DisplayName("Should return 400 Bad Request when provider is blank or not enabled")
	void testAuthenticateWithOAuth_InvalidProvider_ReturnsBadRequest() {
		ResponseEntity<Object> resNull = service.authenticateWithOAuth(null, new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, resNull.getStatusCode());

		OAuthLoginRequest reqBlank = OAuthLoginRequest.builder().provider("   ").build();
		ResponseEntity<Object> resBlank = service.authenticateWithOAuth(reqBlank, new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, resBlank.getStatusCode());

		when(authProviderConfigService.getCachedProvider("DISABLED_PROV"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("DISABLED_PROV").isEnabled('N').build()));
		OAuthLoginRequest reqDisabled = OAuthLoginRequest.builder().provider("DISABLED_PROV").identity("user1").build();
		ResponseEntity<Object> resDisabled = service.authenticateWithOAuth(reqDisabled, new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, resDisabled.getStatusCode());
	}

	@Test
	@DisplayName("Should return 400 Bad Request when neither code nor identity is supplied")
	void testAuthenticateWithOAuth_NoCodeOrIdentity_ReturnsBadRequest() {
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

		OAuthLoginRequest req = OAuthLoginRequest.builder().provider("GITHUB").build();
		ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
	}

	@Test
	@DisplayName("Should return 401 Unauthorized when user is not found or inactive")
	void testAuthenticateWithOAuth_UserNotFoundOrInactive_ReturnsUnauthorized() {
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

		UserAuthMethodEntity link = UserAuthMethodEntity.builder()
				.userSerialNo(999)
				.provider("GITHUB")
				.externalUsername("octocat")
				.isEnabled('Y')
				.build();
		when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "octocat")).thenReturn(Optional.of(link));
		when(usersRepository.findBySerialNumber(999)).thenReturn(null);

		OAuthLoginRequest req = OAuthLoginRequest.builder().provider("GITHUB").identity("octocat").build();
		ResponseEntity<Object> resNotFound = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resNotFound.getStatusCode());

		UsersEntityV2 inactiveUser = new UsersEntityV2();
		inactiveUser.setUserId("inactive_user");
		inactiveUser.setSerialNumber(102);
		inactiveUser.setStatus("SUSPENDED");
		link.setUserSerialNo(102);
		when(usersRepository.findBySerialNumber(102)).thenReturn(inactiveUser);
		ResponseEntity<Object> resInactive = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resInactive.getStatusCode());
	}

	@Test
	@DisplayName("Should return 401 Unauthorized when method is disabled or account is locked")
	void testAuthenticateWithOAuth_MethodDisabledOrLocked_ReturnsUnauthorized() {
		when(authProviderConfigService.getCachedProvider("GITHUB"))
				.thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

		UserAuthMethodEntity disabledLink = UserAuthMethodEntity.builder()
				.userSerialNo(101)
				.provider("GITHUB")
				.externalUsername("octocat")
				.isEnabled('N')
				.build();
		when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "octocat")).thenReturn(Optional.of(disabledLink));
		when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);

		OAuthLoginRequest req = OAuthLoginRequest.builder().provider("GITHUB").identity("octocat").build();
		ResponseEntity<Object> resDisabled = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resDisabled.getStatusCode());

		disabledLink.setIsEnabled('Y');
		UserCredentialEntity lockedCred = new UserCredentialEntity();
		lockedCred.setLockedUntil(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(15));
		when(credentialRepository.findByUserSerialNo(101)).thenReturn(lockedCred);

		ResponseEntity<Object> resLocked = service.authenticateWithOAuth(req, new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resLocked.getStatusCode());
	}

	@Test
	@DisplayName("getSystemAuthProviders delegates to authProviderConfigService")
	void testGetSystemAuthProviders() {
		when(authProviderConfigService.getPublicEnabledProviders()).thenReturn(List.of());
		List<?> providers = service.getSystemAuthProviders();
		assertNotNull(providers);
	}

	@Test
	@DisplayName("findUserOrThrow throws IllegalArgumentException for blank and ResourceNotFoundException when missing")
	void testFindUserOrThrow_Validations() {
		assertThrows(IllegalArgumentException.class, () -> service.getUserAuthMethods(null));
		assertThrows(IllegalArgumentException.class, () -> service.getUserAuthMethods("   "));

		when(usersRepository.findByUserId("non_existent")).thenReturn(null);
		assertThrows(com.empay.user_management.exceptions.ResourceNotFoundException.class,
				() -> service.getUserAuthMethods("non_existent"));
	}

	@Test
	@DisplayName("configureUserAuthMethod validates null and blank request fields")
	void testConfigureUserAuthMethod_BlankValidations() {
		assertThrows(IllegalArgumentException.class, () -> service.configureUserAuthMethod("dev_officer", null, "ADMIN"));

		ConfigureAuthMethodRequest reqBlank = ConfigureAuthMethodRequest.builder().provider("   ").build();
		assertThrows(IllegalArgumentException.class, () -> service.configureUserAuthMethod("dev_officer", reqBlank, "ADMIN"));
	}

	@Test
	@DisplayName("unlinkUserAuthMethod throws when method is not linked")
	void testUnlinkUserAuthMethod_NotLinked() {
		when(usersRepository.findByUserId("dev_officer")).thenReturn(testUser);
		when(authMethodRepository.findByUserSerialNoAndProvider(101, "KEYCLOAK")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
				() -> service.unlinkUserAuthMethod("dev_officer", "KEYCLOAK", "ADMIN"));
	}

    @Test
    @DisplayName("Direct OAuth login is disabled by default returning 403 (Finding S1)")
    void testAuthenticateWithOAuth_DirectLoginDisabledByDefault_ReturnsForbidden() {
        ReflectionTestUtils.setField(service, "directLoginEnabled", false);
        when(authProviderConfigService.getCachedProvider("GITHUB"))
                .thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

        OAuthLoginRequest req = OAuthLoginRequest.builder().provider("GITHUB").identity("direct_user").build();
        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("Direct OAuth identity login is disabled"));
    }

    @Test
    @DisplayName("Null isEnabled on USER_AUTH_METHOD is treated as disabled (Finding S2)")
    void testAuthenticateWithOAuth_NullIsEnabled_TreatedAsDisabled() {
        when(authProviderConfigService.getCachedProvider("GITHUB"))
                .thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

        UserAuthMethodEntity nullEnabledLink = UserAuthMethodEntity.builder()
                .userSerialNo(101)
                .provider("GITHUB")
                .externalSubject("octocat_null")
                .isEnabled(null) // Finding S2
                .build();
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "octocat_null"))
                .thenReturn(Optional.of(nullEnabledLink));
        when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);

        OAuthLoginRequest req = OAuthLoginRequest.builder().provider("GITHUB").identity("octocat_null").build();
        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("has been disabled"));
    }

    @Test
    @DisplayName("User with LOCAL authMode cannot authenticate via OAuth (Finding S2)")
    void testAuthenticateWithOAuth_AuthModeLocal_ReturnsUnauthorized() {
        when(authProviderConfigService.getCachedProvider("GITHUB"))
                .thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

        UserAuthMethodEntity link = UserAuthMethodEntity.builder()
                .userSerialNo(101)
                .provider("GITHUB")
                .externalSubject("octocat_local")
                .isEnabled('Y')
                .build();
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "octocat_local"))
                .thenReturn(Optional.of(link));

        testUser.setAuthMode("LOCAL"); // Finding S2
        when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);

        OAuthLoginRequest req = OAuthLoginRequest.builder().provider("GITHUB").identity("octocat_local").build();
        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("OAuth login is disabled for your account"));
    }

    @Test
    @DisplayName("User with PASSWORD_ONLY authMode cannot authenticate via OAuth (Finding S2)")
    void testAuthenticateWithOAuth_AuthModePasswordOnly_ReturnsUnauthorized() {
        when(authProviderConfigService.getCachedProvider("GITHUB"))
                .thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

        UserAuthMethodEntity link = UserAuthMethodEntity.builder()
                .userSerialNo(101)
                .provider("GITHUB")
                .externalSubject("octocat_pwonly")
                .isEnabled('Y')
                .build();
        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "octocat_pwonly"))
                .thenReturn(Optional.of(link));

        testUser.setAuthMode("PASSWORD_ONLY");
        when(usersRepository.findBySerialNumber(101)).thenReturn(testUser);

        OAuthLoginRequest req = OAuthLoginRequest.builder().provider("GITHUB").identity("octocat_pwonly").build();
        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("OAuth login is disabled for your account"));
    }

    @Test
    @DisplayName("OAuth authentication returns unauthorized when neither method nor legacy idp_subject matches")
    void testAuthenticateWithOAuth_UnlinkedSubject_ReturnsUnauthorized() {
        when(authProviderConfigService.getCachedProvider("GITHUB"))
                .thenReturn(Optional.of(AuthProviderConfigEntity.builder().providerId("GITHUB").isEnabled('Y').build()));

        when(authMethodRepository.findByProviderAndExternalSubject("GITHUB", "unknown_sub")).thenReturn(Optional.empty());
        when(usersRepository.findByIdpSubject("unknown_sub")).thenReturn(null);
        when(usersRepository.findByIdpSubject("GITHUB:unknown_sub")).thenReturn(null);

        OAuthLoginRequest req = OAuthLoginRequest.builder().provider("GITHUB").identity("unknown_sub").build();
        ResponseEntity<Object> res = service.authenticateWithOAuth(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("No MARS account is linked"));
    }

    @Test
    @DisplayName("SSRF protection: validateOutboundEndpoint allows valid HTTPS endpoint")
    void testValidateOutboundEndpoint_ValidHttps_Success() {
        java.net.URI uri = ReflectionTestUtils.invokeMethod(service, "validateOutboundEndpoint",
                "https://accounts.google.com/o/oauth2/token", "GOOGLE", "Token URL");
        assertNotNull(uri);
        assertEquals("https", uri.getScheme());
        assertEquals("accounts.google.com", uri.getHost());
    }

    @Test
    @DisplayName("SSRF protection: validateOutboundEndpoint rejects insecure schemes")
    void testValidateOutboundEndpoint_InsecureScheme_ThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "validateOutboundEndpoint",
                        "http://accounts.google.com/token", "GOOGLE", "Token URL"));
        assertThrows(SecurityException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "validateOutboundEndpoint",
                        "ftp://accounts.google.com/token", "GOOGLE", "Token URL"));
    }

    @Test
    @DisplayName("SSRF protection: validateOutboundEndpoint rejects localhost and metadata services")
    void testValidateOutboundEndpoint_LocalhostAndMetadata_ThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "validateOutboundEndpoint",
                        "https://localhost/token", "LOCAL", "Token URL"));
        assertThrows(SecurityException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "validateOutboundEndpoint",
                        "https://metadata.google.internal/token", "GCP", "Token URL"));
    }

    @Test
    @DisplayName("SSRF protection: validateOutboundEndpoint rejects null or blank URL")
    void testValidateOutboundEndpoint_BlankUrl_ThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "validateOutboundEndpoint",
                        "", "GOOGLE", "Token URL"));
        assertThrows(IllegalStateException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "validateOutboundEndpoint",
                        null, "GOOGLE", "Token URL"));
    }
}
