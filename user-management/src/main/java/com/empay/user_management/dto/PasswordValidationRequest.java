package com.empay.user_management.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for validating a candidate password against the active policy.
 *
 * @param password candidate password to validate
 * @param username optional username for similarity verification
 * @param email optional email for similarity verification
 * @param fullName optional full name for similarity verification
 */
public record PasswordValidationRequest(
	@NotBlank(message = "Password must not be blank")
	String password,
	String username,
	String email,
	String fullName
) {
	/**
	 * Converts the request into a UserContext.
	 *
	 * @return user context instance
	 */
	public UserContext toUserContext() {
		return new UserContext(username, email, fullName);
	}
}
