package com.empay.user_management.dto;

import java.time.LocalDateTime;

import com.empay.user_management.entities.AppRoleEntity;

/**
 * Data transfer object representing an application role.
 * Maps role details from the database entity to client responses.
 *
 * @param roleCode    Unique role code.
 * @param name        Display name of the role.
 * @param description Detailed description of role responsibilities.
 * @param isSystem    Flag indicating if the role is a protected system role.
 * @param createdBy   Identifier of the user who created the role.
 * @param createdAt   Timestamp when the role was created.
 */
public record RoleResponse(
	String roleCode,
	String name,
	String description,
	boolean isSystem,
	String createdBy,
	LocalDateTime createdAt
) {

	public static RoleResponse fromEntity(AppRoleEntity entity) {
		if (entity == null) {
			return null;
		}
		return new RoleResponse(
			entity.getRoleCode(),
			entity.getName(),
			entity.getDescription(),
			entity.isSystemRole(),
			entity.getCreatedBy(),
			entity.getCreatedAt()
		);
	}
}
