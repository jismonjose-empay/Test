package com.empay.user_management.dto;

import java.util.List;

/**
 * Combined response payload containing all application roles and all system permissions.
 * Used during user interface initialization and permission matrix screens.
 *
 * @param roles       List of all application roles.
 * @param permissions List of all available system permissions.
 */
public record RolesAndPermissionsResponse(
	List<RoleResponse> roles,
	List<PermissionResponse> permissions
) {}
