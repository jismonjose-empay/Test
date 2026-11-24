package com.empay.user_management.dto;

import java.util.List;

/**
 * Data transfer object representing an application role with its assigned permission codes.
 * Solves the N+1 single-role fetching problem for UI permission matrices and role management.
 *
 * @param roleCode        Unique role code (e.g. CHECKER, MAKER, SYS_ADMIN).
 * @param roleName        Role display name.
 * @param description     Role description.
 * @param isSystem        Flag indicating if the role is a protected system role.
 * @param permissionCodes List of permission codes assigned to this role.
 */
public record RolePermissionsResponse(
	String roleCode,
	String roleName,
	String description,
	boolean isSystem,
	List<String> permissionCodes
) {
	public RolePermissionsResponse {
		if (permissionCodes == null) {
			permissionCodes = List.of();
		}
	}
}
