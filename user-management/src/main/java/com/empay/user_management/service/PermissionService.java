package com.empay.user_management.service;

import java.util.List;

import com.empay.user_management.dto.PermissionResponse;

public interface PermissionService {

	List<PermissionResponse> getAllPermissions(String module);

	PermissionResponse getPermissionByCode(String permissionCode);

	List<String> getAllModules();

	void deletePermission(String permissionCode);
}
