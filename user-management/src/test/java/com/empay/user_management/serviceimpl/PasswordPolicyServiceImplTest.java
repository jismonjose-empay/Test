package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.empay.user_management.dto.CreatePasswordPolicyRequest;
import com.empay.user_management.dto.PasswordPoliciesOverviewResponse;
import com.empay.user_management.dto.PasswordPolicyResponse;
import com.empay.user_management.dto.PasswordPolicySummaryResponse;
import com.empay.user_management.dto.PasswordValidationResult;
import com.empay.user_management.dto.UpdatePasswordPolicyRequest;
import com.empay.user_management.dto.UserContext;
import com.empay.user_management.entities.PasswordPolicyEntity;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.PasswordBlocklistRepository;
import com.empay.user_management.repository.PasswordPolicyRepository;
import com.empay.user_management.utils.JwtUtils;

/**
 * Unit tests for PasswordPolicyServiceImpl.
 * Verifies policy CRUD operations, activation transactions, and password complexity evaluations.
 */
@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceImplTest {

	@Mock
	private PasswordPolicyRepository passwordPolicyRepository;

	@Mock
	private PasswordBlocklistRepository passwordBlocklistRepository;

	@Mock
	private JwtUtils jwtUtils;

	@InjectMocks
	private PasswordPolicyServiceImpl passwordPolicyService;

	private PasswordPolicyEntity activePolicy;

	@BeforeEach
	void setUp() {
		activePolicy = PasswordPolicyEntity.builder()
			.policyId(1)
			.policyCode("STANDARD_POLICY")
			.name("Standard Bank Policy")
			.description("Default bank-wide policy")
			.isActive("Y")
			.minLength(12)
			.maxLength(64)
			.requireUppercase("Y")
			.requireLowercase("Y")
			.requireDigit("Y")
			.requireSpecial("Y")
			.minCharacterClasses(3)
			.checkCommonPasswords("Y")
			.checkUserSimilarity("Y")
			.checkSequential("Y")
			.expiryDays(90)
			.warnBeforeDays(14)
			.minAgeHours(24)
			.historyDepth(12)
			.maxFailedAttempts(5)
			.lockoutMinutes(30)
			.attemptWindowMinutes(15)
			.lockoutsBeforeDisable(3)
			.hashCost(12)
			.updatedBy("SYSTEM")
			.updatedAt(LocalDateTime.now())
			.build();
	}

	@Test
	void testGetPoliciesOverview_WithActivePolicy() {
		when(passwordPolicyRepository.findAllByOrderByPolicyIdAsc()).thenReturn(List.of(activePolicy));
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));

		PasswordPoliciesOverviewResponse result = passwordPolicyService.getPoliciesOverview();

		assertNotNull(result);
		assertEquals(1, result.policies().size());
		assertEquals("STANDARD_POLICY", result.policies().get(0).policyCode());
		assertEquals("ACTIVE", result.policies().get(0).policyStatus());
		assertTrue(result.policies().get(0).isActive());
		assertNotNull(result.activePolicy());
		assertEquals("STANDARD_POLICY", result.activePolicy().policyCode());
	}

	@Test
	void testGetPoliciesOverview_NoActivePolicy() {
		when(passwordPolicyRepository.findAllByOrderByPolicyIdAsc()).thenReturn(List.of(activePolicy));
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.empty());

		PasswordPoliciesOverviewResponse result = passwordPolicyService.getPoliciesOverview();

		assertNotNull(result);
		assertEquals(1, result.policies().size());
		assertNull(result.activePolicy());
	}

	@Test
	void testGetAllPolicies() {
		when(passwordPolicyRepository.findAllByOrderByPolicyIdAsc()).thenReturn(List.of(activePolicy));

		List<PasswordPolicyResponse> result = passwordPolicyService.getAllPolicies();

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("STANDARD_POLICY", result.get(0).policyCode());
	}

	@Test
	void testGetPolicyById_Success() {
		when(passwordPolicyRepository.findById(1)).thenReturn(Optional.of(activePolicy));

		PasswordPolicyResponse result = passwordPolicyService.getPolicyById(1);

		assertNotNull(result);
		assertEquals(1, result.policyId());
		assertTrue(result.isActive());
	}

	@Test
	void testGetPolicyById_NotFound_ThrowsException() {
		when(passwordPolicyRepository.findById(99)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () ->
			passwordPolicyService.getPolicyById(99));
	}

	@Test
	void testGetPolicyByCode_Success() {
		when(passwordPolicyRepository.findByPolicyCode("STANDARD_POLICY")).thenReturn(Optional.of(activePolicy));

		PasswordPolicyResponse result = passwordPolicyService.getPolicyByCode("standard_policy");

		assertNotNull(result);
		assertEquals("STANDARD_POLICY", result.policyCode());
	}

	@Test
	void testGetPolicyByCode_Blank_ThrowsException() {
		assertThrows(IllegalArgumentException.class, () ->
			passwordPolicyService.getPolicyByCode("   "));
	}

	@Test
	void testGetActivePolicy_Success() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));

		PasswordPolicyResponse result = passwordPolicyService.getActivePolicy();

		assertNotNull(result);
		assertTrue(result.isActive());
	}

	@Test
	void testGetActivePolicy_NotFound_ThrowsException() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () ->
			passwordPolicyService.getActivePolicy());
	}

	@Test
	void testCreatePolicy_Success() {
		CreatePasswordPolicyRequest request = new CreatePasswordPolicyRequest(
			"HIGH_SECURITY", "High Security Policy", "Strict rules", false,
			16, 64, true, true, true, true, 4,
			true, true, true, 60, 10, 24, 24,
			3, 60, 15, 2, 12
		);

		when(passwordPolicyRepository.existsByPolicyCode("HIGH_SECURITY")).thenReturn(false);
		when(passwordPolicyRepository.saveAndFlush(any(PasswordPolicyEntity.class))).thenAnswer(i -> {
			PasswordPolicyEntity entity = i.getArgument(0);
			entity.setPolicyId(2);
			return entity;
		});

		PasswordPolicyResponse response = passwordPolicyService.createPolicy(request, null);

		assertNotNull(response);
		assertEquals("HIGH_SECURITY", response.policyCode());
		assertEquals(16, response.minLength());
		assertFalse(response.isActive());
	}

	@Test
	void testCreatePolicy_AlreadyExists_ThrowsException() {
		CreatePasswordPolicyRequest request = new CreatePasswordPolicyRequest(
			"STANDARD_POLICY", "Duplicate", null, false,
			15, 64, null, null, null, null, null,
			null, null, null, null, null, null, null,
			null, null, null, null, null
		);

		when(passwordPolicyRepository.existsByPolicyCode("STANDARD_POLICY")).thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class, () ->
			passwordPolicyService.createPolicy(request, null));
	}

	@Test
	void testCreatePolicy_InvalidRules_ThrowsException() {
		CreatePasswordPolicyRequest invalidLength = new CreatePasswordPolicyRequest(
			"INVALID_LEN", "Invalid Length", null, false,
			5, 64, null, null, null, null, null,
			null, null, null, null, null, null, null,
			null, null, null, null, null
		);
		when(passwordPolicyRepository.existsByPolicyCode("INVALID_LEN")).thenReturn(false);

		assertThrows(IllegalArgumentException.class, () ->
			passwordPolicyService.createPolicy(invalidLength, null));
	}

	@Test
	void testCreatePolicy_Active_DeactivatesPrevious() {
		CreatePasswordPolicyRequest request = new CreatePasswordPolicyRequest(
			"NEW_ACTIVE", "New Active Policy", null, true,
			15, 64, true, true, true, true, 3,
			true, true, true, 0, 14, 24, 12,
			5, 30, 15, 3, 12
		);

		when(passwordPolicyRepository.existsByPolicyCode("NEW_ACTIVE")).thenReturn(false);
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));
		when(passwordPolicyRepository.saveAndFlush(any(PasswordPolicyEntity.class))).thenAnswer(i -> i.getArgument(0));

		PasswordPolicyResponse response = passwordPolicyService.createPolicy(request, null);

		assertNotNull(response);
		assertTrue(response.isActive());
		assertFalse(activePolicy.isPolicyActive());
		verify(passwordPolicyRepository).saveAndFlush(activePolicy);
	}

	@Test
	void testUpdatePolicy_Success() {
		when(passwordPolicyRepository.findById(1)).thenReturn(Optional.of(activePolicy));
		when(passwordPolicyRepository.saveAndFlush(any(PasswordPolicyEntity.class))).thenAnswer(i -> i.getArgument(0));

		UpdatePasswordPolicyRequest request = new UpdatePasswordPolicyRequest(
			"Updated Name", "Updated Desc", null,
			15, 64, null, null, null, null, null,
			null, null, null, null, null, null, null,
			null, null, null, null, null
		);

		PasswordPolicyResponse response = passwordPolicyService.updatePolicy(1, request, null);

		assertNotNull(response);
		assertEquals("Updated Name", response.name());
		assertEquals(15, response.minLength());
	}

	@Test
	void testActivatePolicy_Success() {
		PasswordPolicyEntity inactive = PasswordPolicyEntity.builder()
			.policyId(2)
			.policyCode("SECONDARY")
			.name("Secondary")
			.isActive("N")
			.minLength(12)
			.maxLength(64)
			.build();

		when(passwordPolicyRepository.findById(2)).thenReturn(Optional.of(inactive));
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));
		when(passwordPolicyRepository.saveAndFlush(any(PasswordPolicyEntity.class))).thenAnswer(i -> i.getArgument(0));

		PasswordPolicyResponse response = passwordPolicyService.activatePolicy(2, null);

		assertNotNull(response);
		assertTrue(response.isActive());
		assertFalse(activePolicy.isPolicyActive());
	}

	@Test
	void testDeletePolicy_Success() {
		PasswordPolicyEntity inactive = PasswordPolicyEntity.builder()
			.policyId(2)
			.policyCode("OBSOLETE")
			.name("Obsolete")
			.isActive("N")
			.build();

		when(passwordPolicyRepository.findById(2)).thenReturn(Optional.of(inactive));

		passwordPolicyService.deletePolicy(2);

		verify(passwordPolicyRepository).delete(inactive);
	}

	@Test
	void testDeletePolicy_Active_ThrowsException() {
		when(passwordPolicyRepository.findById(1)).thenReturn(Optional.of(activePolicy));

		assertThrows(IllegalArgumentException.class, () ->
			passwordPolicyService.deletePolicy(1));

		verify(passwordPolicyRepository, never()).delete(any());
	}

	@Test
	void testDeletePolicyByCode_Success() {
		PasswordPolicyEntity inactive = PasswordPolicyEntity.builder()
			.policyId(2)
			.policyCode("OBSOLETE")
			.name("Obsolete")
			.isActive("N")
			.build();

		when(passwordPolicyRepository.findByPolicyCode("OBSOLETE")).thenReturn(Optional.of(inactive));

		passwordPolicyService.deletePolicyByCode("obsolete");

		verify(passwordPolicyRepository).delete(inactive);
	}

	@Test
	void testDeletePolicyByCode_Active_ThrowsException() {
		when(passwordPolicyRepository.findByPolicyCode("STANDARD_POLICY")).thenReturn(Optional.of(activePolicy));

		assertThrows(IllegalArgumentException.class, () ->
			passwordPolicyService.deletePolicyByCode("standard_policy"));

		verify(passwordPolicyRepository, never()).delete(any());
	}

	@Test
	void testDeletePolicyByCode_Blank_ThrowsException() {
		assertThrows(IllegalArgumentException.class, () ->
			passwordPolicyService.deletePolicyByCode("   "));
	}

	@Test
	void testDeletePolicyByCode_NotFound_ThrowsException() {
		when(passwordPolicyRepository.findByPolicyCode("NON_EXISTING")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () ->
			passwordPolicyService.deletePolicyByCode("non_existing"));
	}

	@Test
	void testValidatePassword_Empty_Fails() {
		PasswordValidationResult result = passwordPolicyService.validatePassword("");
		assertFalse(result.valid());
		assertEquals(1, result.violations().size());
	}

	@Test
	void testValidatePassword_Success() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));
		when(passwordBlocklistRepository.existsById("correcthorsebatterystaple1!")).thenReturn(false);

		UserContext context = new UserContext("john_doe", "john.doe@empay.com", "Johnathan Doe");
		PasswordValidationResult result = passwordPolicyService.validatePassword("CorrectHorseBatteryStaple1!", context);

		assertTrue(result.valid());
		assertTrue(result.violations().isEmpty());
	}

	@ParameterizedTest
	@CsvSource({
		"Short1!, at least 12 characters",
		"MyPassword123!, sequential character sequences",
		"Password321!Abc, sequential character sequences",
		"ALLUPPERCASEPASS123!, lowercase letter",
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1!, must not exceed 64 characters"
	})
	void testValidatePassword_StructuralViolations_Fails(String candidatePassword, String expectedViolationSnippet) {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));

		PasswordValidationResult result = passwordPolicyService.validatePassword(candidatePassword);

		assertFalse(result.valid());
		assertTrue(result.violations().stream().anyMatch(v -> v.contains(expectedViolationSnippet)));
	}

	@Test
	void testValidatePassword_MissingCharacterClasses_Fails() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));

		// All lowercase
		PasswordValidationResult result = passwordPolicyService.validatePassword("alllowercasepasswordwithoutdigits");
		assertFalse(result.valid());
		assertTrue(result.violations().stream().anyMatch(v -> v.contains("uppercase letter")));
		assertTrue(result.violations().stream().anyMatch(v -> v.contains("numeric digit")));
		assertTrue(result.violations().stream().anyMatch(v -> v.contains("special character")));
	}

	@Test
	void testValidatePassword_CommonPasswordBlocklist_Fails() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));

		// Test static fallback
		PasswordValidationResult resultFallback = passwordPolicyService.validatePassword("Password123!");
		assertFalse(resultFallback.valid());

		// Test database blocklist
		when(passwordBlocklistRepository.existsById("supersecretcustompass1!")).thenReturn(true);
		PasswordValidationResult resultDb = passwordPolicyService.validatePassword("SuperSecretCustomPass1!");
		assertFalse(resultDb.valid());
		assertTrue(resultDb.violations().stream().anyMatch(v -> v.contains("password blocklist")));
	}

	@Test
	void testValidatePassword_UserSimilarity_Fails() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));

		UserContext context = new UserContext("johnny", "johnny@empay.com", "Johnny Bravo");
		PasswordValidationResult result = passwordPolicyService.validatePassword("SecureJohnnyPass1!", context);

		assertFalse(result.valid());
		assertTrue(result.violations().stream().anyMatch(v -> v.contains("username, email, or full name")));
	}

	@Test
	void testFindPolicyById_InvalidId_ThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.getPolicyById(null));
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.getPolicyById(0));
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.getPolicyById(-5));
	}

	@Test
	void testActivatePolicy_AlreadyActive_ReturnsImmediately() {
		when(passwordPolicyRepository.findById(1)).thenReturn(Optional.of(activePolicy));

		PasswordPolicyResponse response = passwordPolicyService.activatePolicy(1, null);

		assertNotNull(response);
		assertTrue(response.isActive());
		verify(passwordPolicyRepository, never()).saveAndFlush(any());
	}

	@Test
	void testUpdatePolicy_AllFieldsUpdated_AndDeactivatePrevious() {
		PasswordPolicyEntity inactiveEntity = PasswordPolicyEntity.builder()
			.policyId(3)
			.policyCode("TEST_INACTIVE")
			.name("Test Inactive")
			.isActive("N")
			.minLength(10)
			.maxLength(64)
			.expiryDays(90)
			.warnBeforeDays(10)
			.minAgeHours(24)
			.minCharacterClasses(2)
			.hashCost(12)
			.build();

		when(passwordPolicyRepository.findById(3)).thenReturn(Optional.of(inactiveEntity));
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));
		when(passwordPolicyRepository.saveAndFlush(any(PasswordPolicyEntity.class))).thenAnswer(i -> i.getArgument(0));

		UpdatePasswordPolicyRequest request = new UpdatePasswordPolicyRequest(
			"Updated Full Policy", "New full description", true,
			14, 64, false, false, false, false, 2,
			false, false, false, 60, 10, 24, 10,
			3, 20, 10, 2, 11
		);

		PasswordPolicyResponse response = passwordPolicyService.updatePolicy(3, request, null);

		assertNotNull(response);
		assertTrue(response.isActive());
		assertEquals("Updated Full Policy", response.name());
		assertEquals(14, response.minLength());
		assertFalse(response.requireUppercase());
		assertEquals(2, response.minCharacterClasses());
		assertFalse(response.checkCommonPasswords());
		assertEquals(60, response.expiryDays());
		assertEquals(3, response.maxFailedAttempts());
		assertEquals(11, response.hashCost());
	}

	@Test
	void testValidatePolicyRules_AllValidationBranches() {
		when(passwordPolicyRepository.existsByPolicyCode(any())).thenReturn(false);

		// minLength < 8
		CreatePasswordPolicyRequest reqMinLen = new CreatePasswordPolicyRequest(
			"C1", "N1", null, false, 7, 64, null, null, null, null, 1, null, null, null, 30, 5, 12, null, null, null, null, null, 12
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqMinLen, null));

		// maxLength < 64
		CreatePasswordPolicyRequest reqMaxLen = new CreatePasswordPolicyRequest(
			"C2", "N2", null, false, 8, 63, null, null, null, null, 1, null, null, null, 30, 5, 12, null, null, null, null, null, 12
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqMaxLen, null));

		// minLength > maxLength
		CreatePasswordPolicyRequest reqMinGtMax = new CreatePasswordPolicyRequest(
			"C3", "N3", null, false, 70, 64, null, null, null, null, 1, null, null, null, 30, 5, 12, null, null, null, null, null, 12
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqMinGtMax, null));

		// minAgeHours >= expiryDays * 24
		CreatePasswordPolicyRequest reqMinAgeTooHigh = new CreatePasswordPolicyRequest(
			"C4", "N4", null, false, 8, 64, null, null, null, null, 1, null, null, null, 2, 1, 48, null, null, null, null, null, 12
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqMinAgeTooHigh, null));

		// warnBeforeDays >= expiryDays
		CreatePasswordPolicyRequest reqWarnTooHigh = new CreatePasswordPolicyRequest(
			"C5", "N5", null, false, 8, 64, null, null, null, null, 1, null, null, null, 10, 10, 12, null, null, null, null, null, 12
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqWarnTooHigh, null));

		// minClasses < 0
		CreatePasswordPolicyRequest reqClassesNeg = new CreatePasswordPolicyRequest(
			"C6", "N6", null, false, 8, 64, null, null, null, null, -1, null, null, null, 30, 5, 12, null, null, null, null, null, 12
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqClassesNeg, null));

		// minClasses > 4
		CreatePasswordPolicyRequest reqClassesHigh = new CreatePasswordPolicyRequest(
			"C7", "N7", null, false, 8, 64, null, null, null, null, 5, null, null, null, 30, 5, 12, null, null, null, null, null, 12
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqClassesHigh, null));

		// hashCost < 10
		CreatePasswordPolicyRequest reqHashLow = new CreatePasswordPolicyRequest(
			"C8", "N8", null, false, 8, 64, null, null, null, null, 2, null, null, null, 30, 5, 12, null, null, null, null, null, 9
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqHashLow, null));

		// hashCost > 14
		CreatePasswordPolicyRequest reqHashHigh = new CreatePasswordPolicyRequest(
			"C9", "N9", null, false, 8, 64, null, null, null, null, 2, null, null, null, 30, 5, 12, null, null, null, null, null, 15
		);
		assertThrows(IllegalArgumentException.class, () -> passwordPolicyService.createPolicy(reqHashHigh, null));
	}

	@Test
	void testResolveActor_FromHttpRequest_Success() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(jwtUtils.getUserId(request)).thenReturn("MOCK_ADMIN_USER");
		when(passwordPolicyRepository.existsByPolicyCode("ACTOR_TEST")).thenReturn(false);
		when(passwordPolicyRepository.saveAndFlush(any(PasswordPolicyEntity.class))).thenAnswer(i -> i.getArgument(0));

		CreatePasswordPolicyRequest createReq = new CreatePasswordPolicyRequest(
			"ACTOR_TEST", "Actor Test Policy", null, false,
			10, 64, null, null, null, null, null, null, null, null, 30, 5, 12, null, null, null, null, null, 12
		);

		passwordPolicyService.createPolicy(createReq, request);
		verify(jwtUtils).getUserId(request);
	}

	@Test
	void testResolveActor_JwtException_FallsBackToSystem() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(jwtUtils.getUserId(request)).thenThrow(new RuntimeException("Token error"));
		when(passwordPolicyRepository.existsByPolicyCode("FALLBACK_ACTOR")).thenReturn(false);
		when(passwordPolicyRepository.saveAndFlush(any(PasswordPolicyEntity.class))).thenAnswer(i -> i.getArgument(0));

		CreatePasswordPolicyRequest createReq = new CreatePasswordPolicyRequest(
			"FALLBACK_ACTOR", "Fallback Actor Policy", null, false,
			10, 64, null, null, null, null, null, null, null, null, 30, 5, 12, null, null, null, null, null, 12
		);

		PasswordPolicyResponse response = passwordPolicyService.createPolicy(createReq, request);
		assertNotNull(response);
	}

	@Test
	void testValidatePassword_NullPassword_Fails() {
		PasswordValidationResult result = passwordPolicyService.validatePassword(null);
		assertFalse(result.valid());
		assertEquals("Password must not be empty", result.violations().get(0));
	}

	@Test
	void testValidatePassword_NoActivePolicy_UsesFallbackPolicy() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.empty());

		PasswordValidationResult result = passwordPolicyService.validatePassword("Fallback!9#mZ");
		assertNotNull(result);
		assertTrue(result.valid());
	}

	@Test
	void testValidatePassword_UserSimilarity_NullAndBlankFields() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));
		when(passwordBlocklistRepository.existsById(any())).thenReturn(false);

		UserContext context = new UserContext(null, "", "   ");
		PasswordValidationResult result = passwordPolicyService.validatePassword("CompliantPassword1!", context);

		assertTrue(result.valid());
	}

	@Test
	void testValidatePassword_UserSimilarity_EmailPrefixMatch() {
		when(passwordPolicyRepository.findByIsActive("Y")).thenReturn(Optional.of(activePolicy));

		UserContext context = new UserContext(null, "johndoe@empay.com", null);
		PasswordValidationResult result = passwordPolicyService.validatePassword("Myjohndoepass1!", context);

		assertFalse(result.valid());
		assertTrue(result.violations().stream().anyMatch(v -> v.contains("username, email, or full name")));
	}

	@Test
	void testUserContext_Helpers() {
		UserContext context = UserContext.of("alice");
		assertEquals("alice", context.username());
		assertNotNull(UserContext.empty());
	}

	@Test
	void testPasswordPolicySummaryResponse_Helpers() {
		assertNull(PasswordPolicySummaryResponse.fromEntity(null));
	}

	@Test
	void testPasswordPoliciesOverviewResponse_Helpers() {
		PasswordPoliciesOverviewResponse nullPayload = PasswordPoliciesOverviewResponse.of(null, null);
		assertNotNull(nullPayload);
		assertTrue(nullPayload.policies().isEmpty());
		assertNull(nullPayload.activePolicy());

		PasswordPoliciesOverviewResponse withData = PasswordPoliciesOverviewResponse.of(List.of(), null);
		assertNotNull(withData);
		assertTrue(withData.policies().isEmpty());
	}
}
