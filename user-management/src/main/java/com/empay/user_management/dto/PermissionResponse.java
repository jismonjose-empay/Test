package com.empay.user_management.dto;

import com.empay.user_management.entities.PermissionEntity;

/**
 * Data transfer object representing a system permission.
 * Contains permission attributes and sensitivity classification.
 *
 * @param permissionCode Unique permission code.
 * @param module         Functional module of the permission.
 * @param resource       Target resource name.
 * @param actionName     Action allowed on the resource.
 * @param description    Functional description of the permission.
 * @param isSensitive    Flag indicating if the permission is sensitive.
 * @param legacyRecordId Legacy identifier for backward compatibility.
 */
public record PermissionResponse(
	String permissionCode,
	String module,
	String resource,
	String actionName,
	String description,
	boolean isSensitive,
	Integer legacyRecordId
) {

	public static PermissionResponse fromEntity(PermissionEntity entity) {
		if (entity == null) {
			return null;
		}
		return new PermissionResponse(
			entity.getPermissionCode(),
			entity.getModule(),
			entity.getResource(),
			entity.getActionName(),
			entity.getDescription(),
			entity.isSensitivePermission(),
			entity.getLegacyRecordId()
		);
	}
}
