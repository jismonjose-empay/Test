package com.empay.user_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to update application role details.
 */
public record UpdateRoleRequest(
	@NotBlank(message = "Role name is mandatory")
	@Size(max = 100, message = "Role name must not exceed 100 characters")
	String name,

	@Size(max = 300, message = "Description must not exceed 300 characters")
	String description
) {
}
