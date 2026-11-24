package com.empay.user_management.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload to create a new application role.
 */
public record CreateRoleRequest(
	@NotBlank(message = "Role code is mandatory")
	@Size(max = 40, message = "Role code must not exceed 40 characters")
	@Pattern(regexp = "^[A-Z0-9_]+$", message = "Role code must contain only uppercase letters, numbers, and underscores")
	String roleCode,

	String name,

	@Size(max = 300, message = "Description must not exceed 300 characters")
	String description,

	List<String> permissionCodes,

	List<String> permissions,

	String roleName
) {
	public CreateRoleRequest(String roleCode, String name, String description, List<String> permissionCodes) {
		this(roleCode, name, description, permissionCodes, null, null);
	}

	public List<String> effectivePermissionCodes() {
		if (permissionCodes != null && !permissionCodes.isEmpty()) {
			return permissionCodes;
		}
		if (permissions != null && !permissions.isEmpty()) {
			return permissions;
		}
		return List.of();
	}

	public String effectiveName() {
		if (name != null && !name.isBlank()) {
			return name;
		}
		if (roleName != null && !roleName.isBlank()) {
			return roleName;
		}
		return "";
	}
}

