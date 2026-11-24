package com.empay.user_management.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;

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

import com.empay.user_management.dto.UserPermissionOverridePageResponse;
import com.empay.user_management.dto.UserPermissionOverrideRequest;
import com.empay.user_management.dto.UserPermissionOverrideResponse;
import com.empay.user_management.service.UserPermissionOverrideService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sod/override")
@RequiredArgsConstructor
public class UserPermissionOverrideController {

	private final UserPermissionOverrideService overrideService;

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.GRANT_OVERRIDE')")
	@PostMapping
	public ResponseEntity<UserPermissionOverrideResponse> createOverride(
			@Valid @RequestBody UserPermissionOverrideRequest request) {

		return ResponseEntity.status(HttpStatus.CREATED).body(overrideService.createOverride(request));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
	@GetMapping
	public ResponseEntity<UserPermissionOverridePageResponse> getAllOverrides(
	        @RequestParam(required = false) String search,
	        @RequestParam(defaultValue = "overrideId") String sortBy,
	        @RequestParam(defaultValue = "DESC") String sortDir,
	        @RequestParam(defaultValue = "1") int page,
	        @RequestParam(defaultValue = "20") int pageSize) {

	    return ResponseEntity.ok(
	            overrideService.getAllOverrides(
	                    search, sortBy, sortDir, page, pageSize));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
	@GetMapping("/{id}")
	public ResponseEntity<UserPermissionOverrideResponse> getOverrideById(@PathVariable Long id) {

		return ResponseEntity.ok(overrideService.getOverrideById(id));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_SOD.GRANT_OVERRIDE')")
	@DeleteMapping("/{overrideId}")
	public ResponseEntity<Map<String, String>> deleteOverride(@PathVariable Long overrideId,
			@RequestParam String changedBy) {

		overrideService.deleteOverride(overrideId, changedBy);

		Map<String, String> response = new HashMap<>();
		response.put("message", "Permission override deleted successfully");
		response.put("overrideId", overrideId.toString());

		return ResponseEntity.ok(response);
	}
}
