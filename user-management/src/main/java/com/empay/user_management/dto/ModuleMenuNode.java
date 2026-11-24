package com.empay.user_management.dto;

import java.util.List;

/**
 * Level 1 navigation node representing a top-level functional module (e.g., ADMIN, PAYMENT).
 *
 * @param module unique module identifier
 * @param title human-readable module title
 * @param iconName sidebar icon identifier
 * @param submenus list of level 2 submenus or screen groups
 */
public record ModuleMenuNode(
	String module,
	String title,
	String iconName,
	List<ResourceSubmenuNode> submenus
) {
	/**
	 * Canonical constructor with defensive null-safety.
	 */
	public ModuleMenuNode {
		submenus = (submenus != null) ? List.copyOf(submenus) : List.of();
	}
}

