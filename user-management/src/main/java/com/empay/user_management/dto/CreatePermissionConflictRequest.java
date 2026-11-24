package com.empay.user_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload to configure a permission-level Segregation of Duties conflict.
 * Supports standalone conflicts (derivedFromRoleConflict = null) or conflicts derived from a role conflict.
 */
public record CreatePermissionConflictRequest(
	@NotBlank(message = "Permission code A is mandatory")
	@Size(max = 60, message = "Permission code A must not exceed 60 characters")
	String permissionCodeA,

	@NotBlank(message = "Permission code B is mandatory")
	@Size(max = 60, message = "Permission code B must not exceed 60 characters")
	String permissionCodeB,

	@NotBlank(message = "Severity is mandatory")
	@Pattern(regexp = "^(BLOCK|WARN)$", message = "Severity must be either BLOCK or WARN")
	String severity,

	@NotBlank(message = "Rationale is mandatory")
	@Size(max = 400, message = "Rationale must not exceed 400 characters")
	String rationale,

	Long derivedFromRoleConflict
) {
}
