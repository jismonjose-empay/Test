package com.empay.user_management.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.dto.PermissionConflictRequest;
import com.empay.user_management.dto.PermissionConflictResponse;
import com.empay.user_management.service.PermissionConflictService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sod/permission-conflicts")
@RequiredArgsConstructor
@Validated
public class PermissionConflictController {

	private final PermissionConflictService permissionConflictService;

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.CREATE')")
	@PostMapping
	public ResponseEntity<PermissionConflictResponse> create(@Valid @RequestBody PermissionConflictRequest request) {

		PermissionConflictResponse response = permissionConflictService.createPermissionConflict(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
	@GetMapping
	public ResponseEntity<List<PermissionConflictResponse>> getAll() {

		return ResponseEntity.ok(permissionConflictService.getAllPermissionConflicts());
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
	@GetMapping("/{conflictId}")
	public ResponseEntity<PermissionConflictResponse> getById(@PathVariable Long conflictId) {

		return ResponseEntity.ok(permissionConflictService.getPermissionConflictById(conflictId));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.CREATE')")
	@PutMapping("/{conflictId}")
	public ResponseEntity<PermissionConflictResponse> update(@PathVariable Long conflictId,
			@Valid @RequestBody PermissionConflictRequest request) {

		return ResponseEntity.ok(permissionConflictService.updatePermissionConflict(conflictId, request));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.DELETE')")
	@DeleteMapping("/{conflictId}")
	public ResponseEntity<Void> delete(@PathVariable Long conflictId) {

		permissionConflictService.deletePermissionConflict(conflictId);

		return ResponseEntity.noContent().build();
	}
}
