package com.empay.user_management.service;

import com.empay.user_management.dto.UserNavigationResponse;

/**
 * Service interface for generating dynamic, permission-driven navigation trees.
 */
public interface UserNavigationService {

	/**
	 * Retrieves dynamic navigation menu hierarchy for the current user.
	 *
	 * @param tokenOrUserId authorization token or user identifier
	 * @return structured navigation menu hierarchy
	 */
	UserNavigationResponse getUserNavigationMenu(String tokenOrUserId);

	/**
	 * Retrieves dynamic navigation menu hierarchy specifically for a given role (e.g. SYS_ADMIN).
	 *
	 * @param userId unique user identifier
	 * @param roleCode role code
	 * @return structured navigation menu hierarchy
	 */
	UserNavigationResponse getNavigationForRole(String userId, String roleCode);
}

