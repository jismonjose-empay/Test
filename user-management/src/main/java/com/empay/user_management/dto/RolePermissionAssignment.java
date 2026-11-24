package com.empay.user_management.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents an application role and its target permission codes.
 * Used during matrix synchronization to set the complete permission state.
 *
 * @param roleCode        Target application role code.
 * @param permissionCodes Complete list of permission codes for the role.
 */
public record RolePermissionAssignment(
	@NotBlank(message = "Role code is mandatory")
	String roleCode,

	List<String> permissionCodes
) {
}
