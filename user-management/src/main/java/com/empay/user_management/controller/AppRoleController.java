package com.empay.user_management.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import com.empay.user_management.service.AppRoleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Controller providing APIs for APP_ROLE catalogue and ROLE_PERMISSION management.
 */
@RestController
@RequestMapping("/api/roles")
public class AppRoleController {

	private final AppRoleService appRoleService;

	public AppRoleController(AppRoleService appRoleService) {
		this.appRoleService = appRoleService;
	}

	/**
	 * Retrieve all configured application roles.
	 *
	 * @return List of role records.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW')")
	@GetMapping
	public ResponseEntity<List<RoleResponse>> getAllRoles() {
		return ResponseEntity.ok(appRoleService.getAllRoles());
	}

	/**
	 * Retrieve both all roles and all permissions in a single combined payload.
	 * Ideal for UI bootstrap and permission matrix screens.
	 *
	 * @return Combined roles and permissions record.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW')")
	@GetMapping("/roles-and-permissions")
	public ResponseEntity<RolesAndPermissionsResponse> getRolesAndPermissions() {
		return ResponseEntity.ok(appRoleService.getRolesAndPermissions());
	}

	/**
	 * Retrieve specific application role by role code.
	 *
	 * @param roleCode Role unique code (e.g., USER_ADMIN, MAKER).
	 * @return Role record.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW')")
	@GetMapping("/{roleCode}")
	public ResponseEntity<RoleResponse> getRoleByCode(@PathVariable String roleCode) {
		return ResponseEntity.ok(appRoleService.getRoleByCode(roleCode));
	}

	/**
	 * Create a new application role.
	 *
	 * @param request Role creation request.
	 * @param httpRequest HTTP servlet request for audit context.
	 * @return Created role record.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.CREATE')")
	@PostMapping
	public ResponseEntity<RoleResponse> createRole(
			@RequestBody @Valid CreateRoleRequest request,
			HttpServletRequest httpRequest) {
		RoleResponse created = appRoleService.createRole(request, httpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	/**
	 * Update application role details.
	 *
	 * @param roleCode Role unique code.
	 * @param request Role update request.
	 * @param httpRequest HTTP servlet request.
	 * @return Updated role record.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@PutMapping("/{roleCode}")
	public ResponseEntity<RoleResponse> updateRole(
			@PathVariable String roleCode,
			@RequestBody @Valid UpdateRoleRequest request,
			HttpServletRequest httpRequest) {
		RoleResponse updated = appRoleService.updateRole(roleCode, request, httpRequest);
		return ResponseEntity.ok(updated);
	}

	/**
	 * Evaluate potential Segregation of Duties conflicts for a role and permission set.
	 * Pre-flight check before saving role permissions.
	 *
	 * @param request Conflict check request.
	 * @return Conflict evaluation result.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@PostMapping("/check-conflicts")
	public ResponseEntity<ConflictCheckResponse> checkConflicts(
			@RequestBody @Valid ConflictCheckRequest request) {
		return ResponseEntity.ok(appRoleService.checkConflicts(request));
	}

	/**
	 * Retrieve all configured role-level conflicts (ROLE_CONFLICT).
	 *
	 * @return List of role conflict details.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
	@GetMapping("/conflicts")
	public ResponseEntity<List<SodConflictDetail>> getRoleConflicts() {
		return ResponseEntity.ok(appRoleService.getRoleConflicts());
	}

	/**
	 * Retrieve all configured permission-level conflicts (PERMISSION_CONFLICT).
	 *
	 * @return List of permission conflict details.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
	@GetMapping("/permission-conflicts")
	public ResponseEntity<List<SodConflictDetail>> getPermissionConflicts() {
		return ResponseEntity.ok(appRoleService.getPermissionConflicts());
	}

	/**
	 * Create a new permission-level Segregation of Duties conflict (PERMISSION_CONFLICT).
	 * Supports standalone conflicts (derivedFromRoleConflict = null) or conflicts derived from a role conflict.
	 *
	 * @param request Permission conflict creation payload.
	 * @return Created conflict detail.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.CREATE')")
	@PostMapping("/permission-conflicts")
	public ResponseEntity<SodConflictDetail> createPermissionConflict(
			@RequestBody @Valid CreatePermissionConflictRequest request) {
		SodConflictDetail created = appRoleService.createPermissionConflict(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	/**
	 * Retrieve all permissions assigned to a specific role.
	 *
	 * @param roleCode Role unique code.
	 * @return List of assigned permission records.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW')")
	@GetMapping("/{roleCode}/permissions")
	public ResponseEntity<List<PermissionResponse>> getPermissionsForRole(@PathVariable String roleCode) {
		return ResponseEntity.ok(appRoleService.getPermissionsForRole(roleCode));
	}

	/**
	 * Retrieve assigned permissions for multiple roles in bulk, or for all roles if none specified.
	 * Solves the N+1 single-role fetching problem for UI matrix and role management screens.
	 *
	 * @param roleCodes Optional list of role codes to filter (e.g., ?roleCodes=CHECKER,MAKER).
	 * @return List of role permission details for each requested role.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW')")
	@GetMapping({"/permissions/bulk", "/permissions"})
	public ResponseEntity<List<RolePermissionsResponse>> getBulkRolePermissions(
			@RequestParam(required = false) List<String> roleCodes) {
		List<RolePermissionsResponse> result = appRoleService.getBulkRolePermissions(roleCodes);
		return ResponseEntity.ok(result);
	}

	/**
	 * Assign one or more permissions to a role.
	 *
	 * @param request Role permission assignment payload.
	 * @param httpRequest HTTP servlet request for audit context.
	 * @return Updated list of permissions attached to the role.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@PostMapping("/permissions")
	public ResponseEntity<List<PermissionResponse>> assignPermissions(
			@RequestBody @Valid RolePermissionRequest request,
			HttpServletRequest httpRequest) {
		List<PermissionResponse> assigned = appRoleService.assignPermissionsToRole(request, httpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(assigned);
	}

	/**
	 * Assign one or more permissions to a specific role path.
	 *
	 * @param roleCode Role unique code.
	 * @param permissionCodes List of permission codes to attach.
	 * @param httpRequest HTTP servlet request.
	 * @return Updated list of permissions attached to the role.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@PostMapping("/{roleCode}/permissions")
	public ResponseEntity<List<PermissionResponse>> assignPermissionsToRolePath(
			@PathVariable String roleCode,
			@RequestBody List<String> permissionCodes,
			HttpServletRequest httpRequest) {
		var request = new RolePermissionRequest(roleCode, permissionCodes, null);
		List<PermissionResponse> assigned = appRoleService.assignPermissionsToRole(request, httpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(assigned);
	}

	/**
	 * Synchronize/Update the full set of permissions attached to a role.
	 * Matches the UI multi-select or checkbox matrix state.
	 *
	 * @param roleCode Role unique code.
	 * @param permissionCodes New complete list of permission codes for this role.
	 * @param httpRequest HTTP servlet request.
	 * @return Updated list of permissions attached to the role.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@PutMapping("/{roleCode}/permissions")
	public ResponseEntity<List<PermissionResponse>> updateRolePermissions(
			@PathVariable String roleCode,
			@RequestBody List<String> permissionCodes,
			HttpServletRequest httpRequest) {
		List<PermissionResponse> updated = appRoleService.updateRolePermissions(roleCode, permissionCodes, httpRequest);
		return ResponseEntity.ok(updated);
	}

	/**
	 * Bulk configure role permissions across multiple roles.
	 * Supports full matrix synchronization or targeted grants and revocations.
	 *
	 * @param request Bulk role permissions configuration payload.
	 * @param httpRequest HTTP servlet request for audit context.
	 * @return Summary of bulk operation metrics and updated roles.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@PutMapping("/permissions/bulk")
	public ResponseEntity<BulkRolePermissionSummary> bulkConfigureRolePermissionsPut(
			@RequestBody @Valid BulkRolePermissionsRequest request,
			HttpServletRequest httpRequest) {
		BulkRolePermissionSummary summary = appRoleService.bulkConfigureRolePermissions(request, httpRequest);
		return ResponseEntity.ok(summary);
	}

	/**
	 * Bulk configure role permissions across multiple roles (POST alias for clients).
	 *
	 * @param request Bulk role permissions configuration payload.
	 * @param httpRequest HTTP servlet request for audit context.
	 * @return Summary of bulk operation metrics and updated roles.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@PostMapping("/permissions/bulk")
	public ResponseEntity<BulkRolePermissionSummary> bulkConfigureRolePermissionsPost(
			@RequestBody @Valid BulkRolePermissionsRequest request,
			HttpServletRequest httpRequest) {
		return bulkConfigureRolePermissionsPut(request, httpRequest);
	}

	/**
	 * Delete an application role and its attached permission associations.
	 * System roles cannot be deleted.
	 *
	 * @param roleCode Role unique code.
	 * @return HTTP 204 No Content.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.DELETE')")
	@DeleteMapping("/{roleCode}")
	public ResponseEntity<Void> deleteRole(@PathVariable String roleCode) {
		appRoleService.deleteRole(roleCode);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Revoke a single permission from a role.
	 *
	 * @param roleCode Role unique code.
	 * @param permissionCode Permission code to revoke.
	 * @param httpRequest HTTP servlet request for audit context.
	 * @return HTTP 204 No Content.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@DeleteMapping("/{roleCode}/permissions/{permissionCode}")
	public ResponseEntity<Void> revokePermission(
			@PathVariable String roleCode,
			@PathVariable String permissionCode,
			HttpServletRequest httpRequest) {
		appRoleService.revokePermissionFromRole(roleCode, permissionCode, httpRequest);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Revoke a list of permissions from a role.
	 *
	 * @param roleCode Role unique code.
	 * @param permissionCodes List of permission codes to revoke.
	 * @param httpRequest HTTP servlet request for audit context.
	 * @return HTTP 204 No Content.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.EDIT')")
	@DeleteMapping("/{roleCode}/permissions")
	public ResponseEntity<Void> revokePermissions(
			@PathVariable String roleCode,
			@RequestBody List<String> permissionCodes,
			HttpServletRequest httpRequest) {
		appRoleService.revokePermissionsFromRole(roleCode, permissionCodes, httpRequest);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Retrieve paginated audit history records for a specific role.
	 *
	 * @param roleCode Role unique code.
	 * @param permissionCode Optional filter by permission code.
	 * @param actionTaken Optional filter by action taken (ADDED or REMOVED).
	 * @param page One-based page number (defaults to 1).
	 * @param pageSize Number of items per page (defaults to 20).
	 * @return Paginated history records for the role.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW_HISTORY')")
	@GetMapping("/{roleCode}/permissions/history")
	public ResponseEntity<RolePermissionHistoryPageResponse> getRolePermissionHistory(
			@PathVariable String roleCode,
			@RequestParam(required = false) String permissionCode,
			@RequestParam(required = false) String actionTaken,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int pageSize) {
		RolePermissionHistoryPageResponse history = appRoleService.getRolePermissionHistory(
				roleCode, permissionCode, actionTaken, page, pageSize);
		return ResponseEntity.ok(history);
	}

	/**
	 * Retrieve paginated audit history records across all roles.
	 *
	 * @param roleCode Optional filter by role code.
	 * @param permissionCode Optional filter by permission code.
	 * @param actionTaken Optional filter by action taken (ADDED or REMOVED).
	 * @param page One-based page number (defaults to 1).
	 * @param pageSize Number of items per page (defaults to 20).
	 * @return Paginated history records across all roles.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW_HISTORY')")
	@GetMapping("/permissions/history")
	public ResponseEntity<RolePermissionHistoryPageResponse> getAllRolePermissionHistory(
			@RequestParam(required = false) String roleCode,
			@RequestParam(required = false) String permissionCode,
			@RequestParam(required = false) String actionTaken,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int pageSize) {
		RolePermissionHistoryPageResponse history = appRoleService.getAllRolePermissionHistory(
				roleCode, permissionCode, actionTaken, page, pageSize);
		return ResponseEntity.ok(history);
	}
}
