package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.empay.user_management.dto.CreatePasswordPolicyRequest;
import com.empay.user_management.dto.PasswordPoliciesOverviewResponse;
import com.empay.user_management.dto.PasswordPolicyResponse;
import com.empay.user_management.dto.PasswordPolicySummaryResponse;
import com.empay.user_management.dto.PasswordValidationResult;
import com.empay.user_management.dto.UpdatePasswordPolicyRequest;
import com.empay.user_management.exceptions.GlobalExceptionHandler;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.service.PasswordPolicyService;

/**
 * Unit tests for PasswordPolicyController.
 * Tests REST endpoint contracts, response statuses, and exception translations.
 */
@ExtendWith(MockitoExtension.class)
class PasswordPolicyControllerTest {

	private MockMvc mockMvc;

	@Mock
	private PasswordPolicyService passwordPolicyService;

	@InjectMocks
	private PasswordPolicyController passwordPolicyController;

	private PasswordPolicyResponse samplePolicy;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(passwordPolicyController)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

		samplePolicy = new PasswordPolicyResponse(
			1,
			"DEFAULT_POLICY",
			"Default Policy",
			"Standard policy",
			true,
			15,
			64,
			true,
			true,
			true,
			true,
			3,
			true,
			true,
			true,
			90,
			14,
			24,
			12,
			5,
			30,
			15,
			3,
			12,
			"SYSTEM",
			LocalDateTime.now()
		);
	}

	@Test
	void testGetPoliciesOverview() throws Exception {
		PasswordPolicySummaryResponse summary = new PasswordPolicySummaryResponse(
			1, "DEFAULT_POLICY", "Default Bank Policy", "Default description", "ACTIVE", true
		);
		PasswordPoliciesOverviewResponse overview = PasswordPoliciesOverviewResponse.of(
			List.of(summary), samplePolicy
		);
		when(passwordPolicyService.getPoliciesOverview()).thenReturn(overview);

		mockMvc.perform(get("/api/password-policies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.policies[0].policyCode").value("DEFAULT_POLICY"))
			.andExpect(jsonPath("$.policies[0].policyStatus").value("ACTIVE"))
			.andExpect(jsonPath("$.activePolicy.policyCode").value("DEFAULT_POLICY"));

		mockMvc.perform(get("/api/password-policies/overview"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.policies[0].policyCode").value("DEFAULT_POLICY"));
	}

	@Test
	void testGetAllPolicies() throws Exception {
		when(passwordPolicyService.getAllPolicies()).thenReturn(List.of(samplePolicy));

		mockMvc.perform(get("/api/password-policies/all"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].policyId").value(1))
			.andExpect(jsonPath("$[0].policyCode").value("DEFAULT_POLICY"));
	}

	@Test
	void testGetActivePolicy_Success() throws Exception {
		when(passwordPolicyService.getActivePolicy()).thenReturn(samplePolicy);

		mockMvc.perform(get("/api/password-policies/active"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.policyCode").value("DEFAULT_POLICY"))
			.andExpect(jsonPath("$.isActive").value(true));
	}

	@Test
	void testGetActivePolicy_NotFound() throws Exception {
		when(passwordPolicyService.getActivePolicy())
			.thenThrow(new ResourceNotFoundException("PasswordPolicy", "isActive", "No active policy found"));

		mockMvc.perform(get("/api/password-policies/active"))
			.andExpect(status().isNotFound());
	}

	@Test
	void testGetPolicyById_Success() throws Exception {
		when(passwordPolicyService.getPolicyById(1)).thenReturn(samplePolicy);

		mockMvc.perform(get("/api/password-policies/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.policyId").value(1));
	}

	@Test
	void testGetPolicyByCode_Success() throws Exception {
		when(passwordPolicyService.getPolicyByCode("DEFAULT_POLICY")).thenReturn(samplePolicy);

		mockMvc.perform(get("/api/password-policies/code/DEFAULT_POLICY"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.policyCode").value("DEFAULT_POLICY"));
	}

	@Test
	void testCreatePolicy_Success() throws Exception {
		when(passwordPolicyService.createPolicy(any(CreatePasswordPolicyRequest.class), any()))
			.thenReturn(samplePolicy);

		String json = """
			{
				"policyCode": "HIGH_SEC",
				"name": "High Security",
				"minLength": 16,
				"maxLength": 64
			}
			""";

		mockMvc.perform(post("/api/password-policies")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.policyCode").value("DEFAULT_POLICY"));
	}

	@Test
	void testUpdatePolicy_Success() throws Exception {
		when(passwordPolicyService.updatePolicy(eq(1), any(UpdatePasswordPolicyRequest.class), any()))
			.thenReturn(samplePolicy);

		String json = """
			{
				"name": "Updated Policy Name",
				"minLength": 16
			}
			""";

		mockMvc.perform(put("/api/password-policies/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.policyId").value(1));
	}

	@Test
	void testActivatePolicy_Success() throws Exception {
		when(passwordPolicyService.activatePolicy(eq(1), any())).thenReturn(samplePolicy);

		mockMvc.perform(post("/api/password-policies/1/activate"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isActive").value(true));
	}

	@Test
	void testDeletePolicy_Success() throws Exception {
		doNothing().when(passwordPolicyService).deletePolicy(2);

		mockMvc.perform(delete("/api/password-policies/2"))
			.andExpect(status().isNoContent());
	}

	@Test
	void testDeletePolicy_Active_ReturnsBadRequest() throws Exception {
		doThrow(new IllegalArgumentException("Cannot delete active password policy"))
			.when(passwordPolicyService).deletePolicy(1);

		mockMvc.perform(delete("/api/password-policies/1"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testDeletePolicyByCode_Success() throws Exception {
		doNothing().when(passwordPolicyService).deletePolicyByCode("INACTIVE_CODE");

		mockMvc.perform(delete("/api/password-policies/code/INACTIVE_CODE"))
			.andExpect(status().isNoContent());
	}

	@Test
	void testDeletePolicyByCode_Active_ReturnsBadRequest() throws Exception {
		doThrow(new IllegalArgumentException("Cannot delete active password policy. Only inactive password policies can be deleted."))
			.when(passwordPolicyService).deletePolicyByCode("ACTIVE_CODE");

		mockMvc.perform(delete("/api/password-policies/code/ACTIVE_CODE"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testValidatePassword_Success() throws Exception {
		when(passwordPolicyService.validatePassword(any(), any()))
			.thenReturn(PasswordValidationResult.success());

		String json = """
			{
				"password": "SecurePassword123!",
				"username": "user1"
			}
			""";

		mockMvc.perform(post("/api/password-policies/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.violations").isEmpty());
	}

	@Test
	void testValidatePassword_Failure() throws Exception {
		when(passwordPolicyService.validatePassword(any(), any()))
			.thenReturn(PasswordValidationResult.failure(List.of("Password length must be at least 15 characters")));

		String json = """
			{
				"password": "short"
			}
			""";

		mockMvc.perform(post("/api/password-policies/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.violations[0]").value("Password length must be at least 15 characters"));
	}
}
