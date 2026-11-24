package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.empay.user_management.dto.PasswordValidationResult;
import com.empay.user_management.dto.UserContext;
import com.empay.user_management.entities.LoginAttemptEntity;
import com.empay.user_management.entities.UserAuthMethodEntity;
import com.empay.user_management.entities.UserCredentialEntity;
import com.empay.user_management.entities.UsersEntityV2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.empay.user_management.repository.LoginAttemptRepository;
import com.empay.user_management.repository.UserAuthMethodRepository;
import com.empay.user_management.repository.UserCredentialRepository;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.service.PasswordPolicyService;
import com.empay.user_management.service.UserActivityTrackingService;
import com.empay.user_management.utils.APIConstants;
import com.empay.user_management.utils.JwtUtils;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

	@Mock
	private LoginAttemptRepository loginAttemptRepo;

	@Mock
	private UsersRepositoryV2 usersRepositoryV2;

	@Mock
	private UserCredentialRepository userCredentialRepository;

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private PasswordPolicyService passwordPolicyService;

	@Mock
	private UserActivityTrackingService userActivityTrackingService;

	@Mock
	private UserAuthMethodRepository userAuthMethodRepository;

	@InjectMocks
	private LoginServiceImpl loginService;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLogout_AuthenticatedUser_Success() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("192.168.1.100");
		request.getSession(true);

		when(jwtUtils.getUserId(request)).thenReturn("admin");

		ResponseEntity<Object> response = loginService.logout(request);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());

		Map<String, Object> body = (Map<String, Object>) response.getBody();
		assertNotNull(body);
		assertEquals("Logout successful", body.get("message"));
		assertEquals("admin", body.get("userId"));

		// Verify Set-Cookie headers
		List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
		assertNotNull(setCookies);
		assertTrue(setCookies.stream().anyMatch(c -> c.contains(APIConstants.ACCESS_TOKEN_COOKIE) && c.contains("Max-Age=0")));
		assertTrue(setCookies.stream().anyMatch(c -> c.contains("AUTH_TOKEN") && c.contains("Max-Age=0")));
		assertTrue(setCookies.stream().anyMatch(c -> c.contains(APIConstants.REFRESH_TOKEN_COOKIE) && c.contains("Max-Age=0")));
		// Finding 22: Logout cookies must use SameSite=Strict
		assertTrue(setCookies.stream().allMatch(c -> c.contains("SameSite=Strict")));

		// Verify security context is cleared
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		// Verify audit logging
		verify(userActivityTrackingService).saveUserActivity(any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLogout_SecurityContextUser_Success() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("checker_user", "N/A", List.of())
		);

		MockHttpServletRequest request = new MockHttpServletRequest();
		when(jwtUtils.getUserId(request)).thenReturn(null);

		ResponseEntity<Object> response = loginService.logout(request);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());

		Map<String, Object> body = (Map<String, Object>) response.getBody();
		assertNotNull(body);
		assertEquals("Logout successful", body.get("message"));
		assertEquals("checker_user", body.get("userId"));

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(userActivityTrackingService).saveUserActivity(any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLogout_NullRequest_Success() {
		ResponseEntity<Object> response = loginService.logout(null);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());

		Map<String, Object> body = (Map<String, Object>) response.getBody();
		assertNotNull(body);
		assertEquals("Logout successful", body.get("message"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetCurrentUserSession_ActiveUser_Success() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		when(jwtUtils.getUserId(request)).thenReturn("admin");

		com.empay.user_management.entities.UsersEntityV2 user = new com.empay.user_management.entities.UsersEntityV2();
		user.setUserId("admin");
		user.setSerialNumber(1);
		user.setDisplayName("Administrator");
		user.setEmail("admin@mars3.com");
		user.setStatus("ACTIVE");

		when(usersRepositoryV2.findByUserId("admin")).thenReturn(user);

		ResponseEntity<Object> response = loginService.getCurrentUserSession(request);
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());

		Map<String, Object> body = (Map<String, Object>) response.getBody();
		assertEquals(true, body.get("authenticated"));
		assertEquals("admin", body.get("userId"));
	}

	@Test
	void testGetCurrentUserSession_Unauthenticated() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		when(jwtUtils.getUserId(request)).thenReturn(null);

		ResponseEntity<Object> response = loginService.getCurrentUserSession(request);
		assertNotNull(response);
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	void testGetCurrentUserSession_SuspendedUser_ReturnsUnauthorized() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		when(jwtUtils.getUserId(request)).thenReturn("suspended_user");

		com.empay.user_management.entities.UsersEntityV2 user = new com.empay.user_management.entities.UsersEntityV2();
		user.setUserId("suspended_user");
		user.setSerialNumber(2);
		user.setStatus("SUSPENDED");

		when(usersRepositoryV2.findByUserId("suspended_user")).thenReturn(user);

		ResponseEntity<Object> response = loginService.getCurrentUserSession(request);
		assertNotNull(response);
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword rejects null or empty input data map")
	void testChangePassword_NullOrEmptyRequest() {
		ResponseEntity<Object> nullResp = loginService.changeUserPassword(null);
		assertEquals(HttpStatus.BAD_REQUEST, nullResp.getStatusCode());

		ResponseEntity<Object> emptyResp = loginService.changeUserPassword(Map.of());
		assertEquals(HttpStatus.BAD_REQUEST, emptyResp.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword rejects when user is not found in database")
	void testChangePassword_UserNotFound() {
		when(usersRepositoryV2.findByUserId("unknown_user")).thenReturn(null);

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "unknown_user",
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!"
		));

		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals("User not found", body.get("message"));
		assertEquals(false, body.get("success"));
	}

	@Test
	@DisplayName("changeUserPassword rejects when credentials are not found")
	void testChangePassword_CredentialsNotFound() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("test_user");
		user.setSerialNumber(10);
		when(usersRepositoryV2.findByUserId("test_user")).thenReturn(user);
		when(userCredentialRepository.findByUserSerialNo(10)).thenReturn(null);

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "test_user",
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!"
		));

		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals("User credentials not found", body.get("message"));
	}

	@Test
	@DisplayName("changeUserPassword rejects when account is currently locked")
	void testChangePassword_AccountLocked() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("locked_user");
		user.setSerialNumber(20);
		when(usersRepositoryV2.findByUserId("locked_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(20);
		cred.setLockedUntil(LocalDateTime.now().plusMinutes(15));
		when(userCredentialRepository.findByUserSerialNo(20)).thenReturn(cred);

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "locked_user",
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!"
		));

		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertTrue(String.valueOf(body.get("message")).contains("Account is currently locked"));
	}

	@Test
	@DisplayName("changeUserPassword rejects when current password is incorrect")
	void testChangePassword_IncorrectCurrentPassword() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("test_user");
		user.setSerialNumber(30);
		when(usersRepositoryV2.findByUserId("test_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(30);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("CorrectCurrentPassword123!"));
		when(userCredentialRepository.findByUserSerialNo(30)).thenReturn(cred);

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "test_user",
				"currentPassword", "WrongCurrentPassword!",
				"newPassword", "NewPassword123!"
		));

		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals("Current password is incorrect", body.get("message"));
	}

	@Test
	@DisplayName("changeUserPassword enforces active password policy and returns violations on failure")
	void testChangePassword_PasswordPolicyViolation() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("policy_user");
		user.setSerialNumber(40);
		user.setEmail("policy@empay.com");
		user.setDisplayName("Policy User");
		when(usersRepositoryV2.findByUserId("policy_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(40);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("ValidCurrentPassword123!"));
		when(userCredentialRepository.findByUserSerialNo(40)).thenReturn(cred);

		when(passwordPolicyService.validatePassword(any(), any(UserContext.class)))
				.thenReturn(PasswordValidationResult.failure(List.of(
						"Password length must be at least 15 characters",
						"Password must contain at least one special character"
				)));

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "policy_user",
				"currentPassword", "ValidCurrentPassword123!",
				"newPassword", "weak"
		));

		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals(false, body.get("success"));
		assertTrue(String.valueOf(body.get("message")).contains("Password does not meet active policy requirements"));
		assertTrue(String.valueOf(body.get("message")).contains("at least 15 characters"));
		assertNotNull(body.get("violations"));
	}

	@Test
	@DisplayName("changeUserPassword rejects when new password is same as current password")
	void testChangePassword_SameAsCurrentPassword() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("test_user");
		user.setSerialNumber(50);
		when(usersRepositoryV2.findByUserId("test_user")).thenReturn(user);

		String currentPass = "ComplexCurrentPassword123!";
		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(50);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode(currentPass));
		when(userCredentialRepository.findByUserSerialNo(50)).thenReturn(cred);

		when(passwordPolicyService.validatePassword(any(), any(UserContext.class)))
				.thenReturn(PasswordValidationResult.success());

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "test_user",
				"currentPassword", currentPass,
				"newPassword", currentPass
		));

		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals("New password cannot be the same as the current password", body.get("message"));
	}

	@Test
	@DisplayName("changeUserPassword rejects when confirmPassword does not match newPassword")
	void testChangePassword_MismatchedConfirmPassword() {
		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "test_user",
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!",
				"confirmPassword", "DifferentPass123!"
		));

		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals("New password and confirmation password do not match", body.get("message"));
	}

	@Test
	@DisplayName("changeUserPassword successfully validates against active policy and updates password hash")
	void testChangePassword_Success() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("test_user");
		user.setSerialNumber(60);
		user.setEmail("user@empay.com");
		user.setDisplayName("Test User");
		when(usersRepositoryV2.findByUserId("test_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(60);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("CurrentValidPass123!"));
		when(userCredentialRepository.findByUserSerialNo(60)).thenReturn(cred);

		when(passwordPolicyService.validatePassword(any(), any(UserContext.class)))
				.thenReturn(PasswordValidationResult.success());

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "test_user",
				"currentPassword", "CurrentValidPass123!",
				"newPassword", "BrandNewSecurePassword123!"
		));

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals(true, body.get("success"));
		assertEquals("Password changed successfully", body.get("message"));
		verify(userCredentialRepository).save(cred);
	}

	@Test
	@DisplayName("changeUserPassword successfully decodes hex-encoded inputData and updates password")
	void testChangePassword_HexInputData_Success() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("hex_user");
		user.setSerialNumber(70);
		when(usersRepositoryV2.findByUserId("hex_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(70);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("OldHexPass123!"));
		when(userCredentialRepository.findByUserSerialNo(70)).thenReturn(cred);

		when(passwordPolicyService.validatePassword(any(), any(UserContext.class)))
				.thenReturn(PasswordValidationResult.success());

		// CSV: newPassword,userId,currentPassword,confirmPassword
		String rawCsv = "NewSecureHexPass123!,hex_user,OldHexPass123!,NewSecureHexPass123!";
		String hexData = Hex.encodeHexString(rawCsv.getBytes(StandardCharsets.UTF_8));

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of("inputData", hexData));

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals(true, body.get("success"));
		assertEquals("Password changed successfully", body.get("message"));
	}

	@Test
	@DisplayName("loginCheck with missing user executes dummy BCrypt hash and returns unauthorized (Finding 20)")
	void testLoginCheck_MissingUser_TimingEqualization() {
		when(usersRepositoryV2.findByUserId("nonexistent")).thenReturn(null);

		MockHttpServletRequest request = new MockHttpServletRequest();
		String rawCsv = "nonexistent,SecretPass123!";
		String hexData = Hex.encodeHexString(rawCsv.getBytes(StandardCharsets.UTF_8));

		ResponseEntity<Object> resp = loginService.validateUserLogin(Map.of("inputData", hexData), request);

		assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals("Invalid user ID or password", body.get("message"));
	}

	@Test
	@DisplayName("loginCheck with user having LOCAL_PASSWORD disabled returns unauthorized (Finding S2)")
	void testLoginCheck_DisabledLocalPassword_Unauthorized() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("sso_only_user");
		user.setSerialNumber(88);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("sso_only_user")).thenReturn(user);

		UserAuthMethodEntity disabledLocal = UserAuthMethodEntity.builder()
				.userSerialNo(88)
				.provider("LOCAL_PASSWORD")
				.isEnabled('N')
				.build();
		when(userAuthMethodRepository.findByUserSerialNo(88)).thenReturn(List.of(disabledLocal));

		MockHttpServletRequest request = new MockHttpServletRequest();
		String rawCsv = "sso_only_user,SecretPass123!";
		String hexData = Hex.encodeHexString(rawCsv.getBytes(StandardCharsets.UTF_8));

		ResponseEntity<Object> resp = loginService.validateUserLogin(Map.of("inputData", hexData), request);

		assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals("Invalid user ID or password", body.get("message"));
	}

	@Test
	@DisplayName("getLoginAttempts uses findTop100ByOrderByAttemptedAtDesc pagination (Finding 19)")
	void testGetLoginAttempts_UsesPagination() {
		LoginAttemptEntity attempt = new LoginAttemptEntity();
		attempt.setUserId("testuser");
		when(loginAttemptRepo.findTop100ByOrderByAttemptedAtDesc()).thenReturn(List.of(attempt));

		List<?> resp = loginService.getLoginAttempts();

		assertNotNull(resp);
		assertEquals(1, resp.size());
		verify(loginAttemptRepo).findTop100ByOrderByAttemptedAtDesc();
		verify(loginAttemptRepo, never()).findAll();
	}

	@Test
	@DisplayName("loginCheck handles passwords containing comma without truncation (Finding 14)")
	void testLoginCheck_PasswordWithComma_Success() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("comma_user");
		user.setSerialNumber(99);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("comma_user")).thenReturn(user);

		String passwordWithComma = "Pa55,w0rd!Complex,Special";
		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(99);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode(passwordWithComma));
		when(userCredentialRepository.findByUserSerialNo(99)).thenReturn(cred);
		when(userAuthMethodRepository.findByUserSerialNo(99)).thenReturn(List.of());
		when(jwtUtils.generateToken(any(), any(), any())).thenReturn("mocked.jwt.token");

		MockHttpServletRequest request = new MockHttpServletRequest();
		String rawCsv = "comma_user," + passwordWithComma;
		String hexData = Hex.encodeHexString(rawCsv.getBytes(StandardCharsets.UTF_8));

		ResponseEntity<Object> resp = loginService.validateUserLogin(Map.of("inputData", hexData), request);

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) resp.getBody();
		assertNotNull(body);
		assertEquals("Login successful", body.get("message"));
		assertEquals("comma_user", body.get("userId"));
		assertFalse(body.containsKey("token"), "Session token must not be in login response body (Finding S5)");
		assertFalse(body.containsKey("accessToken"), "Session accessToken must not be in login response body (Finding S5)");
		assertFalse(body.containsKey("authToken"), "Session authToken must not be in login response body (Finding S5)");
		assertNull(resp.getHeaders().getFirst(HttpHeaders.AUTHORIZATION), "Authorization header must not be set on login response (Finding S5)");
		assertNotNull(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE), "Session must be set via HttpOnly cookie");
	}

	@Test
	@DisplayName("getCurrentUserSession does not echo token into response body (Finding S5 / Finding 22)")
	void testGetCurrentUserSession_TokenNotExposedInResponseBody() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		when(jwtUtils.getUserId(request)).thenReturn("admin");

		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("admin");
		user.setSerialNumber(1);
		user.setDisplayName("Administrator");
		user.setEmail("admin@mars3.com");
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("admin")).thenReturn(user);

		ResponseEntity<Object> response = loginService.getCurrentUserSession(request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertFalse(body.containsKey("token"), "Session token must not be echoed in response body");
		assertFalse(body.containsKey("accessToken"), "Session accessToken must not be echoed in response body");
	}

	@Test
	@DisplayName("validateUserLogin returns 401 when local password auth is disabled for user")
	void testValidateUserLogin_PasswordAuthDisabled() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("sso_only_user");
		user.setSerialNumber(55);
		user.setStatus("ACTIVE");

		when(usersRepositoryV2.findByUserId("sso_only_user")).thenReturn(user);

		UserAuthMethodEntity disabledPassword = UserAuthMethodEntity.builder()
				.userSerialNo(55)
				.provider("LOCAL_PASSWORD")
				.isEnabled('N')
				.build();
		when(userAuthMethodRepository.findByUserSerialNo(55)).thenReturn(List.of(disabledPassword));

		MockHttpServletRequest request = new MockHttpServletRequest();
		ResponseEntity<Object> response = loginService.validateUserLogin(
				Map.of("userId", "sso_only_user", "password", "any_pass"), request);

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertEquals("Invalid user ID or password", body.get("message"));
	}

	@Test
	@DisplayName("validateUserLogin parses hex-encoded JSON inputData payload")
	void testValidateUserLogin_HexJsonPayload() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("json_user");
		user.setSerialNumber(66);
		user.setStatus("ACTIVE");

		when(usersRepositoryV2.findByUserId("json_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(66);
		cred.setPasswordHash(new BCryptPasswordEncoder().encode("Secret123!"));
		when(userCredentialRepository.findByUserSerialNo(66)).thenReturn(cred);
		when(userAuthMethodRepository.findByUserSerialNo(66)).thenReturn(List.of());
		when(jwtUtils.generateToken("json_user", 66, null)).thenReturn("mock_token");

		String json = "{\"userId\":\"json_user\",\"password\":\"Secret123!\"}";
		String hexJson = Hex.encodeHexString(json.getBytes(StandardCharsets.UTF_8));

		MockHttpServletRequest request = new MockHttpServletRequest();
		ResponseEntity<Object> response = loginService.validateUserLogin(
				Map.of("inputData", hexJson), request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin rejects password authentication when user authMode is SSO (Finding S2)")
	void testValidateUserLogin_AuthModeSSO_PasswordAuthDisabled() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("sso_user");
		user.setSerialNumber(77);
		user.setStatus("ACTIVE");
		user.setAuthMode("SSO"); // Finding S2

		when(usersRepositoryV2.findByUserId("sso_user")).thenReturn(user);

		String rawCsv = "sso_user,SomePassword123!";
		String hexData = Hex.encodeHexString(rawCsv.getBytes(StandardCharsets.UTF_8));

		ResponseEntity<Object> response = loginService.validateUserLogin(
				Map.of("inputData", hexData), new MockHttpServletRequest());

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin performs simulateHashWork on locked account path (Finding 20)")
	void testValidateUserLogin_AccountLocked_SimulatesHashWork() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("locked_timing_user");
		user.setSerialNumber(88);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("locked_timing_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(88);
		cred.setPasswordHash("hashed");
		cred.setLockedUntil(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(30));
		when(userCredentialRepository.findByUserSerialNo(88)).thenReturn(cred);

		String rawCsv = "locked_timing_user,AttemptedPass";
		String hexData = Hex.encodeHexString(rawCsv.getBytes(StandardCharsets.UTF_8));

		ResponseEntity<Object> response = loginService.validateUserLogin(
				Map.of("inputData", hexData), new MockHttpServletRequest());

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertEquals("Invalid user ID or password", body.get("message"));
		verify(loginAttemptRepo).save(argThat(attempt -> "ACCOUNT_LOCKED".equals(attempt.getFailureReason())));
	}

	@Test
	@DisplayName("changeUserPassword supports direct JSON body fields with comma in password (Finding 14)")
	void testChangeUserPassword_DirectJsonBodyWithComma() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("change_user");
		user.setSerialNumber(99);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("change_user")).thenReturn(user);

		String currentPass = "Old,Pass!123";
		String newPass = "New,Complex,Pass!456";

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(99);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode(currentPass));
		when(userCredentialRepository.findByUserSerialNo(99)).thenReturn(cred);
		when(passwordPolicyService.validatePassword(eq(newPass), any(com.empay.user_management.dto.UserContext.class)))
				.thenReturn(new com.empay.user_management.dto.PasswordValidationResult(true, List.of()));

		Map<String, String> body = Map.of(
				"userId", "change_user",
				"currentPassword", currentPass,
				"newPassword", newPass,
				"confirmPassword", newPass
		);

		ResponseEntity<Object> response = loginService.changeUserPassword(body);
		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns bad request when inputDataMap is null")
	void testValidateUserLogin_NullMap_ReturnsBadRequest() {
		ResponseEntity<Object> resp = loginService.validateUserLogin(null, new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns bad request when map is empty")
	void testValidateUserLogin_EmptyMap_ReturnsBadRequest() {
		ResponseEntity<Object> resp = loginService.validateUserLogin(Map.of(), new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin accepts unencrypted username and password map directly")
	void testValidateUserLogin_DirectUsernameAndPassword_Success() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("direct_user");
		user.setSerialNumber(101);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("direct_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(101);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("ValidPass123!"));
		when(userCredentialRepository.findByUserSerialNo(101)).thenReturn(cred);
		when(userAuthMethodRepository.findByUserSerialNo(101)).thenReturn(List.of());
		when(jwtUtils.generateToken("direct_user", 101, null)).thenReturn("direct_token");

		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("username", "direct_user", "password", "ValidPass123!"), new MockHttpServletRequest());
		assertEquals(HttpStatus.OK, resp.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword returns bad request when body is null or empty")
	void testChangeUserPassword_NullAndEmpty_ReturnsBadRequest() {
		ResponseEntity<Object> r1 = loginService.changeUserPassword(null);
		assertEquals(HttpStatus.BAD_REQUEST, r1.getStatusCode());

		ResponseEntity<Object> r2 = loginService.changeUserPassword(Map.of());
		assertEquals(HttpStatus.BAD_REQUEST, r2.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword returns bad request when new and confirm passwords do not match")
	void testChangeUserPassword_PasswordsDoNotMatch_ReturnsBadRequest() {
		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "testuser",
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!",
				"confirmPassword", "DifferentPass123!"
		));
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin accepts direct userId and password containing commas (Finding 14)")
	void testValidateUserLogin_DirectUserId_WithCommasInPassword_Success() {
		String passwordWithCommas = "pass,word,with,commas,123!";
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("comma_user");
		user.setSerialNumber(102);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("comma_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(102);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode(passwordWithCommas));
		when(userCredentialRepository.findByUserSerialNo(102)).thenReturn(cred);
		when(userAuthMethodRepository.findByUserSerialNo(102)).thenReturn(List.of());
		when(jwtUtils.generateToken("comma_user", 102, null)).thenReturn("mock_token_commas");

		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("userId", "comma_user", "password", passwordWithCommas), new MockHttpServletRequest());
		assertEquals(HttpStatus.OK, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns bad request when user is provided but password and inputData are missing")
	void testValidateUserLogin_MissingPasswordAndInputData_ReturnsBadRequest() {
		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("userId", "some_user"), new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns bad request when inputData is invalid hex")
	void testValidateUserLogin_InvalidHexInputData_ReturnsBadRequest() {
		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("inputData", "zzNotHexData123"), new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin parses hex-encoded JSON inputData successfully")
	void testValidateUserLogin_HexJsonInputData_Success() {
		String json = "{\"userId\":\"hex_user\",\"password\":\"ValidPass123!\"}";
		String hex = Hex.encodeHexString(json.getBytes(StandardCharsets.UTF_8));

		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("hex_user");
		user.setSerialNumber(103);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("hex_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(103);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("ValidPass123!"));
		when(userCredentialRepository.findByUserSerialNo(103)).thenReturn(cred);
		when(userAuthMethodRepository.findByUserSerialNo(103)).thenReturn(List.of());
		when(jwtUtils.generateToken("hex_user", 103, null)).thenReturn("hex_jwt_token");

		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("inputData", hex), new MockHttpServletRequest());
		assertEquals(HttpStatus.OK, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns unauthorized when user status is inactive")
	void testValidateUserLogin_UserInactive_ReturnsUnauthorized() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("inactive_user");
		user.setSerialNumber(104);
		user.setStatus("INACTIVE");
		when(usersRepositoryV2.findByUserId("inactive_user")).thenReturn(user);

		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("userId", "inactive_user", "password", "ValidPass123!"), new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns unauthorized when user authMode is SSO")
	void testValidateUserLogin_UserAuthModeSSO_ReturnsUnauthorized() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("sso_user");
		user.setSerialNumber(105);
		user.setStatus("ACTIVE");
		user.setAuthMode("SSO");
		when(usersRepositoryV2.findByUserId("sso_user")).thenReturn(user);

		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("userId", "sso_user", "password", "ValidPass123!"), new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns unauthorized when credentials are missing")
	void testValidateUserLogin_UserCredentialMissing_ReturnsUnauthorized() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("no_cred_user");
		user.setSerialNumber(106);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("no_cred_user")).thenReturn(user);
		when(userCredentialRepository.findByUserSerialNo(106)).thenReturn(null);

		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("userId", "no_cred_user", "password", "ValidPass123!"), new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns unauthorized when account is locked")
	void testValidateUserLogin_AccountLocked_ReturnsUnauthorized() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("locked_user");
		user.setSerialNumber(107);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("locked_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(107);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("ValidPass123!"));
		cred.setLockedUntil(LocalDateTime.now().plusMinutes(15));
		when(userCredentialRepository.findByUserSerialNo(107)).thenReturn(cred);

		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("userId", "locked_user", "password", "ValidPass123!"), new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
	}

	@Test
	@DisplayName("validateUserLogin returns unauthorized when password does not match")
	void testValidateUserLogin_PasswordIncorrect_ReturnsUnauthorized() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("wrong_pwd_user");
		user.setSerialNumber(108);
		user.setStatus("ACTIVE");
		when(usersRepositoryV2.findByUserId("wrong_pwd_user")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(108);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("CorrectPass123!"));
		cred.setFailedAttempts(0);
		when(userCredentialRepository.findByUserSerialNo(108)).thenReturn(cred);

		ResponseEntity<Object> resp = loginService.validateUserLogin(
				Map.of("userId", "wrong_pwd_user", "password", "WrongPass123!"), new MockHttpServletRequest());
		assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
		verify(userCredentialRepository).save(cred);
	}

	@Test
	@DisplayName("changeUserPassword returns bad request when required fields are missing")
	void testChangeUserPassword_MissingRequiredFields_ReturnsBadRequest() {
		// Missing userId
		ResponseEntity<Object> r1 = loginService.changeUserPassword(Map.of(
				"currentPassword", "OldPass123!", "newPassword", "NewPass123!"));
		assertEquals(HttpStatus.BAD_REQUEST, r1.getStatusCode());

		// Missing currentPassword
		ResponseEntity<Object> r2 = loginService.changeUserPassword(Map.of(
				"userId", "user1", "newPassword", "NewPass123!"));
		assertEquals(HttpStatus.BAD_REQUEST, r2.getStatusCode());

		// Missing newPassword
		ResponseEntity<Object> r3 = loginService.changeUserPassword(Map.of(
				"userId", "user1", "currentPassword", "OldPass123!"));
		assertEquals(HttpStatus.BAD_REQUEST, r3.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword returns bad request when user is not found")
	void testChangeUserPassword_UserNotFound_ReturnsBadRequest() {
		when(usersRepositoryV2.findByUserId("missing_user")).thenReturn(null);
		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "missing_user",
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!",
				"confirmPassword", "NewPass123!"
		));
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword returns bad request when user credential not found")
	void testChangeUserPassword_CredentialNotFound_ReturnsBadRequest() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("user_no_cred");
		user.setSerialNumber(201);
		when(usersRepositoryV2.findByUserId("user_no_cred")).thenReturn(user);
		when(userCredentialRepository.findByUserSerialNo(201)).thenReturn(null);

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "user_no_cred",
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!",
				"confirmPassword", "NewPass123!"
		));
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword returns bad request when account is locked")
	void testChangeUserPassword_AccountLocked_ReturnsBadRequest() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("user_locked");
		user.setSerialNumber(202);
		when(usersRepositoryV2.findByUserId("user_locked")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(202);
		cred.setLockedUntil(LocalDateTime.now().plusMinutes(10));
		when(userCredentialRepository.findByUserSerialNo(202)).thenReturn(cred);

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "user_locked",
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!",
				"confirmPassword", "NewPass123!"
		));
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword returns bad request when current password is incorrect")
	void testChangeUserPassword_CurrentPasswordIncorrect_ReturnsBadRequest() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("user_wrong_old");
		user.setSerialNumber(203);
		when(usersRepositoryV2.findByUserId("user_wrong_old")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(203);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("RealOldPass123!"));
		cred.setFailedAttempts(0);
		when(userCredentialRepository.findByUserSerialNo(203)).thenReturn(cred);

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "user_wrong_old",
				"currentPassword", "WrongOldPass123!",
				"newPassword", "NewPass123!",
				"confirmPassword", "NewPass123!"
		));
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword successfully updates credentials and clears failed attempts")
	void testChangeUserPassword_Success_UpdatesCredential() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("user_success");
		user.setSerialNumber(204);
		user.setEmail("user_success@test.com");
		when(usersRepositoryV2.findByUserId("user_success")).thenReturn(user);

		UserCredentialEntity cred = new UserCredentialEntity();
		cred.setUserSerialNo(204);
		cred.setPasswordHash(new BCryptPasswordEncoder(12).encode("OldPass123!"));
		cred.setFailedAttempts(2);
		when(userCredentialRepository.findByUserSerialNo(204)).thenReturn(cred);
		when(passwordPolicyService.validatePassword(eq("BrandNewPass123!"), any()))
				.thenReturn(PasswordValidationResult.success());

		ResponseEntity<Object> resp = loginService.changeUserPassword(Map.of(
				"userId", "user_success",
				"currentPassword", "OldPass123!",
				"newPassword", "BrandNewPass123!",
				"confirmPassword", "BrandNewPass123!"
		));
		assertEquals(HttpStatus.OK, resp.getStatusCode());
		verify(userCredentialRepository).save(cred);
		assertEquals(0, cred.getFailedAttempts());
	}

	@Test
	@DisplayName("validateUserLogin returns bad request when userId or password exceeds maximum allowed bounds (DoS mitigation)")
	void testValidateUserLogin_InputLengthExceeded_ReturnsBadRequest() {
		String longUserId = "u".repeat(101);
		ResponseEntity<Object> r1 = loginService.validateUserLogin(
				Map.of("userId", longUserId, "password", "ValidPass123!"), new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, r1.getStatusCode());

		String longPassword = "p".repeat(257);
		ResponseEntity<Object> r2 = loginService.validateUserLogin(
				Map.of("userId", "valid_user", "password", longPassword), new MockHttpServletRequest());
		assertEquals(HttpStatus.BAD_REQUEST, r2.getStatusCode());
	}

	@Test
	@DisplayName("changeUserPassword returns bad request when userId or password exceeds maximum allowed bounds")
	void testChangeUserPassword_InputLengthExceeded_ReturnsBadRequest() {
		String longUserId = "u".repeat(101);
		ResponseEntity<Object> r1 = loginService.changeUserPassword(Map.of(
				"userId", longUserId,
				"currentPassword", "OldPass123!",
				"newPassword", "NewPass123!",
				"confirmPassword", "NewPass123!"
		));
		assertEquals(HttpStatus.BAD_REQUEST, r1.getStatusCode());

		String longPassword = "p".repeat(257);
		ResponseEntity<Object> r2 = loginService.changeUserPassword(Map.of(
				"userId", "valid_user",
				"currentPassword", longPassword,
				"newPassword", "NewPass123!",
				"confirmPassword", "NewPass123!"
		));
		assertEquals(HttpStatus.BAD_REQUEST, r2.getStatusCode());
	}
}
