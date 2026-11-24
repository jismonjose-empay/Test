package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.empay.user_management.dto.AuthProviderConfigDto;
import com.empay.user_management.dto.AuthProviderDto;
import com.empay.user_management.dto.UserAuthMethodDto;
import com.empay.user_management.dto.UserAuthMethodsOverviewResponse;
import com.empay.user_management.entities.AuthProviderConfigEntity;
import com.empay.user_management.exceptions.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class UserAuthMethodControllerTest {

    private MockMvc mockMvc;

    @Mock
    private com.empay.user_management.service.UserAuthMethodService authMethodService;

    @Mock
    private com.empay.user_management.service.AuthProviderConfigService authProviderConfigService;

    @InjectMocks
    private UserAuthMethodController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/auth-providers returns full system provider configurations")
    void testGetAllProviderConfigs() throws Exception {
        AuthProviderConfigDto dto = AuthProviderConfigDto.builder()
                .providerId("GITHUB")
                .name("GitHub")
                .type("OAUTH2")
                .isEnabled(true)
                .clientSecretMasked("••••••••")
                .hasClientSecret(true)
                .build();

        when(authProviderConfigService.getAllProviderConfigs()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/auth-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value("GITHUB"))
                .andExpect(jsonPath("$[0].clientSecretMasked").value("••••••••"))
                .andExpect(jsonPath("$[0].hasClientSecret").value(true));
    }

    @Test
    @DisplayName("PATCH /api/admin/auth-providers/{providerId}/status toggles provider active status")
    void testToggleProviderStatus() throws Exception {
        AuthProviderConfigDto dto = AuthProviderConfigDto.builder()
                .providerId("GITHUB")
                .isEnabled(false)
                .build();

        when(authProviderConfigService.toggleProviderStatus(eq("GITHUB"), eq(false), any()))
                .thenReturn(dto);

        mockMvc.perform(patch("/api/admin/auth-providers/GITHUB/status?enabled=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("GITHUB"))
                .andExpect(jsonPath("$.isEnabled").value(false))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/auth-methods returns user's auth methods")
    void testGetUserAuthMethods() throws Exception {
        UserAuthMethodsOverviewResponse overview = UserAuthMethodsOverviewResponse.builder()
                .userId("john_doe")
                .displayName("John Doe")
                .primaryMethod("LOCAL_PASSWORD")
                .methods(List.of(
                        UserAuthMethodDto.builder()
                                .provider("LOCAL_PASSWORD")
                                .providerName("Local Password")
                                .isLinked(true)
                                .isEnabled(true)
                                .build(),
                        UserAuthMethodDto.builder()
                                .provider("GITHUB")
                                .providerName("GitHub")
                                .isLinked(false)
                                .isEnabled(false)
                                .build()
                ))
                .build();

        when(authMethodService.getUserAuthMethods("john_doe")).thenReturn(overview);

        mockMvc.perform(get("/api/users/john_doe/auth-methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("john_doe"))
                .andExpect(jsonPath("$.primaryMethod").value("LOCAL_PASSWORD"))
                .andExpect(jsonPath("$.methods.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/auth-methods links GitHub method")
    void testConfigureAuthMethod() throws Exception {
        UserAuthMethodsOverviewResponse overview = UserAuthMethodsOverviewResponse.builder()
                .userId("john_doe")
                .methods(List.of(
                        UserAuthMethodDto.builder()
                                .provider("GITHUB")
                                .isLinked(true)
                                .isEnabled(true)
                                .externalUsername("octocat")
                                .build()
                ))
                .build();

        when(authMethodService.configureUserAuthMethod(eq("john_doe"), any(), any())).thenReturn(overview);

        String requestJson = "{\"provider\":\"GITHUB\",\"externalUsername\":\"octocat\",\"isEnabled\":true}";

        mockMvc.perform(post("/api/users/john_doe/auth-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods[0].provider").value("GITHUB"))
                .andExpect(jsonPath("$.methods[0].externalUsername").value("octocat"));
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/auth-methods/{provider} unlinks method")
    void testUnlinkAuthMethod() throws Exception {
        UserAuthMethodsOverviewResponse overview = UserAuthMethodsOverviewResponse.builder()
                .userId("john_doe")
                .methods(List.of())
                .build();

        when(authMethodService.unlinkUserAuthMethod(eq("john_doe"), eq("GITHUB"), any())).thenReturn(overview);

        mockMvc.perform(delete("/api/users/john_doe/auth-methods/GITHUB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("john_doe"));
    }

    @Test
    @DisplayName("GET /api/auth/providers returns public system auth providers")
    void testGetSupportedProviders() throws Exception {
        when(authMethodService.getSystemAuthProviders()).thenReturn(List.of(
                AuthProviderDto.builder().id("GITHUB").name("GitHub").build(),
                AuthProviderDto.builder().id("LOCAL_PASSWORD").name("Local Password").build()
        ));

        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("GITHUB"))
                .andExpect(jsonPath("$[1].id").value("LOCAL_PASSWORD"));
    }

    @Test
    @DisplayName("GET /api/auth/oauth/{provider}/authorize returns provider connection info")
    void testGetOAuthAuthorizationUrl() throws Exception {
        AuthProviderConfigEntity entity = AuthProviderConfigEntity.builder()
                .providerId("GITHUB")
                .name("GitHub")
                .type("OAUTH2")
                .isEnabled('Y')
                .authorizationUrl("https://github.com/login/oauth/authorize")
                .clientId("gh-client-id")
                .build();

        when(authProviderConfigService.getCachedProvider("GITHUB")).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/api/auth/oauth/GITHUB/authorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GITHUB"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.clientId").value("gh-client-id"));
    }

    @Test
    @DisplayName("GET /api/admin/auth-providers/{providerId} returns specific provider config")
    void testGetProviderConfigById() throws Exception {
        AuthProviderConfigDto dto = AuthProviderConfigDto.builder()
                .providerId("GOOGLE")
                .name("Google")
                .type("OIDC")
                .isEnabled(true)
                .build();

        when(authProviderConfigService.getProviderConfig("GOOGLE")).thenReturn(dto);

        mockMvc.perform(get("/api/admin/auth-providers/GOOGLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("GOOGLE"))
                .andExpect(jsonPath("$.name").value("Google"));
    }

    @Test
    @DisplayName("POST /api/admin/auth-providers creates new provider config")
    void testCreateProviderConfig() throws Exception {
        AuthProviderConfigDto dto = AuthProviderConfigDto.builder()
                .providerId("AZURE")
                .name("Microsoft Azure")
                .type("OIDC")
                .isEnabled(true)
                .build();

        when(authProviderConfigService.saveProviderConfig(any(), any())).thenReturn(dto);

        String json = "{\"providerId\":\"AZURE\",\"name\":\"Microsoft Azure\",\"type\":\"OIDC\",\"isEnabled\":true}";

        mockMvc.perform(post("/api/admin/auth-providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("AZURE"));
    }

    @Test
    @DisplayName("PUT /api/admin/auth-providers/{providerId} updates existing provider config")
    void testUpdateProviderConfig() throws Exception {
        AuthProviderConfigDto dto = AuthProviderConfigDto.builder()
                .providerId("AZURE")
                .name("Azure Updated")
                .type("OIDC")
                .isEnabled(true)
                .build();

        when(authProviderConfigService.saveProviderConfig(any(), any())).thenReturn(dto);

        String json = "{\"providerId\":\"AZURE\",\"name\":\"Azure Updated\",\"type\":\"OIDC\",\"isEnabled\":true}";

        mockMvc.perform(put("/api/admin/auth-providers/AZURE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Azure Updated"));
    }

    @Test
    @DisplayName("DELETE /api/admin/auth-providers/{providerId} deletes provider config")
    void testDeleteProviderConfig() throws Exception {
        mockMvc.perform(delete("/api/admin/auth-providers/KEYCLOAK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("PATCH /api/admin/auth-providers/{providerId}/status with body toggles status")
    void testToggleStatusWithBody() throws Exception {
        AuthProviderConfigDto dto = AuthProviderConfigDto.builder()
                .providerId("GITHUB")
                .isEnabled(false)
                .build();

        when(authProviderConfigService.toggleProviderStatus(eq("GITHUB"), eq(false), any()))
                .thenReturn(dto);

        mockMvc.perform(patch("/api/admin/auth-providers/GITHUB/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("GITHUB"))
                .andExpect(jsonPath("$.isEnabled").value(false));
    }

    @Test
    @DisplayName("PATCH /api/admin/auth-providers/{providerId}/status rejects ambiguous request with 400 Bad Request (Finding S11)")
    void testToggleStatus_AmbiguousRequest_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/auth-providers/GITHUB/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"typoField\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/users/{userId}/auth-methods/{provider} updates user method")
    void testUpdateUserAuthMethod() throws Exception {
        UserAuthMethodsOverviewResponse overview = UserAuthMethodsOverviewResponse.builder()
                .userId("john_doe")
                .methods(List.of(
                        UserAuthMethodDto.builder()
                                .provider("GITHUB")
                                .isEnabled(false)
                                .build()
                ))
                .build();

        when(authMethodService.configureUserAuthMethod(eq("john_doe"), any(), any()))
                .thenReturn(overview);

        mockMvc.perform(put("/api/users/john_doe/auth-methods/GITHUB")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("john_doe"));
    }

    @Test
    @DisplayName("GET /api/auth/oauth/{provider}/authorize returns configured authorization URL")
    void testGetOAuthAuthorizationUrl_Configured() throws Exception {
        AuthProviderConfigEntity entity = AuthProviderConfigEntity.builder()
                .providerId("GITHUB")
                .name("GitHub")
                .type("OAUTH2")
                .clientId("test-client-id")
                .authorizationUrl("https://github.com/login/oauth/authorize")
                .scopes("read:user user:email")
                .redirectUri("http://localhost:5173/oauth/callback")
                .isEnabled('Y')
                .build();

        when(authProviderConfigService.getCachedProvider("GITHUB")).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/api/auth/oauth/github/authorize?state=xyz123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GITHUB"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.clientId").value("test-client-id"))
                .andExpect(jsonPath("$.targetUrl").exists());
    }

    @Test
    @DisplayName("GET /api/auth/oauth/{provider}/authorize returns NOT_CONFIGURED when clientId is missing")
    void testGetOAuthAuthorizationUrl_NotConfigured() throws Exception {
        AuthProviderConfigEntity entity = AuthProviderConfigEntity.builder()
                .providerId("GITHUB")
                .name("GitHub")
                .type("OAUTH2")
                .clientId(null)
                .isEnabled('Y')
                .build();

        when(authProviderConfigService.getCachedProvider("GITHUB")).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/api/auth/oauth/github/authorize"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("NOT_CONFIGURED"));
    }

    @Test
    @DisplayName("GET /api/auth/oauth/{provider}/login redirects to provider authorization page")
    void testRedirectToProviderLogin() throws Exception {
        AuthProviderConfigEntity entity = AuthProviderConfigEntity.builder()
                .providerId("GITHUB")
                .name("GitHub")
                .type("OAUTH2")
                .clientId("test-client-id")
                .authorizationUrl("https://github.com/login/oauth/authorize")
                .scopes("read:user")
                .redirectUri("http://localhost:5173/oauth/callback")
                .isEnabled('Y')
                .build();

        when(authProviderConfigService.getCachedProvider("GITHUB")).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/api/auth/oauth/github/login?state=testState"))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"));
    }

    @Test
    @DisplayName("POST /api/auth/oauth/login delegates to authMethodService")
    void testOAuthLogin_DelegatesToService() throws Exception {
        when(authMethodService.authenticateWithOAuth(any(), any()))
                .thenReturn(ResponseEntity.ok(Map.of("authenticated", true, "userId", "admin")));

        mockMvc.perform(post("/api/auth/oauth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"GITHUB\",\"code\":\"auth-code-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.userId").value("admin"));
    }

    @Test
    @DisplayName("GET /api/auth/oauth/{provider}/authorize returns DISABLED when provider is not enabled")
    void testGetOAuthAuthorizationUrl_Disabled() throws Exception {
        AuthProviderConfigEntity entity = AuthProviderConfigEntity.builder()
                .providerId("GITHUB")
                .isEnabled('N')
                .build();
        when(authProviderConfigService.getCachedProvider("GITHUB")).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/api/auth/oauth/github/authorize"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(get("/api/auth/oauth/github/login"))
                .andExpect(status().isBadRequest());
    }
}
