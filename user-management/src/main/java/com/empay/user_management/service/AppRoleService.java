package com.empay.user_management.service;

import java.util.List;

import com.empay.user_management.dto.BulkRolePermissionSummary;
import com.empay.user_management.dto.BulkRolePermissionsRequest;
import com.empay.user_management.dto.ConflictCheckRequest;
import com.empay.user_management.dto.ConflictCheckResponse;
import com.empay.user_management.dto.CreatePermissionConflictRequest;
import com.empay.user_management.dto.CreateRoleRequest;
import com.empay.user_management.dto.PermissionResponse;
import com.empay.user_management.dto.RolePermissionHistoryPageResponse;
import com.empay.user_management.dto.RolePermissionRequest;
import com.empay.user_management.dto.RolePermissionsResponse;
import com.empay.user_management.dto.RoleResponse;
import com.empay.user_management.dto.RolesAndPermissionsResponse;
import com.empay.user_management.dto.SodConflictDetail;
import com.empay.user_management.dto.UpdateRoleRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface AppRoleService {

	List<RoleResponse> getAllRoles();

	RoleResponse getRoleByCode(String roleCode);

	List<PermissionResponse> getPermissionsForRole(String roleCode);

	List<RolePermissionsResponse> getBulkRolePermissions(List<String> roleCodes);

	RolesAndPermissionsResponse getRolesAndPermissions();

	RoleResponse createRole(CreateRoleRequest request, HttpServletRequest httpRequest);

	RoleResponse updateRole(String roleCode, UpdateRoleRequest request, HttpServletRequest httpRequest);

	List<PermissionResponse> assignPermissionsToRole(RolePermissionRequest request, HttpServletRequest httpRequest);

	List<PermissionResponse> updateRolePermissions(String roleCode, List<String> permissionCodes, HttpServletRequest httpRequest);

	BulkRolePermissionSummary bulkConfigureRolePermissions(BulkRolePermissionsRequest request, HttpServletRequest httpRequest);

	ConflictCheckResponse checkConflicts(ConflictCheckRequest request);

	List<SodConflictDetail> getRoleConflicts();

	List<SodConflictDetail> getPermissionConflicts();

	SodConflictDetail createPermissionConflict(CreatePermissionConflictRequest request);

	void deleteRole(String roleCode);

	void revokePermissionFromRole(String roleCode, String permissionCode);

	void revokePermissionFromRole(String roleCode, String permissionCode, HttpServletRequest httpRequest);

	void revokePermissionsFromRole(String roleCode, List<String> permissionCodes);

	void revokePermissionsFromRole(String roleCode, List<String> permissionCodes, HttpServletRequest httpRequest);

	/**
	 * Retrieves paginated audit history records for a specific application role.
	 *
	 * @param roleCode unique code of the role
	 * @param permissionCode optional filter for permission code
	 * @param actionTaken optional filter for action taken, such as ADDED or REMOVED
	 * @param page one-based page number
	 * @param pageSize number of records per page
	 * @return paginated history records matching criteria
	 */
	RolePermissionHistoryPageResponse getRolePermissionHistory(
			String roleCode,
			String permissionCode,
			String actionTaken,
			int page,
			int pageSize);

	/**
	 * Retrieves paginated audit history records across all application roles.
	 *
	 * @param roleCode optional filter for role code
	 * @param permissionCode optional filter for permission code
	 * @param actionTaken optional filter for action taken, such as ADDED or REMOVED
	 * @param page one-based page number
	 * @param pageSize number of records per page
	 * @return paginated history records matching criteria
	 */
	RolePermissionHistoryPageResponse getAllRolePermissionHistory(
			String roleCode,
			String permissionCode,
			String actionTaken,
			int page,
			int pageSize);
}
