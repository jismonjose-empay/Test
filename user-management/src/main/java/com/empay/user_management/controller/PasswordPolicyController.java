package com.empay.user_management.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.dto.CreatePasswordPolicyRequest;
import com.empay.user_management.dto.PasswordPoliciesOverviewResponse;
import com.empay.user_management.dto.PasswordPolicyResponse;
import com.empay.user_management.dto.PasswordValidationRequest;
import com.empay.user_management.dto.PasswordValidationResult;
import com.empay.user_management.dto.UpdatePasswordPolicyRequest;
import com.empay.user_management.service.PasswordPolicyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Controller providing APIs for PASSWORD_POLICY catalogue and password validation.
 */
@RestController
@RequestMapping("/api/password-policies")
public class PasswordPolicyController {

	private final PasswordPolicyService passwordPolicyService;

	public PasswordPolicyController(PasswordPolicyService passwordPolicyService) {
		this.passwordPolicyService = passwordPolicyService;
	}

	/**
	 * Retrieves an overview containing summary list of all policies and details of the active policy.
	 *
	 * @return overview containing policies summary list and active policy details
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.VIEW')")
	@GetMapping
	public ResponseEntity<PasswordPoliciesOverviewResponse> getPoliciesOverview() {
		return ResponseEntity.ok(passwordPolicyService.getPoliciesOverview());
	}

	/**
	 * Alternative path for retrieving policies overview and active policy details.
	 *
	 * @return overview containing policies summary list and active policy details
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.VIEW')")
	@GetMapping("/overview")
	public ResponseEntity<PasswordPoliciesOverviewResponse> getPoliciesOverviewExplicit() {
		return ResponseEntity.ok(passwordPolicyService.getPoliciesOverview());
	}

	/**
	 * Retrieves all defined password policies in full detail.
	 *
	 * @return list of password policy responses
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.VIEW')")
	@GetMapping("/all")
	public ResponseEntity<List<PasswordPolicyResponse>> getAllPolicies() {
		return ResponseEntity.ok(passwordPolicyService.getAllPolicies());
	}

	/**
	 * Retrieves the currently active password policy.
	 *
	 * @return active password policy response
	 */
    @PreAuthorize("permitAll()")
	@GetMapping("/active")
	public ResponseEntity<PasswordPolicyResponse> getActivePolicy() {
		return ResponseEntity.ok(passwordPolicyService.getActivePolicy());
	}

	/**
	 * Retrieves a password policy by its numerical identifier.
	 *
	 * @param id unique policy identifier
	 * @return password policy response
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.VIEW')")
	@GetMapping("/{id}")
	public ResponseEntity<PasswordPolicyResponse> getPolicyById(@PathVariable Integer id) {
		return ResponseEntity.ok(passwordPolicyService.getPolicyById(id));
	}

	/**
	 * Retrieves a password policy by its unique alphanumeric code.
	 *
	 * @param code unique alphanumeric policy code
	 * @return password policy response
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.VIEW')")
	@GetMapping("/code/{code}")
	public ResponseEntity<PasswordPolicyResponse> getPolicyByCode(@PathVariable String code) {
		return ResponseEntity.ok(passwordPolicyService.getPolicyByCode(code));
	}

	/**
	 * Creates a new password policy.
	 *
	 * @param request payload containing policy definitions
	 * @param httpRequest HTTP request context
	 * @return created policy response
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.CREATE')")
	@PostMapping
	public ResponseEntity<PasswordPolicyResponse> createPolicy(
			@RequestBody @Valid CreatePasswordPolicyRequest request,
			HttpServletRequest httpRequest) {
		PasswordPolicyResponse response = passwordPolicyService.createPolicy(request, httpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Updates an existing password policy.
	 *
	 * @param id identifier of the policy to update
	 * @param request payload containing updated properties
	 * @param httpRequest HTTP request context
	 * @return updated policy response
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.EDIT')")
	@PutMapping("/{id}")
	public ResponseEntity<PasswordPolicyResponse> updatePolicy(
			@PathVariable Integer id,
			@Valid @RequestBody UpdatePasswordPolicyRequest request,
			HttpServletRequest httpRequest) {
		return ResponseEntity.ok(passwordPolicyService.updatePolicy(id, request, httpRequest));
	}

	/**
	 * Activates a specified password policy and deactivates any previously active policy.
	 *
	 * @param id identifier of the policy to activate
	 * @param httpRequest HTTP request context
	 * @return activated policy response
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.ACTIVATE')")
	@PostMapping("/{id}/activate")
	public ResponseEntity<PasswordPolicyResponse> activatePolicy(
			@PathVariable Integer id,
			HttpServletRequest httpRequest) {
		return ResponseEntity.ok(passwordPolicyService.activatePolicy(id, httpRequest));
	}

	/**
	 * Deletes an inactive password policy by its identifier.
	 * Active policies cannot be deleted.
	 *
	 * @param id identifier of the policy to delete
	 * @return empty response with status 204
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.DELETE')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePolicy(@PathVariable Integer id) {
		passwordPolicyService.deletePolicy(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Deletes an inactive password policy by its alphanumeric code.
	 * Active policies cannot be deleted.
	 *
	 * @param code unique alphanumeric policy code
	 * @return empty response with status 204
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_PASSWORD_POLICY.DELETE')")
	@DeleteMapping("/code/{code}")
	public ResponseEntity<Void> deletePolicyByCode(@PathVariable String code) {
		passwordPolicyService.deletePolicyByCode(code);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Validates a candidate password against the active policy.
	 *
	 * @param request payload containing the candidate password and optional user context
	 * @return validation result
	 */
	@PreAuthorize("permitAll()")
	@PostMapping("/validate")
	public ResponseEntity<PasswordValidationResult> validatePassword(
			@Valid @RequestBody PasswordValidationRequest request) {
		PasswordValidationResult result = passwordPolicyService.validatePassword(
			request.password(),
			request.toUserContext()
		);
		return ResponseEntity.ok(result);
	}
}
