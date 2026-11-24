package com.empay.user_management.dto;

import java.util.List;

/**
 * Data transfer object representing the result of a user password change operation.
 *
 * @param success true if the password changed successfully, false otherwise
 * @param message status or descriptive error message
 * @param violations collection of validation rule violation descriptions
 */
public record ChangePasswordResponse(
		boolean success,
		String message,
		List<String> violations
) {

	/**
	 * Compact constructor enforcing null safety and unmodifiable violation list.
	 */
	public ChangePasswordResponse {
		violations = violations != null ? List.copyOf(violations) : List.of();
	}

	/**
	 * Creates a change password response without validation violations.
	 *
	 * @param success true if the operation succeeded
	 * @param message descriptive status message
	 */
	public ChangePasswordResponse(boolean success, String message) {
		this(success, message, List.of());
	}

	/**
	 * Creates a successful password change response.
	 *
	 * @param message success description message
	 * @return successful change password response
	 */
	public static ChangePasswordResponse ofSuccess(String message) {
		return new ChangePasswordResponse(true, message, List.of());
	}

	/**
	 * Creates a failed password change response with policy violations.
	 *
	 * @param message failure description message
	 * @param violations list of policy violation details
	 * @return failed change password response
	 */
	public static ChangePasswordResponse ofFailure(String message, List<String> violations) {
		return new ChangePasswordResponse(false, message, violations);
	}

	/**
	 * Creates a failed password change response without violations.
	 *
	 * @param message failure description message
	 * @return failed change password response
	 */
	public static ChangePasswordResponse ofFailure(String message) {
		return new ChangePasswordResponse(false, message, List.of());
	}

	/**
	 * Returns true if the operation was successful.
	 *
	 * @return true if successful
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Returns the operation status or failure message.
	 *
	 * @return status message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the unmodifiable list of validation violations.
	 *
	 * @return list of violations
	 */
	public List<String> getViolations() {
		return violations;
	}
}
