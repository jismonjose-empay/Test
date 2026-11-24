package com.empay.user_management.dto;

import com.empay.user_management.entities.PermissionConflictEntity;
import com.empay.user_management.entities.RoleConflictEntity;

/**
 * Details of a Segregation of Duties conflict between roles or permissions.
 * Identifies the conflicting pair, severity, and business rationale.
 *
 * @param conflictType              Type of conflict (ROLE_CONFLICT or PERMISSION_CONFLICT).
 * @param codeA                     First conflicting code in the pair.
 * @param codeB                     Second conflicting code in the pair.
 * @param severity                  Conflict severity level (BLOCK or WARN).
 * @param rationale                 Business reason for the segregation restriction.
 * @param derivedFromRoleConflictId Identifier of parent role conflict when derived.
 * @param derivedRoleConflict       Summary of parent role conflict when derived.
 */
public record SodConflictDetail(
	String conflictType,
	String codeA,
	String codeB,
	String severity,
	String rationale,
	Long derivedFromRoleConflictId,
	String derivedRoleConflict
) implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	public static SodConflictDetail fromPermissionConflict(PermissionConflictEntity entity) {
		String derivedRoleConflictStr = null;
		if (entity.getDerivedRoleConflict() != null) {
			derivedRoleConflictStr = entity.getDerivedRoleConflict().getRoleCodeA()
				+ " vs "
				+ entity.getDerivedRoleConflict().getRoleCodeB();
		}
		return new SodConflictDetail(
			"PERMISSION_CONFLICT",
			entity.getPermissionCodeA(),
			entity.getPermissionCodeB(),
			entity.getSeverity(),
			entity.getRationale(),
			entity.getDerivedFromRoleConflict(),
			derivedRoleConflictStr
		);
	}

	public static SodConflictDetail fromRoleConflict(RoleConflictEntity entity) {
		return new SodConflictDetail(
			"ROLE_CONFLICT",
			entity.getRoleCodeA(),
			entity.getRoleCodeB(),
			entity.getSeverity(),
			entity.getRationale(),
			entity.getConflictId(),
			entity.getRoleCodeA() + " vs " + entity.getRoleCodeB()
		);
	}
}
