package com.empay.user_management.dto;

import java.util.List;

import jakarta.validation.Valid;

/**
 * Data transfer object for bulk role permission configuration.
 * Supports full matrix state replacement and incremental delta actions.
 *
 * @param action          Action type for delta execution (GRANT, REVOKE, or SYNC).
 * @param roleCode        Target application role code for single-role actions.
 * @param permissionCodes List of permission codes to grant or revoke for the role.
 * @param items           List of role-permission pairs for multi-role operations.
 * @param roles           List of roles with complete permission sets for matrix synchronization.
 * @param grants          Explicit list of role-permission pairs to insert.
 * @param revocations     Explicit list of role-permission pairs to delete.
 */
public record BulkRolePermissionsRequest(
	String action,
	String roleCode,
	List<String> permissionCodes,
	List<@Valid RolePermissionPair> items,
	List<@Valid RolePermissionAssignment> roles,
	List<@Valid RolePermissionPair> grants,
	List<@Valid RolePermissionPair> revocations
) {
	public BulkRolePermissionsRequest(
			List<@Valid RolePermissionAssignment> roles,
			List<@Valid RolePermissionPair> grants,
			List<@Valid RolePermissionPair> revocations) {
		this(null, null, null, null, roles, grants, revocations);
	}

	public static BulkRolePermissionsRequest ofAction(String action, String roleCode, List<String> permissionCodes) {
		return new BulkRolePermissionsRequest(action, roleCode, permissionCodes, null, null, null, null);
	}

	public static BulkRolePermissionsRequest ofAction(String action, List<@Valid RolePermissionPair> items) {
		return new BulkRolePermissionsRequest(action, null, null, items, null, null, null);
	}

	public static BulkRolePermissionsRequest ofRoles(List<@Valid RolePermissionAssignment> roles) {
		return new BulkRolePermissionsRequest(null, null, null, null, roles, null, null);
	}

	public boolean hasAction() {
		return action != null && !action.isBlank();
	}

	public boolean hasSingleRoleAction() {
		return roleCode != null && !roleCode.isBlank() && permissionCodes != null && !permissionCodes.isEmpty();
	}

	public boolean hasItems() {
		return items != null && !items.isEmpty();
	}

	public boolean hasRoles() {
		return roles != null && !roles.isEmpty();
	}

	public boolean hasGrants() {
		return grants != null && !grants.isEmpty();
	}

	public boolean hasRevocations() {
		return revocations != null && !revocations.isEmpty();
	}

	public boolean isEmpty() {
		return !hasAction() && !hasSingleRoleAction() && !hasItems() && !hasRoles() && !hasGrants() && !hasRevocations();
	}
}

