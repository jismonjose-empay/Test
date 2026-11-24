package com.empay.user_management.dto;

import java.util.List;

/**
 * Result record from evaluating a candidate password against the active password policy.
 *
 * @param valid true if the password satisfies all active policy rules
 * @param violations list of human-readable rule violation descriptions
 */
public record PasswordValidationResult(
	boolean valid,
	List<String> violations
) {
	/**
	 * Creates a successful validation result.
	 *
	 * @return successful validation result
	 */
	public static PasswordValidationResult success() {
		return new PasswordValidationResult(true, List.of());
	}

	/**
	 * Creates a failed validation result with the given violation messages.
	 *
	 * @param violations list of breached rule descriptions
	 * @return failed validation result
	 */
	public static PasswordValidationResult failure(List<String> violations) {
		return new PasswordValidationResult(false, violations != null ? List.copyOf(violations) : List.of());
	}
}
