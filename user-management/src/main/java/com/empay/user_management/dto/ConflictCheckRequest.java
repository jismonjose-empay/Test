package com.empay.user_management.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request payload to check if a role and its permission set trigger SoD conflicts.
 */
public record ConflictCheckRequest(
	@NotBlank(message = "Role code is mandatory")
	String roleCode,

	@NotEmpty(message = "Permission codes must not be empty")
	List<String> permissionCodes
) {
}
