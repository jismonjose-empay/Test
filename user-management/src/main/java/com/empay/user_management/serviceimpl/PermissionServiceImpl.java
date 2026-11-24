package com.empay.user_management.serviceimpl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.user_management.dto.PermissionResponse;
import com.empay.user_management.entities.PermissionEntity;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.RolePermissionRepository;
import com.empay.user_management.service.PermissionService;

import lombok.extern.log4j.Log4j2;

@Service
@Transactional(readOnly = true)
@Log4j2
public class PermissionServiceImpl implements PermissionService {

	private static final String RESOURCE_NAME = "PERMISSION";

	private final PermissionRepository permissionRepository;
	private final RolePermissionRepository rolePermissionRepository;

	public PermissionServiceImpl(
			PermissionRepository permissionRepository,
			RolePermissionRepository rolePermissionRepository) {
		this.permissionRepository = permissionRepository;
		this.rolePermissionRepository = rolePermissionRepository;
	}

	@Override
	public List<PermissionResponse> getAllPermissions(String module) {
		log.info("Fetching permission catalogue with optional module filter: {}", module);
		List<PermissionEntity> entities = (module != null && !module.isBlank())
			? permissionRepository.findByModuleOrderByResourceAscActionNameAsc(module.trim().toUpperCase())
			: permissionRepository.findAllByOrderByModuleAscResourceAscActionNameAsc();

		return entities.stream()
			.map(PermissionResponse::fromEntity)
			.toList();
	}

	@Override
	public PermissionResponse getPermissionByCode(String permissionCode) {
		log.info("Fetching permission details for code: {}", permissionCode);
		return permissionRepository.findByPermissionCode(permissionCode)
			.map(PermissionResponse::fromEntity)
			.orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "permissionCode", "Permission not found with code: " + permissionCode));
	}

	@Override
	public List<String> getAllModules() {
		log.info("Fetching distinct permission modules");
		return permissionRepository.findDistinctModules();
	}

	@Override
	@Transactional
	public void deletePermission(String permissionCode) {
		log.info("Deleting permission with code: {}", permissionCode);
		if (permissionCode == null || permissionCode.isBlank()) {
			throw new IllegalArgumentException("Permission code must not be blank");
		}

		var permission = permissionRepository.findByPermissionCode(permissionCode)
			.orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "permissionCode", "Permission not found with code: " + permissionCode));

		if (rolePermissionRepository.existsByIdPermissionCode(permissionCode)) {
			throw new IllegalArgumentException(
				"Cannot delete permission: Permission '" + permissionCode + "' is currently assigned to one or more roles in ROLE_PERMISSION. Revoke all assignments before deleting the permission."
			);
		}

		permissionRepository.delete(permission);
	}
}
