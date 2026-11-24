package com.empay.user_management.dto;

import java.util.List;

/**
 * Level 2 and 3 navigation node representing a submenu screen or collapsible folder.
 * Supports recursive nested children for 3-tier menu trees.
 *
 * @param resource unique resource identifier
 * @param title display title for the submenu or screen
 * @param routePath frontend route path (null for parent folder groups)
 * @param iconName icon identifier for the submenu
 * @param legacyRecordId legacy menu order (UIL_SER_NUMBER) for sorting
 * @param actions permitted in-page action capabilities (VIEW, CREATE, UPDATE, etc.)
 * @param children nested sub-submenus (Level 3 leaf screens)
 */
public record ResourceSubmenuNode(
	String resource,
	String title,
	String routePath,
	String iconName,
	Integer legacyRecordId,
	List<String> actions,
	List<ResourceSubmenuNode> children
) {
	/**
	 * Canonical constructor with defensive null-safety.
	 */
	public ResourceSubmenuNode {
		actions = (actions != null) ? List.copyOf(actions) : List.of();
		children = (children != null) ? List.copyOf(children) : List.of();
	}

	/**
	 * Convenience constructor for leaf submenu nodes without children.
	 *
	 * @param resource unique resource identifier
	 * @param title display title for the submenu or screen
	 * @param routePath frontend route path
	 * @param iconName icon identifier for the submenu
	 * @param legacyRecordId legacy menu order for sorting
	 * @param actions permitted in-page action capabilities
	 */
	public ResourceSubmenuNode(
			String resource,
			String title,
			String routePath,
			String iconName,
			Integer legacyRecordId,
			List<String> actions) {
		this(resource, title, routePath, iconName, legacyRecordId, actions, List.of());
	}
}

