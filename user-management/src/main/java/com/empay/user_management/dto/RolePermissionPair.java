package com.empay.user_management.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents an explicit mapping between an application role and a permission.
 * Used for targeted grant and revoke operations.
 *
 * @param roleCode       Application role code.
 * @param permissionCode Permission code.
 */
public record RolePermissionPair(
	@NotBlank(message = "Role code is mandatory")
	String roleCode,

	@NotBlank(message = "Permission code is mandatory")
	String permissionCode
) {
}
