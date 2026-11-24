package com.empay.user_management.dto;

import java.util.List;

/**
 * Summary details for bulk role permission operations.
 * Contains operation metrics and updated role records.
 *
 * @param totalRolesProcessed     Number of distinct roles modified.
 * @param totalPermissionsGranted Number of permissions added.
 * @param totalPermissionsRevoked Number of permissions removed.
 * @param updatedRoles            List of modified roles with their current permissions.
 * @param message                 Human-readable operation summary.
 */
public record BulkRolePermissionSummary(
	int totalRolesProcessed,
	int totalPermissionsGranted,
	int totalPermissionsRevoked,
	List<RoleResponse> updatedRoles,
	String message
) {
}
