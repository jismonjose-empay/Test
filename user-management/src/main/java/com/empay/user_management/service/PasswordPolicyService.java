package com.empay.user_management.service;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import com.empay.user_management.dto.CreatePasswordPolicyRequest;
import com.empay.user_management.dto.PasswordPoliciesOverviewResponse;
import com.empay.user_management.dto.PasswordPolicyResponse;
import com.empay.user_management.dto.PasswordValidationResult;
import com.empay.user_management.dto.UpdatePasswordPolicyRequest;
import com.empay.user_management.dto.UserContext;

/**
 * Service interface for managing password policies and validating passwords.
 */
public interface PasswordPolicyService {

	/**
	 * Retrieves an overview containing summary list of all policies and details of the active policy.
	 *
	 * @return combined overview response
	 */
	PasswordPoliciesOverviewResponse getPoliciesOverview();

	/**
	 * Retrieves all defined password policies ordered by identifier.
	 *
	 * @return list of password policy responses
	 */
	List<PasswordPolicyResponse> getAllPolicies();

	/**
	 * Retrieves a password policy by its numerical identifier.
	 *
	 * @param id unique policy identifier
	 * @return policy response
	 */
	PasswordPolicyResponse getPolicyById(Integer id);

	/**
	 * Retrieves a password policy by its alphanumeric code.
	 *
	 * @param code unique policy code
	 * @return policy response
	 */
	PasswordPolicyResponse getPolicyByCode(String code);

	/**
	 * Retrieves the currently active password policy enforced by the system.
	 *
	 * @return active password policy response
	 */
	PasswordPolicyResponse getActivePolicy();

	/**
	 * Creates a new password policy.
	 *
	 * @param request payload containing policy definitions
	 * @param httpRequest HTTP request for actor identification
	 * @return created policy response
	 */
	PasswordPolicyResponse createPolicy(CreatePasswordPolicyRequest request, HttpServletRequest httpRequest);

	/**
	 * Updates properties of an existing password policy.
	 *
	 * @param id identifier of the policy to update
	 * @param request payload containing updated properties
	 * @param httpRequest HTTP request for actor identification
	 * @return updated policy response
	 */
	PasswordPolicyResponse updatePolicy(Integer id, UpdatePasswordPolicyRequest request, HttpServletRequest httpRequest);

	/**
	 * Activates a specified password policy and deactivates any previously active policy.
	 *
	 * @param id identifier of the policy to activate
	 * @param httpRequest HTTP request for actor identification
	 * @return activated policy response
	 */
	PasswordPolicyResponse activatePolicy(Integer id, HttpServletRequest httpRequest);

	/**
	 * Deletes an inactive password policy by its identifier.
	 * Active policies cannot be deleted.
	 *
	 * @param id identifier of the policy to delete
	 */
	void deletePolicy(Integer id);

	/**
	 * Deletes an inactive password policy by its alphanumeric code.
	 * Active policies cannot be deleted.
	 *
	 * @param code unique alphanumeric policy code
	 */
	void deletePolicyByCode(String code);

	/**
	 * Evaluates a candidate password against the active policy and user context.
	 *
	 * @param rawPassword the candidate password in plaintext
	 * @param userContext contextual user identity information for similarity checks
	 * @return validation result containing boolean flag and any violation descriptions
	 */
	PasswordValidationResult validatePassword(String rawPassword, UserContext userContext);

	/**
	 * Evaluates a candidate password against the active policy without user context.
	 *
	 * @param rawPassword the candidate password in plaintext
	 * @return validation result containing boolean flag and any violation descriptions
	 */
	PasswordValidationResult validatePassword(String rawPassword);
}
