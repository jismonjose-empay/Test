package com.empay.user_management.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.dto.CreatePermissionConflictRequest;
import com.empay.user_management.dto.PermissionResponse;
import com.empay.user_management.dto.SodConflictDetail;
import com.empay.user_management.service.AppRoleService;
import com.empay.user_management.service.PermissionService;

import jakarta.validation.Valid;

/**
 * Controller providing APIs for PERMISSION catalogue and permission conflicts.
 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

	private final PermissionService permissionService;
	private final AppRoleService appRoleService;

	public PermissionController(PermissionService permissionService, AppRoleService appRoleService) {
		this.permissionService = permissionService;
		this.appRoleService = appRoleService;
	}

	/**
	 * Retrieve all system permissions with optional module filter.
	 *
	 * @param module Optional module name filter (e.g., ADMIN, PAYMENT, MERCHANT, PARAM).
	 * @return List of permission records.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW')")
	@GetMapping
	public ResponseEntity<List<PermissionResponse>> getAllPermissions(
			@RequestParam(required = false) String module) {
		return ResponseEntity.ok(permissionService.getAllPermissions(module));
	}

	/**
	 * Retrieve distinct modules defined in the permission catalogue.
	 *
	 * @return List of distinct module names.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW')")
	@GetMapping("/modules")
	public ResponseEntity<List<String>> getAllModules() {
		return ResponseEntity.ok(permissionService.getAllModules());
	}

	/**
	 * Retrieve single permission details by permission code.
	 *
	 * @param permissionCode Permission unique code (e.g., ADMIN.USER.VIEW).
	 * @return Permission record.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.VIEW')")
	@GetMapping("/{permissionCode}")
	public ResponseEntity<PermissionResponse> getPermissionByCode(@PathVariable String permissionCode) {
		return ResponseEntity.ok(permissionService.getPermissionByCode(permissionCode));
	}

	/**
	 * Retrieve all configured permission-level conflicts (PERMISSION_CONFLICT).
	 *
	 * @return List of permission conflict details.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
	@GetMapping("/conflicts")
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
	@PostMapping("/conflicts")
	public ResponseEntity<SodConflictDetail> createPermissionConflict(
			@RequestBody @Valid CreatePermissionConflictRequest request) {
		SodConflictDetail created = appRoleService.createPermissionConflict(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	/**
	 * Delete a permission and all its attached role associations.
	 *
	 * @param permissionCode Permission unique code.
	 * @return HTTP 204 No Content.
	 */
	@PreAuthorize("hasAuthority('ADMIN.UM_ROLES.DELETE')")
	@DeleteMapping("/{permissionCode}")
	public ResponseEntity<Void> deletePermission(@PathVariable String permissionCode) {
		permissionService.deletePermission(permissionCode);
		return ResponseEntity.noContent().build();
	}
}
