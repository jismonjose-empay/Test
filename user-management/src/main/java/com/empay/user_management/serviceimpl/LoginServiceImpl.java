package com.empay.user_management.serviceimpl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.empay.user_management.entities.UserRoleEntityV2;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import com.empay.user_management.utils.APIConstants;
import com.empay.user_management.utils.JwtUtils;
import java.time.Duration;
import java.util.HashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.empay.user_management.dto.ChangePasswordResponse;
import com.empay.user_management.dto.LoginAttemptResponse;
import com.empay.user_management.dto.LoginCheckResult;
import com.empay.user_management.entities.LoginAttemptEntity;
import com.empay.user_management.entities.UserCredentialEntity;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.repository.LoginAttemptRepository;
import com.empay.user_management.repository.UserCredentialRepository;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.repository.UserAuthMethodRepository;
import org.springframework.beans.factory.annotation.Value;
import com.empay.user_management.service.LoginService;
import com.empay.user_management.service.PasswordPolicyService;
import com.empay.user_management.dto.PasswordValidationResult;
import com.empay.user_management.dto.UserContext;
import com.empay.user_management.service.UserActivityTrackingService;
import com.empay.user_management.valueobjects.UserActivityTrackingVO;
import java.util.LinkedHashMap;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

	private final LoginAttemptRepository loginAttemptRepo;

	private final UsersRepositoryV2 usersRepositoryV2;

	private final UserCredentialRepository userCredentialRepository;

	private final com.empay.user_management.utils.JwtUtils jwtUtils;

	private final PasswordPolicyService passwordPolicyService;

	private final UserActivityTrackingService userActivityTrackingService;

	private final UserAuthMethodRepository userAuthMethodRepository;

	@Value("${server.servlet.session.cookie.secure:true}")
	private boolean cookieSecure = true;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

	private final String dummyPasswordHash = passwordEncoder.encode("timing-mitigation-dummy-salt-2026");

	private static final int MAX_FAILED_ATTEMPTS = 5;
	private static final long LOCKOUT_MINUTES = 15;

	private static final String UNKNOWN = "UNKNOWN";
	private static final String MESSAGE = "message";
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");
	private static final String SUCCESS = "success";
	private static final String USER_ID = "userId";
	private static final String USERNAME = "username";
	private static final String ANONYMOUS_USER = "anonymousUser";
	private static final String AUTHENTICATED = "authenticated";
	private static final String SAME_SITE_STRICT = "Strict";
	private static final String NEW_PASSWORD = "newPassword";
	private static final String CONFIRM_PASSWORD = "confirmPassword";

	@Transactional
	public ResponseEntity<Object> validateUserLogin(Map<String, String> inputDataMap, HttpServletRequest request) {

		if (inputDataMap == null) {
			insertIntoLoginAttempts(UNKNOWN, null, 'N', "INVALID_INPUT", request);
			return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Username and password are required"));
		}

		String[] loginDetails = null;
		String directUser = inputDataMap.get(inputDataMap.containsKey(USER_ID) ? USER_ID : USERNAME);
		String directPass = inputDataMap.get("password");

		if (directUser != null && !directUser.isBlank() && directPass != null && !directPass.isBlank()) {
			loginDetails = new String[]{ directUser.trim(), directPass };
		} else {
			String inputData = inputDataMap.get("inputData");
			if (inputData == null || inputData.isBlank()) {
				insertIntoLoginAttempts(UNKNOWN, null, 'N', "INVALID_INPUT", request);
				return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Username and password are required"));
			}
			loginDetails = decryptInput(inputData);
		}

		if (loginDetails.length == 0 || loginDetails[0].length() > 100 || loginDetails[1].length() > 256) {
			insertIntoLoginAttempts(UNKNOWN, null, 'N', "INVALID_INPUT_DATA", request);
			return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Invalid login data"));
		}

		String userId = loginDetails[0];

		LoginCheckResult result = loginCheck(loginDetails, request);

		insertIntoLoginAttempts(userId, result.userSerialNumber(), result.authenticated() ? 'Y' : 'N',
				result.failureReason(), request);

		if (!result.authenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(MESSAGE, "Invalid user ID or password"));
		}

		String token = jwtUtils.generateToken(userId, result.userSerialNumber(), result.userRole());
		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put(MESSAGE, "Login successful");
		responseBody.put(USER_ID, userId);
		// Finding S5: Do NOT echo session token into response body. The HttpOnly cookie is the session.
		if (result.userRole() != null) {
			responseBody.put("userRole", result.userRole());
			responseBody.put("role", result.userRole());
		}

		ResponseCookie cookie = ResponseCookie.from(APIConstants.ACCESS_TOKEN_COOKIE, token != null ? token : "")
				.httpOnly(true)
				.secure(cookieSecure)
				.path("/")
				.maxAge(Duration.ofMinutes(APIConstants.ACCESS_TOKEN_EXPIRY_IN_MINUTES))
				.sameSite(SAME_SITE_STRICT)
				.build();

		ResponseCookie authCookie = ResponseCookie.from("AUTH_TOKEN", token != null ? token : "")
				.httpOnly(true)
				.secure(cookieSecure)
				.path("/")
				.maxAge(Duration.ofMinutes(APIConstants.ACCESS_TOKEN_EXPIRY_IN_MINUTES))
				.sameSite(SAME_SITE_STRICT)
				.build();

		return ResponseEntity.ok()
				// Finding S5: Do NOT set Authorization header in response. The HttpOnly cookie is the session.
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.header(HttpHeaders.SET_COOKIE, authCookie.toString())
				.body(responseBody);
	}

	private String[] decryptInput(String inputLoginData) {
		try {
			String dataString = new String(Hex.decodeHex(inputLoginData), StandardCharsets.UTF_8);

			// Support JSON payload in hex
			if (dataString.trim().startsWith("{") && dataString.trim().endsWith("}")) {
				String trimmed = dataString.trim();
				String user = extractJsonField(trimmed, USER_ID);
				if (user == null) user = extractJsonField(trimmed, USERNAME);
				String pass = extractJsonField(trimmed, "password");
				if (user != null && !user.isBlank() && pass != null && !pass.isBlank()) {
					return new String[]{ user, pass };
				}
			}

			// Split on first comma only so commas in password are preserved (Finding 14)
			String[] loginDetails = dataString.split(",", 2);

			if (loginDetails.length < 2 || loginDetails[0].isBlank() || loginDetails[1].isBlank()) {
				return new String[0];
			}

			return loginDetails;

		} catch (DecoderException e) {
			return new String[0];
		}
	}

	private String extractJsonField(String json, String field) {
		String key = "\"" + field + "\"";
		int idx = json.indexOf(key);
		if (idx == -1) return null;
		int colon = json.indexOf(":", idx + key.length());
		if (colon == -1) return null;
		int firstQuote = json.indexOf("\"", colon + 1);
		if (firstQuote == -1) return null;
		int secondQuote = json.indexOf("\"", firstQuote + 1);
		if (secondQuote == -1) return null;
		return json.substring(firstQuote + 1, secondQuote);
	}

	private LoginCheckResult loginCheck(String[] loginDetails, HttpServletRequest request) {
		String userId = loginDetails[0];
		String password = loginDetails[1];

		UsersEntityV2 userEntity = usersRepositoryV2.findByUserId(userId);
		if (userEntity == null) {
			simulateHashWork(password);
			return new LoginCheckResult(false, null, "USER_NOT_FOUND");
		}

		Integer serialNumber = userEntity.getSerialNumber();
		String eligibilityError = checkUserEligibility(userEntity, serialNumber);
		if (eligibilityError != null) {
			simulateHashWork(password);
			return new LoginCheckResult(false, serialNumber, eligibilityError);
		}

		UserCredentialEntity credential = userCredentialRepository.findByUserSerialNo(serialNumber);
		if (credential == null) {
			simulateHashWork(password);
			return new LoginCheckResult(false, serialNumber, "CREDENTIAL_NOT_FOUND");
		}

		LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
		if (isAccountLocked(credential, now)) {
			// Finding 20: Simulate hash work on locked path to eliminate timing side-channel
			simulateHashWork(password);
			return new LoginCheckResult(false, serialNumber, "ACCOUNT_LOCKED");
		}

		if (!validateLogin(credential, password)) {
			registerFailedLogin(credential, now);
			return new LoginCheckResult(false, serialNumber, "INVALID_PASSWORD");
		}

		recordSuccessfulLogin(credential, now, request);
		String userRole = resolveUserRole(userEntity);
		return new LoginCheckResult(true, serialNumber, null, userRole);
	}

	private void simulateHashWork(String password) {
		passwordEncoder.matches(password != null ? password : "", dummyPasswordHash);
	}

	private boolean isPasswordDisabledByAuthMode(String authMode) {
		if (authMode == null || authMode.isBlank()) {
			return false;
		}
		String cleanMode = authMode.trim().toUpperCase(Locale.ROOT);
		return "SSO".equals(cleanMode) || "OAUTH".equals(cleanMode) || "EXTERNAL".equals(cleanMode) || "SAML".equals(cleanMode);
	}

	private boolean isPasswordDisabledByAuthMethods(Integer serialNumber) {
		if (userAuthMethodRepository == null) {
			return false;
		}
		var authMethods = userAuthMethodRepository.findByUserSerialNo(serialNumber);
		if (authMethods == null || authMethods.isEmpty()) {
			return false;
		}
		return authMethods.stream()
				.noneMatch(m -> "LOCAL_PASSWORD".equalsIgnoreCase(m.getProvider())
						&& Character.toUpperCase(m.getIsEnabled() != null ? m.getIsEnabled() : 'N') == 'Y');
	}

	private String checkUserEligibility(UsersEntityV2 userEntity, Integer serialNumber) {
		if (!"ACTIVE".equalsIgnoreCase(userEntity.getStatus())) {
			return "USER_NOT_ACTIVE";
		}
		if (isPasswordDisabledByAuthMode(userEntity.getAuthMode()) || isPasswordDisabledByAuthMethods(serialNumber)) {
			return "PASSWORD_AUTH_DISABLED";
		}
		return null;
	}

	private boolean isAccountLocked(UserCredentialEntity credential, LocalDateTime now) {
		if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(now)) {
			return true;
		}
		if (credential.getLockedUntil() != null && !credential.getLockedUntil().isAfter(now)) {
			credential.setLockedUntil(null);
		}
		return false;
	}

	private void recordSuccessfulLogin(UserCredentialEntity credential, LocalDateTime now, HttpServletRequest request) {
		credential.setPreviousSuccessAt(credential.getLastSuccessAt());
		credential.setPreviousSuccessIp(credential.getLastSuccessIp());
		credential.setFailedAttempts(0);
		credential.setFirstFailedAt(null);
		credential.setLockedUntil(null);
		credential.setLastSuccessAt(now);
		if (request != null) {
			credential.setLastSuccessIp(request.getRemoteAddr());
		}
		userCredentialRepository.save(credential);
	}

	private String resolveUserRole(UsersEntityV2 userEntity) {
		if (userEntity == null) {
			return null;
		}
		List<UserRoleEntityV2> roles = userEntity.getUserRoles();
		if (roles != null && !roles.isEmpty()) {
			return roles.stream()
					.map(UserRoleEntityV2::getRoleCode)
					.filter(Objects::nonNull)
					.filter(r -> !r.isBlank())
					.distinct()
					.collect(Collectors.joining(","));
		}
		return null;
	}

	private boolean validateLogin(UserCredentialEntity userCredential, String password) {
		String storedHash = userCredential.getPasswordHash();
		return storedHash != null && passwordEncoder.matches(password, storedHash);
	}

	private void registerFailedLogin(UserCredentialEntity credential, LocalDateTime now) {

		int failedAttempts = credential.getFailedAttempts();

		credential.setFailedAttempts(failedAttempts + 1);

		if (credential.getFirstFailedAt() == null) {
			credential.setFirstFailedAt(now);
		}

		if (failedAttempts + 1 >= MAX_FAILED_ATTEMPTS) {

			credential.setLockedUntil(now.plusMinutes(LOCKOUT_MINUTES));

			credential.setLockoutCount(credential.getLockoutCount() + 1);
		}

		userCredentialRepository.save(credential);
	}

	private void insertIntoLoginAttempts(String userId, Integer userSerialNumber, Character succeeded,
			String failureReason, HttpServletRequest request) {

		LoginAttemptEntity attempt = new LoginAttemptEntity();

		attempt.setUserId(userId);
		attempt.setUserSerialNumber(userSerialNumber);
		attempt.setSucceeded(succeeded);
		attempt.setFailureReason(failureReason);

		if (request != null) {
			attempt.setSourceIp(request.getRemoteAddr());
			attempt.setUserAgent(request.getHeader("User-Agent"));

			HttpSession session = request.getSession(false);

			if (session != null) {
				attempt.setSessionId(session.getId());
			}
		}

		loginAttemptRepo.save(attempt);
	}

	@Override
	public List<LoginAttemptResponse> getLoginAttempts() {
		return mapToLoginAttemptResponse(loginAttemptRepo.findTop100ByOrderByAttemptedAtDesc());
	}

	private List<LoginAttemptResponse> mapToLoginAttemptResponse(List<LoginAttemptEntity> loginAttemptList) {

		return loginAttemptList.stream().map(attempt -> {
			LoginAttemptResponse response = new LoginAttemptResponse();

			response.setId(String.valueOf(attempt.getAttemptId()));
			response.setReason(attempt.getFailureReason());

			response.setResult(switch (attempt.getSucceeded() == null ? ' ' : attempt.getSucceeded()) {
			case 'N' -> "FAIL";
			case 'Y' -> "OK";
			default -> null;
			});

			response.setSourceIp(attempt.getSourceIp());
			response.setUser(attempt.getUserId());

			if (attempt.getAttemptedAt() != null) {
				response.setWhen(attempt.getAttemptedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
			}

			return response;
		}).toList();
	}

	private record PasswordChangePayload(String newPassword, String resolvedUserId, String currentPassword, String confirmPassword) {}

	private PasswordChangePayload resolveEncryptedPayload(String inputData) {
		if (inputData == null || inputData.isBlank()) {
			return null;
		}
		String[] loginDetails = decryptPwdData(inputData);
		if (loginDetails.length < 3) {
			return null;
		}
		String newPassword = loginDetails[0];
		String resolvedUserId = loginDetails[1];
		String currentPassword = loginDetails[2];
		String confirmPassword = loginDetails.length >= 4 ? loginDetails[3] : null;
		return new PasswordChangePayload(newPassword, resolvedUserId, currentPassword, confirmPassword);
	}

	private String resolveCurrentPassword(Map<String, String> inputDataMap, String fallback) {
		if (fallback != null && !fallback.isBlank()) {
			return fallback;
		}
		String current = inputDataMap.get("currentPassword");
		return current != null ? current : inputDataMap.get("oldPassword");
	}

	private String resolveSecurityContextUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return null;
		}
		String authName = auth.getName();
		if (authName != null && !authName.isBlank() && !ANONYMOUS_USER.equalsIgnoreCase(authName)) {
			return authName;
		}
		return null;
	}

	private PasswordChangePayload resolvePasswordChangePayload(Map<String, String> inputDataMap) {
		// Finding 14: Support direct JSON body fields first so passwords with commas are never truncated
		String directNewPassword = inputDataMap.get(NEW_PASSWORD);
		String directUserId = inputDataMap.get(USER_ID);
		String directCurrentPassword = resolveCurrentPassword(inputDataMap, null);
		String directConfirmPassword = inputDataMap.get(CONFIRM_PASSWORD);

		if (directNewPassword != null && !directNewPassword.isBlank() && directCurrentPassword != null && !directCurrentPassword.isBlank()) {
			String resolvedUser = (directUserId != null && !directUserId.isBlank()) ? directUserId : resolveSecurityContextUser();
			return new PasswordChangePayload(directNewPassword, resolvedUser, directCurrentPassword, directConfirmPassword);
		}

		PasswordChangePayload encrypted = resolveEncryptedPayload(inputDataMap.get("inputData"));

		String newPassword = (encrypted != null && encrypted.newPassword() != null && !encrypted.newPassword().isBlank())
				? encrypted.newPassword()
				: inputDataMap.get(NEW_PASSWORD);

		String resolvedUserId = (encrypted != null && encrypted.resolvedUserId() != null && !encrypted.resolvedUserId().isBlank())
				? encrypted.resolvedUserId()
				: inputDataMap.get(USER_ID);

		String currentPassword = resolveCurrentPassword(inputDataMap, encrypted != null ? encrypted.currentPassword() : null);

		String confirmPassword = (encrypted != null && encrypted.confirmPassword() != null && !encrypted.confirmPassword().isBlank())
				? encrypted.confirmPassword()
				: inputDataMap.get(CONFIRM_PASSWORD);

		if (resolvedUserId == null || resolvedUserId.isBlank()) {
			resolvedUserId = resolveSecurityContextUser();
		}
		return new PasswordChangePayload(newPassword, resolvedUserId, currentPassword, confirmPassword);
	}

	private String validatePasswordChangePayload(PasswordChangePayload payload) {
		if (payload.resolvedUserId() == null || payload.resolvedUserId().isBlank()) {
			return "User ID is required";
		}
		if (payload.currentPassword() == null || payload.currentPassword().isBlank()) {
			return "Current password is required";
		}
		if (payload.newPassword() == null || payload.newPassword().isBlank()) {
			return "New password is required";
		}
		if (payload.resolvedUserId().length() > 100) {
			return "User ID cannot exceed 100 characters";
		}
		if (payload.currentPassword().length() > 256 || payload.newPassword().length() > 256) {
			return "Password cannot exceed 256 characters";
		}
		if (payload.confirmPassword() != null && !payload.confirmPassword().isBlank()
				&& !"NA".equalsIgnoreCase(payload.confirmPassword()) && !payload.newPassword().equals(payload.confirmPassword())) {
			return "New password and confirmation password do not match";
		}
		return null;
	}

	@Override
	@Transactional
	public ResponseEntity<Object> changeUserPassword(Map<String, String> inputDataMap) {

		if (inputDataMap == null || inputDataMap.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Invalid request data", SUCCESS, false));
		}

		PasswordChangePayload payload = resolvePasswordChangePayload(inputDataMap);
		String error = validatePasswordChangePayload(payload);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of(MESSAGE, error, SUCCESS, false));
		}

		ChangePasswordResponse result = changeUserPassword(payload.newPassword(), payload.resolvedUserId(), payload.currentPassword());

		if (!result.isSuccess()) {
			Map<String, Object> errorBody = new LinkedHashMap<>();
			errorBody.put(SUCCESS, false);
			errorBody.put(MESSAGE, result.getMessage());
			if (result.getViolations() != null && !result.getViolations().isEmpty()) {
				errorBody.put("violations", result.getViolations());
			}
			return ResponseEntity.badRequest().body(errorBody);
		}

		Map<String, Object> successBody = new LinkedHashMap<>();
		successBody.put(SUCCESS, true);
		successBody.put(MESSAGE, result.getMessage());
		return ResponseEntity.ok(successBody);
	}

	private String[] parseJsonPasswordPayload(String trimmed) {
		String newPass = extractJsonField(trimmed, NEW_PASSWORD);
		String user = extractJsonField(trimmed, USER_ID);
		if (user == null) user = extractJsonField(trimmed, USERNAME);
		String currentPass = extractJsonField(trimmed, "currentPassword");
		if (currentPass == null) currentPass = extractJsonField(trimmed, "oldPassword");
		String confirmPass = extractJsonField(trimmed, CONFIRM_PASSWORD);
		if (newPass != null && user != null && currentPass != null) {
			return confirmPass != null
					? new String[]{ newPass, user, currentPass, confirmPass }
					: new String[]{ newPass, user, currentPass };
		}
		return new String[0];
	}

	private String[] decryptPwdData(String inputPwdData) {
		if (inputPwdData == null || inputPwdData.isBlank()) {
			return new String[0];
		}

		try {
			byte[] decodedBytes = Hex.decodeHex(inputPwdData.toCharArray());
			String dataString = new String(decodedBytes, StandardCharsets.UTF_8).trim();

			if (dataString.startsWith("{") && dataString.endsWith("}")) {
				String[] jsonDetails = parseJsonPasswordPayload(dataString);
				if (jsonDetails.length > 0) {
					return jsonDetails;
				}
			}

			// Split on first 3 commas only so commas in passwords are not fragmented (Finding 14)
			String[] loginDetails = dataString.split(",", 4);
			return loginDetails.length >= 3 ? loginDetails : new String[0];
		} catch (DecoderException e) {
			return new String[0];
		}
	}

	private ChangePasswordResponse changeUserPassword(String newPassword, String userId, String currentPassword) {
		UsersEntityV2 userEntity = usersRepositoryV2.findByUserId(userId);

		if (userEntity == null) {
			return ChangePasswordResponse.ofFailure("User not found");
		}

		UserCredentialEntity credential = userCredentialRepository.findByUserSerialNo(userEntity.getSerialNumber());

		if (credential == null) {
			return ChangePasswordResponse.ofFailure("User credentials not found");
		}

		LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);

		// Check whether the account is currently locked (Finding 12)
		if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(now)) {
			return ChangePasswordResponse.ofFailure("Account is currently locked. Try again later.");
		}

		// Clear an expired lock before checking the password
		if (credential.getLockedUntil() != null && !credential.getLockedUntil().isAfter(now)) {
			credential.setLockedUntil(null);
		}

		// Validate current password with failed login tracking and lockout enforcement (Finding 12)
		if (!validateLogin(credential, currentPassword)) {
			registerFailedLogin(credential, now);
			return ChangePasswordResponse.ofFailure("Current password is incorrect");
		}

		// Enforce active password policy engine
		if (passwordPolicyService != null) {
			UserContext userContext = new UserContext(userId, userEntity.getEmail(), userEntity.getDisplayName());
			PasswordValidationResult policyResult = passwordPolicyService.validatePassword(newPassword, userContext);
			if (!policyResult.valid()) {
				String errors = String.join(", ", policyResult.violations());
				log.warn("Password change for user [{}] rejected: does not satisfy active password policy. Violations: {}", userId, errors);
				return ChangePasswordResponse.ofFailure("Password does not meet active policy requirements: " + errors, policyResult.violations());
			}
		}

		// Ensure new password is not identical to current password
		if (passwordEncoder.matches(newPassword, credential.getPasswordHash())) {
			return ChangePasswordResponse.ofFailure("New password cannot be the same as the current password");
		}

		credential.setPasswordHash(passwordEncoder.encode(newPassword));
		credential.setPasswordChangedAt(now);
		credential.setPasswordExpiresAt(now.plusDays(365));
		credential.setMustChange("N");
		credential.setFailedAttempts(0);
		credential.setFirstFailedAt(null);
		credential.setLockedUntil(null);
		userCredentialRepository.save(credential);

		log.info("Password successfully changed for user [{}]", userId);
		return ChangePasswordResponse.ofSuccess("Password changed successfully");
	}

	@Override
	public ResponseEntity<Object> logout(HttpServletRequest request) {
		String userId = resolveCurrentUserId(request);
		log.info("Processing logout for user: {}", userId);

		recordLogoutActivity(userId, request);

		if (request != null) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
		}

		SecurityContextHolder.clearContext();

		HttpHeaders headers = buildLogoutHeaders();

		Map<String, Object> responseBody = new LinkedHashMap<>();
		responseBody.put(MESSAGE, "Logout successful");
		if (userId != null) {
			responseBody.put(USER_ID, userId);
		}

		return ResponseEntity.ok()
				.headers(headers)
				.body(responseBody);
	}

	private void recordLogoutActivity(String userId, HttpServletRequest request) {
		if (userActivityTrackingService != null) {
			try {
				UserActivityTrackingVO vo = new UserActivityTrackingVO();
				vo.setActorUserId(userId != null ? userId : "ANONYMOUS");
				vo.setEventType("LOGOUT");
				vo.setResourceType("AUTH");
				vo.setResourceId(userId != null ? userId : "N/A");
				vo.setDetail("User logged out successfully");
				userActivityTrackingService.saveUserActivity(vo, request);
			} catch (Exception e) {
				log.warn("Failed to record logout user activity: {}", e.getMessage());
			}
		}
	}

	private HttpHeaders buildLogoutHeaders() {
		ResponseCookie accessCookie = ResponseCookie.from(APIConstants.ACCESS_TOKEN_COOKIE, "")
				.httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite(SAME_SITE_STRICT).build();
		ResponseCookie authCookie = ResponseCookie.from("AUTH_TOKEN", "")
				.httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite(SAME_SITE_STRICT).build();
		ResponseCookie refreshCookie = ResponseCookie.from(APIConstants.REFRESH_TOKEN_COOKIE, "")
				.httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite(SAME_SITE_STRICT).build();

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
		headers.add(HttpHeaders.SET_COOKIE, authCookie.toString());
		headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
		return headers;
	}

	private String resolveCurrentUserId(HttpServletRequest request) {
		String userId = null;
		if (request != null) {
			try {
				userId = jwtUtils.getUserId(request);
			} catch (Exception ex) {
				// Fallback to SecurityContext
			}
		}
		if (userId == null || userId.isBlank()) {
			try {
				var auth = SecurityContextHolder.getContext().getAuthentication();
				if (auth != null && auth.isAuthenticated() && !ANONYMOUS_USER.equalsIgnoreCase(auth.getName())) {
					userId = auth.getName();
				}
			} catch (Exception ex) {
				// unauthenticated
			}
		}
		return (userId != null && !userId.isBlank() && !ANONYMOUS_USER.equalsIgnoreCase(userId)) ? userId : null;
	}

	@Override
	public ResponseEntity<Object> getCurrentUserSession(HttpServletRequest request) {
		String userId = resolveCurrentUserId(request);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of(AUTHENTICATED, false, MESSAGE, "No active session"));
		}

		UsersEntityV2 userEntity = usersRepositoryV2.findByUserId(userId);
		if (userEntity == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of(AUTHENTICATED, false, MESSAGE, "User not found"));
		}

		if (userEntity.getStatus() != null && !"ACTIVE".equalsIgnoreCase(userEntity.getStatus())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of(AUTHENTICATED, false, MESSAGE, "User account is suspended or inactive"));
		}

		String userRole = resolveUserRole(userEntity);
		Map<String, Object> session = new LinkedHashMap<>();
		session.put(AUTHENTICATED, true);
		session.put(USER_ID, userEntity.getUserId());
		session.put("userSerialNumber", userEntity.getSerialNumber());
		String displayName = userEntity.getDisplayName() != null ? userEntity.getDisplayName() : userEntity.getUserId();
		session.put("userName", displayName);
		session.put("name", displayName);
		session.put("email", userEntity.getEmail());
		session.put("userEmail", userEntity.getEmail());
		session.put("status", userEntity.getStatus());
		if (userRole != null) {
			session.put("role", userRole);
			session.put("userRole", userRole);
		}

		// Note: token and accessToken are intentionally omitted from response body (Finding S5).
		// Authentication is maintained securely via HttpOnly session cookies.
		return ResponseEntity.ok(session);
	}
}
