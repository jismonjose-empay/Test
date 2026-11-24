package com.empay.user_management.dto;

import java.time.LocalDateTime;

import com.empay.user_management.entities.RolePermissionHistoryEntity;

/**
 * Data transfer object representing a role permission history entry.
 *
 * @param historyId unique identifier of the history record
 * @param roleCode unique code of the application role
 * @param permissionCode code of the affected permission
 * @param actionTaken action applied to the mapping, such as ADDED or REMOVED
 * @param changedBy identifier of the user who executed the action
 * @param changedAt timestamp when the action occurred
 */
public record RolePermissionHistoryResponse(
		Long historyId,
		String roleCode,
		String permissionCode,
		String actionTaken,
		String changedBy,
		LocalDateTime changedAt
) {

	/**
	 * Creates a role permission history response from a persistent entity.
	 *
	 * @param entity persistent role permission history entity
	 * @return mapped history response DTO, or null if entity is null
	 */
	public static RolePermissionHistoryResponse fromEntity(RolePermissionHistoryEntity entity) {
		if (entity == null) {
			return null;
		}
		return new RolePermissionHistoryResponse(
				entity.getHistoryId(),
				entity.getRoleCode(),
				entity.getPermissionCode(),
				entity.getActionTaken(),
				entity.getChangedBy(),
				entity.getChangedAt()
		);
	}
}
