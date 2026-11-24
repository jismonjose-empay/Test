package com.empay.user_management.dto;

import java.util.List;

/**
 * Top-level response for dynamic user navigation menu hierarchy.
 *
 * @param userId unique user identifier
 * @param displayName human-readable employee display name
 * @param roles list of active role codes assigned to user
 * @param menus list of accessible top-level module menus
 */
public record UserNavigationResponse(
	String userId,
	String displayName,
	List<String> roles,
	List<ModuleMenuNode> menus
) {
	/**
	 * Canonical constructor with defensive null-safety.
	 */
	public UserNavigationResponse {
		roles = (roles != null) ? List.copyOf(roles) : List.of();
		menus = (menus != null) ? List.copyOf(menus) : List.of();
	}
}

