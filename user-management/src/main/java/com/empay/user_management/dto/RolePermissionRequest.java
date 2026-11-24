package com.empay.user_management.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request payload to assign permissions to an application role.
 *
 * @param roleCode        Target application role code.
 * @param permissionCodes List of permission codes to assign to the role.
 * @param grantedBy       Optional identifier of the user granting the permissions.
 */
public record RolePermissionRequest(
	@NotBlank(message = "Role code is mandatory")
	String roleCode,

	@NotEmpty(message = "Permission codes must not be empty")
	List<String> permissionCodes,

	String grantedBy
) {}
