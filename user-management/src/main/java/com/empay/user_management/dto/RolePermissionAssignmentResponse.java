package com.empay.user_management.dto;

import java.time.LocalDateTime;

import com.empay.user_management.entities.RolePermissionEntity;

/**
 * Detailed response for a role permission mapping.
 * Includes permission attributes and audit metadata.
 *
 * @param roleCode       Application role code.
 * @param permissionCode Permission code.
 * @param module         Functional module of the permission.
 * @param resource       Resource name.
 * @param actionName     Action name.
 * @param description    Permission description.
 * @param isSensitive    Flag indicating if the permission is sensitive.
 * @param grantedBy      User who granted the permission.
 * @param grantedAt      Timestamp when the permission was granted.
 */
public record RolePermissionAssignmentResponse(
	String roleCode,
	String permissionCode,
	String module,
	String resource,
	String actionName,
	String description,
	boolean isSensitive,
	String grantedBy,
	LocalDateTime grantedAt
) {

	public static RolePermissionAssignmentResponse fromEntity(RolePermissionEntity entity) {
		if (entity == null) {
			return null;
		}
		var perm = entity.getPermission();
		return new RolePermissionAssignmentResponse(
			entity.getId().getRoleCode(),
			entity.getId().getPermissionCode(),
			perm != null ? perm.getModule() : null,
			perm != null ? perm.getResource() : null,
			perm != null ? perm.getActionName() : null,
			perm != null ? perm.getDescription() : null,
			perm != null && perm.isSensitivePermission(),
			entity.getGrantedBy(),
			entity.getGrantedAt()
		);
	}
}
