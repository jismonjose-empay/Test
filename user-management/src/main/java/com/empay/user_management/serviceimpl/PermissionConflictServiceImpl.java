package com.empay.user_management.serviceimpl;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.user_management.dto.PermissionConflictRequest;
import com.empay.user_management.dto.PermissionConflictResponse;
import com.empay.user_management.entities.PermissionConflictEntity;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.PermissionConflictRepository;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.RoleConflictRepository;
import com.empay.user_management.repository.RolePermissionRepository;
import com.empay.user_management.service.PermissionConflictService;

import lombok.extern.log4j.Log4j2;

@Service
@Transactional
@Log4j2
public class PermissionConflictServiceImpl implements PermissionConflictService {

	private static final String PERMISSION_CONFLICT_RESOURCE = "PermissionConflict";
	private static final String CONFLICT_CODES_FIELD = "permissionCodeA,permissionCodeB";

	private final PermissionConflictRepository permissionConflictRepository;
	private final PermissionRepository permissionRepository;
	private final RoleConflictRepository roleConflictRepository;
	private final RolePermissionRepository rolePermissionRepository;

	public PermissionConflictServiceImpl(
			PermissionConflictRepository permissionConflictRepository,
			PermissionRepository permissionRepository,
			RoleConflictRepository roleConflictRepository,
			RolePermissionRepository rolePermissionRepository) {
		this.permissionConflictRepository = permissionConflictRepository;
		this.permissionRepository = permissionRepository;
		this.roleConflictRepository = roleConflictRepository;
		this.rolePermissionRepository = rolePermissionRepository;
	}

	@Override
	public PermissionConflictResponse createPermissionConflict(PermissionConflictRequest request) {

		String permissionCodeA = normalizeCode(request.getPermissionCodeA());
		String permissionCodeB = normalizeCode(request.getPermissionCodeB());
		String severity = normalizeSeverity(request.getSeverity());
		String rationale = normalizeRationale(request.getRationale());

		String[] sortedCodes = sortPermissionCodes(permissionCodeA, permissionCodeB);

		permissionCodeA = sortedCodes[0];
		permissionCodeB = sortedCodes[1];

		validatePermissionsExist(permissionCodeA, permissionCodeB);
		validateRoleConflict(request.getDerivedFromRoleConflict());

		if (permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB(permissionCodeA, permissionCodeB)) {

			throw new ResourceAlreadyExistsException(PERMISSION_CONFLICT_RESOURCE, CONFLICT_CODES_FIELD,
					"A conflict already exists for permissions " + permissionCodeA + " and " + permissionCodeB);
		}

		if (rolePermissionRepository.existsRoleWithBothPermissions(
		        permissionCodeA, permissionCodeB)) {

		    throw new IllegalArgumentException(
		            "Cannot create permission conflict because both permissions "
		                    + "are already assigned to the same role.");
		}
		PermissionConflictEntity entity = PermissionConflictEntity.builder().permissionCodeA(permissionCodeA)
				.permissionCodeB(permissionCodeB).severity(severity).rationale(rationale)
				.derivedFromRoleConflict(request.getDerivedFromRoleConflict()).build();

		try {
			PermissionConflictEntity saved = permissionConflictRepository.saveAndFlush(entity);

			return toResponse(saved);

		} catch (DataIntegrityViolationException ex) {
			throw new ResourceAlreadyExistsException(PERMISSION_CONFLICT_RESOURCE, CONFLICT_CODES_FIELD,
					"The permission conflict could not be saved because " + "it violates a database constraint");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<PermissionConflictResponse> getAllPermissionConflicts() {

		return permissionConflictRepository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PermissionConflictResponse getPermissionConflictById(Long conflictId) {

		return toResponse(findConflict(conflictId));
	}

	@Override
	public PermissionConflictResponse updatePermissionConflict(Long conflictId, PermissionConflictRequest request) {

		PermissionConflictEntity entity = findConflict(conflictId);

		String permissionCodeA = normalizeCode(request.getPermissionCodeA());
		String permissionCodeB = normalizeCode(request.getPermissionCodeB());
		String severity = normalizeSeverity(request.getSeverity());
		String rationale = normalizeRationale(request.getRationale());

		String[] sortedCodes = sortPermissionCodes(permissionCodeA, permissionCodeB);

		permissionCodeA = sortedCodes[0];
		permissionCodeB = sortedCodes[1];

		validatePermissionsExist(permissionCodeA, permissionCodeB);
		validateRoleConflict(request.getDerivedFromRoleConflict());

		if (permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeBAndConflictIdNot(permissionCodeA,
				permissionCodeB, conflictId)) {

			throw new ResourceAlreadyExistsException(PERMISSION_CONFLICT_RESOURCE, CONFLICT_CODES_FIELD,
					"A conflict already exists for permissions " + permissionCodeA + " and " + permissionCodeB);
		}

		entity.setPermissionCodeA(permissionCodeA);
		entity.setPermissionCodeB(permissionCodeB);
		entity.setSeverity(severity);
		entity.setRationale(rationale);
		entity.setDerivedFromRoleConflict(request.getDerivedFromRoleConflict());

		try {
			PermissionConflictEntity updated = permissionConflictRepository.saveAndFlush(entity);

			return toResponse(updated);

		} catch (DataIntegrityViolationException ex) {
			throw new ResourceAlreadyExistsException(PERMISSION_CONFLICT_RESOURCE, CONFLICT_CODES_FIELD,
					"The permission conflict could not be updated because " + "it violates a database constraint");
		}
	}

	@Override
	public void deletePermissionConflict(Long conflictId) {

		PermissionConflictEntity entity = findConflict(conflictId);
		permissionConflictRepository.delete(entity);
	}

	private PermissionConflictEntity findConflict(Long conflictId) {

		if (conflictId == null || conflictId <= 0) {
			throw new IllegalArgumentException("Conflict ID must be a positive number");
		}

		return permissionConflictRepository.findById(conflictId)
				.orElseThrow(() -> new ResourceNotFoundException(PERMISSION_CONFLICT_RESOURCE, "conflictId",
						"Permission conflict not found with ID: " + conflictId));
	}

	private String[] sortPermissionCodes(String codeA, String codeB) {

		if (codeA.equals(codeB)) {
			throw new IllegalArgumentException("Permission code A and permission code B " + "must be different");
		}

		if (codeA.compareTo(codeB) < 0) {
			return new String[] { codeA, codeB };
		}

		return new String[] { codeB, codeA };
	}

	private void validatePermissionsExist(String permissionCodeA, String permissionCodeB) {

		if (!permissionRepository.existsByPermissionCode(permissionCodeA)) {
			throw new ResourceNotFoundException("Permission", "permissionCode",
					"Permission not found: " + permissionCodeA);
		}

		if (!permissionRepository.existsByPermissionCode(permissionCodeB)) {
			throw new ResourceNotFoundException("Permission", "permissionCode",
					"Permission not found: " + permissionCodeB);
		}
	}

	private void validateRoleConflict(Long roleConflictId) {

		if (roleConflictId == null) {
			return;
		}

		if (!roleConflictRepository.existsById(roleConflictId)) {
			throw new ResourceNotFoundException("RoleConflict", "conflictId",
					"Role conflict not found with ID: " + roleConflictId);
		}
	}

	private String normalizeCode(String code) {

		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Permission codes are required");
		}

		return code.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeSeverity(String severity) {

		if (severity == null || severity.isBlank()) {
			throw new IllegalArgumentException("Severity is required");
		}

		String normalized = severity.trim().toUpperCase(Locale.ROOT);

		if (!"BLOCK".equals(normalized) && !"WARN".equals(normalized)) {
			throw new IllegalArgumentException("Severity must be BLOCK or WARN");
		}

		return normalized;
	}

	private String normalizeRationale(String rationale) {

		if (rationale == null || rationale.isBlank()) {
			throw new IllegalArgumentException("Rationale is required");
		}

		String normalized = rationale.trim();

		if (normalized.length() > 400) {
			throw new IllegalArgumentException("Rationale cannot exceed 400 characters");
		}

		return normalized;
	}

	private PermissionConflictResponse toResponse(PermissionConflictEntity entity) {

		return PermissionConflictResponse.builder().conflictId(entity.getConflictId())
				.permissionCodeA(entity.getPermissionCodeA()).permissionCodeB(entity.getPermissionCodeB())
				.severity(entity.getSeverity()).rationale(entity.getRationale())
				.derivedFromRoleConflict(entity.getDerivedFromRoleConflict()).build();
	}
}
