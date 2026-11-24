package com.empay.user_management.dto;

/**
 * Context container holding user information for identity and similarity checks.
 *
 * @param username username of the user
 * @param email email address of the user
 * @param fullName full name of the user
 */
public record UserContext(
	String username,
	String email,
	String fullName
) {
	/**
	 * Creates an empty user context.
	 *
	 * @return empty user context instance
	 */
	public static UserContext empty() {
		return new UserContext(null, null, null);
	}

	/**
	 * Creates a user context with the specified username.
	 *
	 * @param username username of the user
	 * @return user context with username populated
	 */
	public static UserContext of(String username) {
		return new UserContext(username, null, null);
	}
}
