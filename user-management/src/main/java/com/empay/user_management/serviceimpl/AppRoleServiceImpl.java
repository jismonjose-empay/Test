package com.empay.user_management.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.empay.user_management.dto.RolePermissionsResponse;

import com.empay.user_management.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.empay.user_management.dto.BulkRolePermissionSummary;
import com.empay.user_management.dto.BulkRolePermissionsRequest;
import com.empay.user_management.dto.ConflictCheckRequest;
import com.empay.user_management.dto.ConflictCheckResponse;
import com.empay.user_management.dto.CreatePermissionConflictRequest;
import com.empay.user_management.dto.CreateRoleRequest;
import com.empay.user_management.dto.PermissionResponse;
import com.empay.user_management.dto.RolePermissionAssignment;
import com.empay.user_management.dto.RolePermissionHistoryPageResponse;
import com.empay.user_management.dto.RolePermissionHistoryResponse;
import com.empay.user_management.dto.RolePermissionPair;
import com.empay.user_management.dto.RolePermissionRequest;
import com.empay.user_management.dto.RoleResponse;
import com.empay.user_management.dto.RolesAndPermissionsResponse;
import com.empay.user_management.dto.SodConflictDetail;
import com.empay.user_management.dto.UpdateRoleRequest;
import com.empay.user_management.entities.AppRoleEntity;
import com.empay.user_management.entities.PermissionConflictEntity;
import com.empay.user_management.entities.RoleConflictEntity;
import com.empay.user_management.entities.RolePermissionEntity;
import com.empay.user_management.entities.RolePermissionHistoryEntity;
import com.empay.user_management.entities.RolePermissionId;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.exceptions.SodConflictException;
import com.empay.user_management.service.AppRoleService;
import com.empay.user_management.utils.JwtUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Service
@Transactional(readOnly = true)
@Log4j2
public class AppRoleServiceImpl implements AppRoleService {

	private static final String ROLE_RESOURCE = "APP_ROLE";
	private static final String PERMISSION_RESOURCE = "PERMISSION";
	private static final String ROLE_PERMISSION_RESOURCE = "ROLE_PERMISSION";
	private static final String DEFAULT_USER = "SYSTEM";
	private static final String ROLE_CODE_FIELD = "roleCode";
	private static final String ROLE_NOT_FOUND_PREFIX = "Role not found with code: ";
	private static final String PERMISSION_CODE_FIELD = "permissionCode";
	private static final String PERMISSION_NOT_FOUND_PREFIX = "Permission not found with code: ";
	private static final String SEVERITY_BLOCK = "BLOCK";
	private static final String SEVERITY_WARN = "WARN";
	private static final String ACTION_ADDED = "ADDED";
	private static final String ACTION_REMOVED = "REMOVED";
	private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

	private final AppRoleRepository appRoleRepository;
	private final PermissionRepository permissionRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final RoleConflictRepository roleConflictRepository;
	private final PermissionConflictRepository permissionConflictRepository;
	private final RolePermissionHistoryRepository rolePermissionHistoryRepository;
	private final JwtUtils jwtUtils;
	private final UserRoleRepositoryV2 userRoleRepositoryV2;

	public AppRoleServiceImpl(
            AppRoleRepository appRoleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            RoleConflictRepository roleConflictRepository,
            PermissionConflictRepository permissionConflictRepository,
            RolePermissionHistoryRepository rolePermissionHistoryRepository,
            JwtUtils jwtUtils, UserRoleRepositoryV2 userRoleRepositoryV2) {
		this.appRoleRepository = appRoleRepository;
		this.permissionRepository = permissionRepository;
		this.rolePermissionRepository = rolePermissionRepository;
		this.roleConflictRepository = roleConflictRepository;
		this.permissionConflictRepository = permissionConflictRepository;
		this.rolePermissionHistoryRepository = rolePermissionHistoryRepository;
		this.jwtUtils = jwtUtils;
        this.userRoleRepositoryV2 = userRoleRepositoryV2;
    }

	private ResourceNotFoundException roleNotFound(String roleCode) {
		return new ResourceNotFoundException(ROLE_RESOURCE, ROLE_CODE_FIELD, ROLE_NOT_FOUND_PREFIX + roleCode);
	}

	private ResourceNotFoundException permissionNotFound(String permissionCode) {
		return new ResourceNotFoundException(PERMISSION_RESOURCE, PERMISSION_CODE_FIELD, PERMISSION_NOT_FOUND_PREFIX + permissionCode);
	}

	@Override
	public List<RoleResponse> getAllRoles() {
		log.info("Fetching all application roles");
		return appRoleRepository.findAllByOrderByRoleCodeAsc()
			.stream()
			.map(RoleResponse::fromEntity)
			.toList();
	}

	@Override
	public RoleResponse getRoleByCode(String roleCode) {
		log.info("Fetching role details for role code: {}", roleCode);
		if (roleCode == null || roleCode.isBlank()) {
			throw new IllegalArgumentException("Role code must not be blank");
		}

		return appRoleRepository.findByRoleCode(roleCode)
			.map(RoleResponse::fromEntity)
			.orElseThrow(() -> roleNotFound(roleCode));
	}

	@Override
	public List<PermissionResponse> getPermissionsForRole(String roleCode) {
		log.info("Fetching permissions attached to role code: {}", roleCode);
		if (roleCode == null || roleCode.isBlank()) {
			throw new IllegalArgumentException("Role code must not be blank");
		}

		if (!appRoleRepository.existsByRoleCode(roleCode)) {
			throw roleNotFound(roleCode);
		}

		return permissionRepository.findPermissionsByRoleCode(roleCode)
			.stream()
			.map(PermissionResponse::fromEntity)
			.toList();
	}

	@Override
	public List<RolePermissionsResponse> getBulkRolePermissions(List<String> roleCodes) {
		log.info("Fetching bulk role permissions for roleCodes: {}", roleCodes);

		List<AppRoleEntity> roles;
		if (roleCodes != null && !roleCodes.isEmpty()) {
			List<String> cleanedRoleCodes = roleCodes.stream()
				.filter(code -> code != null && !code.isBlank())
				.map(String::trim)
				.distinct()
				.toList();
			roles = appRoleRepository.findByRoleCodeInOrderByRoleCodeAsc(cleanedRoleCodes);
		} else {
			roles = appRoleRepository.findAllByOrderByRoleCodeAsc();
		}

		if (roles.isEmpty()) {
			return List.of();
		}

		List<String> effectiveRoleCodes = roles.stream().map(AppRoleEntity::getRoleCode).toList();
		List<RolePermissionEntity> assignments = rolePermissionRepository.findAssignmentsByRoleCodes(effectiveRoleCodes);

		Map<String, List<String>> permissionCodesByRole = assignments.stream()
			.filter(a -> a.getPermission() != null)
			.collect(Collectors.groupingBy(
				a -> a.getId().getRoleCode(),
				LinkedHashMap::new,
				Collectors.mapping(a -> a.getPermission().getPermissionCode(), Collectors.toList())
			));

		return roles.stream()
			.map(role -> {
				List<String> permCodes = permissionCodesByRole.getOrDefault(role.getRoleCode(), List.of());
				return new RolePermissionsResponse(
					role.getRoleCode(),
					role.getName(),
					role.getDescription(),
					role.isSystemRole(),
					permCodes
				);
			})
			.toList();
	}

	@Override
	public RolesAndPermissionsResponse getRolesAndPermissions() {
		log.info("Fetching bootstrap payload containing all roles and all permissions");
		var roles = appRoleRepository.findAllByOrderByRoleCodeAsc()
			.stream()
			.map(RoleResponse::fromEntity)
			.toList();

		var permissions = permissionRepository.findAllByOrderByModuleAscResourceAscActionNameAsc()
			.stream()
			.map(PermissionResponse::fromEntity)
			.toList();

		return new RolesAndPermissionsResponse(roles, permissions);
	}

	@Override
	@Transactional
	public RoleResponse createRole(CreateRoleRequest request, HttpServletRequest httpRequest) {
		String roleCode = request.roleCode().trim();
		log.info("Creating new role: {}", roleCode);

		if (appRoleRepository.existsByRoleCode(roleCode)) {
			throw new ResourceAlreadyExistsException(ROLE_RESOURCE, ROLE_CODE_FIELD, "Role already exists with code: " + roleCode);
		}

		String roleName = request.effectiveName().trim();
		if (roleName.isBlank()) {
			throw new IllegalArgumentException("Role name is mandatory");
		}

		List<String> permissionCodes = request.effectivePermissionCodes();
		if (!permissionCodes.isEmpty()) {
			validateRolePermissionsOnCreate(permissionCodes);
		}

		String createdBy = resolveGrantedBy(null, httpRequest);
		AppRoleEntity role = AppRoleEntity.builder()
			.roleCode(roleCode)
			.name(roleName)
			.description(request.description() != null ? request.description().trim() : null)
			.isSystem("N")
			.createdBy(createdBy)
			.createdAt(LocalDateTime.now(DEFAULT_ZONE))
			.build();

		AppRoleEntity saved = appRoleRepository.save(role);

		saveInitialRolePermissions(roleCode, permissionCodes, createdBy);

		return RoleResponse.fromEntity(saved);
	}

	private void validateRolePermissionsOnCreate(List<String> permissionCodes) {
		Set<String> payloadCodes = new HashSet<>();
		for (String code : permissionCodes) {
			if (!payloadCodes.add(code)) {
				throw new ResourceAlreadyExistsException(
					ROLE_PERMISSION_RESOURCE,
					PERMISSION_CODE_FIELD,
					"Duplicate permission code '" + code + "' in request payload"
				);
			}
			if (!permissionRepository.existsByPermissionCode(code)) {
				throw permissionNotFound(code);
			}
		}
		List<SodConflictDetail> conflicts = detectConflicts(payloadCodes);
		validateNoBlockingConflicts(conflicts);
	}

	private void recordHistory(String roleCode, String permissionCode, String actionTaken, String changedBy) {
		var historyEntity = RolePermissionHistoryEntity.builder()
			.roleCode(roleCode)
			.permissionCode(permissionCode)
			.actionTaken(actionTaken)
			.changedBy(changedBy)
			.changedAt(LocalDateTime.now(DEFAULT_ZONE))
			.build();
		rolePermissionHistoryRepository.save(historyEntity);
	}

	private void saveInitialRolePermissions(String roleCode, List<String> permissionCodes, String createdBy) {
		if (permissionCodes != null && !permissionCodes.isEmpty()) {
			for (String code : permissionCodes) {
				var assignment = RolePermissionEntity.builder()
					.id(new RolePermissionId(roleCode, code))
					.grantedBy(createdBy)
					.grantedAt(LocalDateTime.now(DEFAULT_ZONE))
					.build();
				rolePermissionRepository.save(assignment);
				recordHistory(roleCode, code, ACTION_ADDED, createdBy);
			}
		}
	}

	@Override
	@Transactional
	public RoleResponse updateRole(String roleCode, UpdateRoleRequest request, HttpServletRequest httpRequest) {
		log.info("Updating role details for role code: {}", roleCode);
		if (roleCode == null || roleCode.isBlank()) {
			throw new IllegalArgumentException("Role code must not be blank");
		}

		var role = appRoleRepository.findByRoleCode(roleCode)
			.orElseThrow(() -> roleNotFound(roleCode));

		role.setName(request.name().trim());
		if (request.description() != null) {
			role.setDescription(request.description().trim());
		}

		AppRoleEntity saved = appRoleRepository.save(role);
		return RoleResponse.fromEntity(saved);
	}

	@Override
	@Transactional
	public List<PermissionResponse> assignPermissionsToRole(RolePermissionRequest request, HttpServletRequest httpRequest) {
		String roleCode = request.roleCode();
		log.info("Assigning permissions to role: {}", roleCode);

		if (roleCode == null || roleCode.isBlank()) {
			throw new IllegalArgumentException("Role code must not be blank");
		}

		if (request.permissionCodes() == null || request.permissionCodes().isEmpty()) {
			throw new IllegalArgumentException("Permission codes list must not be empty");
		}

		if (!appRoleRepository.existsByRoleCode(roleCode)) {
			throw roleNotFound(roleCode);
		}

		String grantedBy = resolveGrantedBy(request.grantedBy(), httpRequest);

		Set<String> payloadCodes = new HashSet<>();
		for (String permissionCode : request.permissionCodes()) {
			if (!payloadCodes.add(permissionCode)) {
				throw new ResourceAlreadyExistsException(
					ROLE_PERMISSION_RESOURCE,
					PERMISSION_CODE_FIELD,
					"Duplicate permission code '" + permissionCode + "' in request payload"
				);
			}
		}

		for (String permissionCode : request.permissionCodes()) {
			if (!permissionRepository.existsByPermissionCode(permissionCode)) {
				throw permissionNotFound(permissionCode);
			}

			if (rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode(roleCode, permissionCode)) {
				throw new ResourceAlreadyExistsException(
					ROLE_PERMISSION_RESOURCE,
					PERMISSION_CODE_FIELD,
					"Permission '" + permissionCode + "' is already assigned to role '" + roleCode + "'"
				);
			}
		}

		// Calculate resulting permissions and check SoD conflicts (PERMISSION_CONFLICT & ROLE_CONFLICT)
		Set<String> resultingCodes = new HashSet<>(rolePermissionRepository.findAssignedPermissionCodesByRoleCode(roleCode));
		resultingCodes.addAll(request.permissionCodes());

		List<SodConflictDetail> conflicts = detectConflicts(resultingCodes);
		validateNoBlockingConflicts(conflicts);

		for (String permissionCode : request.permissionCodes()) {
			var assignment = RolePermissionEntity.builder()
				.id(new RolePermissionId(roleCode, permissionCode))
				.grantedBy(grantedBy)
				.grantedAt(LocalDateTime.now(DEFAULT_ZONE))
				.build();
			rolePermissionRepository.save(assignment);
			recordHistory(roleCode, permissionCode, ACTION_ADDED, grantedBy);
		}

		return getPermissionsForRole(roleCode);
	}

	@Override
	@Transactional
	public List<PermissionResponse> updateRolePermissions(String roleCode, List<String> permissionCodes, HttpServletRequest httpRequest) {
		log.info("Updating all permissions for role: {}", roleCode);

		if (roleCode == null || roleCode.isBlank()) {
			throw new IllegalArgumentException("Role code must not be blank");
		}

		if (!appRoleRepository.existsByRoleCode(roleCode)) {
			throw roleNotFound(roleCode);
		}

		Set<String> targetSet = validateTargetPermissionCodes(roleCode, permissionCodes);
		List<SodConflictDetail> conflicts = detectConflicts(targetSet);
		validateNoBlockingConflicts(conflicts);

		Set<String> currentCodes = rolePermissionRepository.findAssignedPermissionCodesByRoleCode(roleCode);
		String grantedBy = resolveGrantedBy(null, httpRequest);

		// Remove unselected permissions
		List<String> toRemove = currentCodes.stream()
			.filter(code -> !targetSet.contains(code))
			.toList();

		if (!toRemove.isEmpty()) {
			rolePermissionRepository.deleteByRoleCodeAndPermissionCodes(roleCode, toRemove);
			for (String code : toRemove) {
				recordHistory(roleCode, code, ACTION_REMOVED, grantedBy);
			}
		}

		// Add new selected permissions
		List<String> toAdd = targetSet.stream()
			.filter(code -> !currentCodes.contains(code))
			.toList();

		for (String newPermissionCode : toAdd) {
			var assignment = RolePermissionEntity.builder()
				.id(new RolePermissionId(roleCode, newPermissionCode))
				.grantedBy(grantedBy)
				.grantedAt(LocalDateTime.now(DEFAULT_ZONE))
				.build();
			rolePermissionRepository.save(assignment);
			recordHistory(roleCode, newPermissionCode, ACTION_ADDED, grantedBy);
		}

		return getPermissionsForRole(roleCode);
	}

	@Override
	@Transactional
	public BulkRolePermissionSummary bulkConfigureRolePermissions(BulkRolePermissionsRequest request, HttpServletRequest httpRequest) {
		log.info("Executing bulk role permission configuration");

		if (request == null || request.isEmpty()) {
			throw new IllegalArgumentException("Bulk role permission payload must contain roles, grants, revocations, or action items");
		}

		List<RolePermissionAssignment> effectiveRoles = new ArrayList<>();
		List<RolePermissionPair> effectiveGrants = new ArrayList<>();
		List<RolePermissionPair> effectiveRevocations = new ArrayList<>();

		resolveBulkRequests(request, effectiveRoles, effectiveGrants, effectiveRevocations);

		if (effectiveRoles.isEmpty() && effectiveGrants.isEmpty() && effectiveRevocations.isEmpty()) {
			throw new IllegalArgumentException("Bulk role permission payload must contain roles, grants, revocations, or action items");
		}

		String grantedBy = resolveGrantedBy(null, httpRequest);
		Set<String> affectedRoleCodes = new HashSet<>();

		int[] matrixMetrics = processBulkMatrixRoles(effectiveRoles, grantedBy, affectedRoleCodes);
		int totalGranted = matrixMetrics[0] + processBulkGrants(effectiveGrants, grantedBy, affectedRoleCodes);
		int totalRevoked = matrixMetrics[1] + processBulkRevocations(effectiveRevocations, grantedBy, affectedRoleCodes);

		List<RoleResponse> updatedRoles = affectedRoleCodes.stream()
			.map(appRoleRepository::findByRoleCode)
			.flatMap(Optional::stream)
			.map(RoleResponse::fromEntity)
			.toList();

		String message = String.format(
			"Bulk role permission configuration processed successfully. Granted: %d, Revoked: %d across %d roles.",
			totalGranted, totalRevoked, affectedRoleCodes.size()
		);

		return new BulkRolePermissionSummary(
			affectedRoleCodes.size(),
			totalGranted,
			totalRevoked,
			updatedRoles,
			message
		);
	}

	private void resolveBulkRequests(
			BulkRolePermissionsRequest request,
			List<RolePermissionAssignment> effectiveRoles,
			List<RolePermissionPair> effectiveGrants,
			List<RolePermissionPair> effectiveRevocations) {
		if (request.hasRoles()) {
			effectiveRoles.addAll(request.roles());
		}
		if (request.hasGrants()) {
			effectiveGrants.addAll(request.grants());
		}
		if (request.hasRevocations()) {
			effectiveRevocations.addAll(request.revocations());
		}

		if (request.hasAction()) {
			resolveActionBasedBulkRequests(request, effectiveRoles, effectiveGrants, effectiveRevocations);
		} else {
			handleGrantAction(request, effectiveGrants);
		}
	}

	private void resolveActionBasedBulkRequests(
			BulkRolePermissionsRequest request,
			List<RolePermissionAssignment> effectiveRoles,
			List<RolePermissionPair> effectiveGrants,
			List<RolePermissionPair> effectiveRevocations) {
		String act = request.action().trim().toUpperCase();
		switch (act) {
			case "GRANT" -> handleGrantAction(request, effectiveGrants);
			case "REVOKE" -> handleRevokeAction(request, effectiveRevocations);
			case "SYNC" -> handleSyncAction(request, effectiveRoles);
			default -> throw new IllegalArgumentException("Action must be either GRANT, REVOKE, or SYNC");
		}
	}

	private void handleGrantAction(BulkRolePermissionsRequest request, List<RolePermissionPair> effectiveGrants) {
		if (request.hasSingleRoleAction()) {
			for (String perm : request.permissionCodes()) {
				effectiveGrants.add(new RolePermissionPair(request.roleCode().trim(), perm.trim()));
			}
		}
		if (request.hasItems()) {
			effectiveGrants.addAll(request.items());
		}
	}

	private void handleRevokeAction(BulkRolePermissionsRequest request, List<RolePermissionPair> effectiveRevocations) {
		if (request.hasSingleRoleAction()) {
			for (String perm : request.permissionCodes()) {
				effectiveRevocations.add(new RolePermissionPair(request.roleCode().trim(), perm.trim()));
			}
		}
		if (request.hasItems()) {
			effectiveRevocations.addAll(request.items());
		}
	}

	private void handleSyncAction(BulkRolePermissionsRequest request, List<RolePermissionAssignment> effectiveRoles) {
		if (request.hasSingleRoleAction()) {
			effectiveRoles.add(new RolePermissionAssignment(request.roleCode().trim(), request.permissionCodes()));
		}
	}

	private int[] processBulkMatrixRoles(
			List<RolePermissionAssignment> effectiveRoles,
			String grantedBy,
			Set<String> affectedRoleCodes) {
		int totalGranted = 0;
		int totalRevoked = 0;
		for (RolePermissionAssignment assignment : effectiveRoles) {
			int[] singleResult = syncSingleRoleMatrix(assignment, grantedBy, affectedRoleCodes);
			totalGranted += singleResult[0];
			totalRevoked += singleResult[1];
		}
		return new int[] { totalGranted, totalRevoked };
	}

	private int[] syncSingleRoleMatrix(
			RolePermissionAssignment assignment,
			String grantedBy,
			Set<String> affectedRoleCodes) {
		if (assignment.roleCode() == null || assignment.roleCode().isBlank()) {
			throw new IllegalArgumentException("Role code in assignments must not be blank");
		}
		String roleCode = assignment.roleCode().trim();
		affectedRoleCodes.add(roleCode);

		if (!appRoleRepository.existsByRoleCode(roleCode)) {
			throw roleNotFound(roleCode);
		}

		Set<String> targetSet = validateTargetPermissionCodes(roleCode, assignment.permissionCodes());
		List<SodConflictDetail> conflicts = detectConflicts(targetSet);
		validateNoBlockingConflicts(conflicts);

		Set<String> currentCodes = rolePermissionRepository.findAssignedPermissionCodesByRoleCode(roleCode);

		int revokedCount = 0;
		List<String> toRemove = currentCodes.stream()
			.filter(c -> !targetSet.contains(c))
			.toList();
		if (!toRemove.isEmpty()) {
			rolePermissionRepository.deleteByRoleCodeAndPermissionCodes(roleCode, toRemove);
			for (String code : toRemove) {
				recordHistory(roleCode, code, ACTION_REMOVED, grantedBy);
			}
			revokedCount = toRemove.size();
		}

		int grantedCount = 0;
		List<String> toAdd = targetSet.stream()
			.filter(c -> !currentCodes.contains(c))
			.toList();
		if (!toAdd.isEmpty()) {
			List<RolePermissionEntity> newEntities = toAdd.stream()
				.map(p -> RolePermissionEntity.builder()
					.id(new RolePermissionId(roleCode, p))
					.grantedBy(grantedBy)
					.grantedAt(LocalDateTime.now(DEFAULT_ZONE))
					.build())
				.toList();
			rolePermissionRepository.saveAll(newEntities);
			for (String code : toAdd) {
				recordHistory(roleCode, code, ACTION_ADDED, grantedBy);
			}
			grantedCount = toAdd.size();
		}

		return new int[] { grantedCount, revokedCount };
	}

	private Set<String> validateTargetPermissionCodes(String roleCode, List<String> permissionCodes) {
		List<String> targetCodes = (permissionCodes == null) ? List.of() : permissionCodes;
		Set<String> targetSet = new HashSet<>();
		for (String code : targetCodes) {
			if (code == null || code.isBlank()) {
				throw new IllegalArgumentException("Permission code must not be blank for role: " + roleCode);
			}
			String permCode = code.trim();
			if (!targetSet.add(permCode)) {
				throw new ResourceAlreadyExistsException(
					ROLE_PERMISSION_RESOURCE,
					PERMISSION_CODE_FIELD,
					"Duplicate permission code '" + permCode + "' in request payload"
				);
			}
			if (!permissionRepository.existsByPermissionCode(permCode)) {
				throw permissionNotFound(permCode);
			}
		}
		return targetSet;
	}

	private int processBulkGrants(
			List<RolePermissionPair> effectiveGrants,
			String grantedBy,
			Set<String> affectedRoleCodes) {
		int totalGranted = 0;
		for (RolePermissionPair pair : effectiveGrants) {
			String roleCode = pair.roleCode().trim();
			String permCode = pair.permissionCode().trim();
			affectedRoleCodes.add(roleCode);

			if (!appRoleRepository.existsByRoleCode(roleCode)) {
				throw roleNotFound(roleCode);
			}
			if (!permissionRepository.existsByPermissionCode(permCode)) {
				throw permissionNotFound(permCode);
			}

			if (!rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode(roleCode, permCode)) {
				Set<String> resulting = new HashSet<>(rolePermissionRepository.findAssignedPermissionCodesByRoleCode(roleCode));
				resulting.add(permCode);

				List<SodConflictDetail> conflicts = detectConflicts(resulting);
				validateNoBlockingConflicts(conflicts);

				var entity = RolePermissionEntity.builder()
					.id(new RolePermissionId(roleCode, permCode))
					.grantedBy(grantedBy)
					.grantedAt(LocalDateTime.now(DEFAULT_ZONE))
					.build();
				rolePermissionRepository.save(entity);
				recordHistory(roleCode, permCode, ACTION_ADDED, grantedBy);
				totalGranted++;
			}
		}
		return totalGranted;
	}

	private int processBulkRevocations(
			List<RolePermissionPair> effectiveRevocations,
			String changedBy,
			Set<String> affectedRoleCodes) {
		int totalRevoked = 0;
		for (RolePermissionPair pair : effectiveRevocations) {
			String roleCode = pair.roleCode().trim();
			String permCode = pair.permissionCode().trim();
			affectedRoleCodes.add(roleCode);

			if (rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode(roleCode, permCode)) {
				rolePermissionRepository.deleteById(new RolePermissionId(roleCode, permCode));
				recordHistory(roleCode, permCode, ACTION_REMOVED, changedBy);
				totalRevoked++;
			}
		}
		return totalRevoked;
	}

	@Override
	public ConflictCheckResponse checkConflicts(ConflictCheckRequest request) {
		String roleCode = request.roleCode();
		log.info("Evaluating SoD conflicts for role: {}", roleCode);

		if (roleCode != null && !roleCode.isBlank() && !appRoleRepository.existsByRoleCode(roleCode)) {
			throw roleNotFound(roleCode);
		}

		Set<String> permissionSet = new HashSet<>(request.permissionCodes());
		if (roleCode != null && !roleCode.isBlank() && appRoleRepository.existsByRoleCode(roleCode)) {
			permissionSet.addAll(rolePermissionRepository.findAssignedPermissionCodesByRoleCode(roleCode));
		}

		List<SodConflictDetail> conflicts = detectConflicts(permissionSet);
		return ConflictCheckResponse.of(roleCode, conflicts);
	}

	@Override
	public List<SodConflictDetail> getRoleConflicts() {
		return roleConflictRepository.findAll().stream()
			.map(SodConflictDetail::fromRoleConflict)
			.toList();
	}

	@Override
	public List<SodConflictDetail> getPermissionConflicts() {
		return permissionConflictRepository.findAll().stream()
			.map(SodConflictDetail::fromPermissionConflict)
			.toList();
	}

	@Override
	@Transactional
	public SodConflictDetail createPermissionConflict(CreatePermissionConflictRequest request) {
		log.info("Creating permission conflict between {} and {}", request.permissionCodeA(), request.permissionCodeB());

		if (request.permissionCodeA() == null || request.permissionCodeA().isBlank()) {
			throw new IllegalArgumentException("Permission code A must not be blank");
		}
		if (request.permissionCodeB() == null || request.permissionCodeB().isBlank()) {
			throw new IllegalArgumentException("Permission code B must not be blank");
		}

		String codeA = request.permissionCodeA().trim();
		String codeB = request.permissionCodeB().trim();

		if (codeA.equalsIgnoreCase(codeB)) {
			throw new IllegalArgumentException("Permission code A and permission code B must be distinct");
		}

		if (!permissionRepository.existsByPermissionCode(codeA)) {
			throw permissionNotFound(codeA);
		}
		if (!permissionRepository.existsByPermissionCode(codeB)) {
			throw permissionNotFound(codeB);
		}

		// Enforce alphabetical order for database check constraint CK_PC_ORDER: permission_code_a < permission_code_b
		String orderedCodeA = codeA.compareTo(codeB) < 0 ? codeA : codeB;
		String orderedCodeB = codeA.compareTo(codeB) < 0 ? codeB : codeA;

		if (permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB(orderedCodeA, orderedCodeB)) {
			throw new ResourceAlreadyExistsException(
				"PERMISSION_CONFLICT",
				"pair",
				"Permission conflict already exists between '" + orderedCodeA + "' and '" + orderedCodeB + "'"
			);
		}

		RoleConflictEntity derivedRoleConflict = null;
		if (request.derivedFromRoleConflict() != null) {
			derivedRoleConflict = roleConflictRepository.findById(request.derivedFromRoleConflict())
				.orElseThrow(() -> new ResourceNotFoundException(
					"ROLE_CONFLICT",
					"conflictId",
					"Role conflict not found with id: " + request.derivedFromRoleConflict()
				));
		}

		String severity = (request.severity() != null) ? request.severity().trim().toUpperCase() : SEVERITY_BLOCK;
		if (!SEVERITY_BLOCK.equals(severity) && !SEVERITY_WARN.equals(severity)) {
			throw new IllegalArgumentException("Severity must be either BLOCK or WARN");
		}

		PermissionConflictEntity entity = PermissionConflictEntity.builder()
			.permissionCodeA(orderedCodeA)
			.permissionCodeB(orderedCodeB)
			.severity(severity)
			.rationale(request.rationale().trim())
			.derivedFromRoleConflict(request.derivedFromRoleConflict())
			.derivedRoleConflict(derivedRoleConflict)
			.build();

		PermissionConflictEntity saved = permissionConflictRepository.save(entity);
		return SodConflictDetail.fromPermissionConflict(saved);
	}

	private List<SodConflictDetail> detectConflicts(Set<String> permissionCodes) {
		if (permissionCodes == null || permissionCodes.size() < 2) {
			return List.of();
		}

		List<PermissionConflictEntity> permConflicts = permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(permissionCodes, permissionCodes);
		return permConflicts.stream()
			.map(SodConflictDetail::fromPermissionConflict)
			.toList();
	}

	private void validateNoBlockingConflicts(List<SodConflictDetail> conflicts) {
		List<SodConflictDetail> blocking = conflicts.stream()
			.filter(c -> SEVERITY_BLOCK.equalsIgnoreCase(c.severity()))
			.toList();

		if (!blocking.isEmpty()) {
			String details = blocking.stream()
				.map(c -> c.conflictType() + ": " + c.codeA() + " conflicts with " + c.codeB() + " (" + c.rationale() + ")")
				.collect(Collectors.joining("; "));
			throw new SodConflictException("Segregation of duties conflict detected: " + details, blocking);
		}
	}

	private String resolveGrantedBy(String requestGrantedBy, HttpServletRequest httpRequest) {
		if (requestGrantedBy != null && !requestGrantedBy.isBlank()) {
			return requestGrantedBy.trim();
		}
		if (httpRequest != null && jwtUtils != null) {
			try {
				String userId = jwtUtils.getUserId(httpRequest);
				if (userId != null && !userId.isBlank()) {
					return userId;
				}
			} catch (Exception ex) {
				log.debug("Unable to extract user ID from JWT cookies: {}", ex.getMessage());
			}
		}
		return DEFAULT_USER;
	}

	@Override
	@Transactional
	public void deleteRole(String roleCode) {
		log.info("Deleting role with code: {}", roleCode);
		if (roleCode == null || roleCode.isBlank()) {
			throw new IllegalArgumentException("Role code must not be blank");
		}

		var role = appRoleRepository.findByRoleCode(roleCode)
			.orElseThrow(() -> roleNotFound(roleCode));

		if (role.isSystemRole()) {
			throw new IllegalArgumentException("System role cannot be deleted: " + roleCode);
		}

		if(userRoleRepositoryV2.existsByRoleCode(roleCode)){
			throw new IllegalArgumentException(
					"Cannot delete role: Role '" + roleCode + "' has assigned to Users."
			);
		}

		if (rolePermissionRepository.existsByIdRoleCode(roleCode)) {
			rolePermissionRepository.deleteByRoleCode(roleCode);
		}

		appRoleRepository.delete(role);
	}

	@Override
	@Transactional
	public void revokePermissionFromRole(String roleCode, String permissionCode) {
		doRevokePermissionFromRole(roleCode, permissionCode, null);
	}

	@Override
	@Transactional
	public void revokePermissionFromRole(String roleCode, String permissionCode, HttpServletRequest httpRequest) {
		doRevokePermissionFromRole(roleCode, permissionCode, httpRequest);
	}

	private void doRevokePermissionFromRole(String roleCode, String permissionCode, HttpServletRequest httpRequest) {
		log.info("Revoking permission {} from role {}", permissionCode, roleCode);
		if (!appRoleRepository.existsByRoleCode(roleCode)) {
			throw roleNotFound(roleCode);
		}

		if (!permissionRepository.existsByPermissionCode(permissionCode)) {
			throw permissionNotFound(permissionCode);
		}

		var id = new RolePermissionId(roleCode, permissionCode);
		if (rolePermissionRepository.existsById(id)) {
			rolePermissionRepository.deleteById(id);
			String changedBy = resolveGrantedBy(null, httpRequest);
			recordHistory(roleCode, permissionCode, ACTION_REMOVED, changedBy);
		}
	}

	@Override
	@Transactional
	public void revokePermissionsFromRole(String roleCode, List<String> permissionCodes) {
		doRevokePermissionsFromRole(roleCode, permissionCodes, null);
	}

	@Override
	@Transactional
	public void revokePermissionsFromRole(String roleCode, List<String> permissionCodes, HttpServletRequest httpRequest) {
		doRevokePermissionsFromRole(roleCode, permissionCodes, httpRequest);
	}

	private void doRevokePermissionsFromRole(String roleCode, List<String> permissionCodes, HttpServletRequest httpRequest) {
		log.info("Revoking multiple permissions from role {}", roleCode);
		if (!appRoleRepository.existsByRoleCode(roleCode)) {
			throw roleNotFound(roleCode);
		}

		if (permissionCodes != null && !permissionCodes.isEmpty()) {
			rolePermissionRepository.deleteByRoleCodeAndPermissionCodes(roleCode, permissionCodes);
			String changedBy = resolveGrantedBy(null, httpRequest);
			for (String code : permissionCodes) {
				recordHistory(roleCode, code, ACTION_REMOVED, changedBy);
			}
		}
	}

	@Override
	public RolePermissionHistoryPageResponse getRolePermissionHistory(
			String roleCode,
			String permissionCode,
			String actionTaken,
			int page,
			int pageSize) {
		log.info("Fetching permission history for role: {}", roleCode);
		if (roleCode == null || roleCode.isBlank()) {
			throw new IllegalArgumentException("Role code must not be blank");
		}
		if (!appRoleRepository.existsByRoleCode(roleCode)) {
			throw roleNotFound(roleCode);
		}

		return queryHistory(roleCode.trim(), permissionCode, actionTaken, page, pageSize);
	}

	@Override
	public RolePermissionHistoryPageResponse getAllRolePermissionHistory(
			String roleCode,
			String permissionCode,
			String actionTaken,
			int page,
			int pageSize) {
		log.info("Fetching global role permission history");
		return queryHistory(roleCode, permissionCode, actionTaken, page, pageSize);
	}

	private RolePermissionHistoryPageResponse queryHistory(
			String roleCode,
			String permissionCode,
			String actionTaken,
			int page,
			int pageSize) {
		int pageNumber = Math.max(page - 1, 0);
		int size = (pageSize <= 0) ? 20 : pageSize;

		Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "changedAt"));

		Specification<RolePermissionHistoryEntity> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (roleCode != null && !roleCode.isBlank()) {
				predicates.add(cb.equal(root.get(ROLE_CODE_FIELD), roleCode.trim()));
			}
			if (permissionCode != null && !permissionCode.isBlank()) {
				predicates.add(cb.equal(root.get(PERMISSION_CODE_FIELD), permissionCode.trim()));
			}
			if (actionTaken != null && !actionTaken.isBlank()) {
				predicates.add(cb.equal(root.get("actionTaken"), actionTaken.trim().toUpperCase()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<RolePermissionHistoryEntity> resultPage = rolePermissionHistoryRepository.findAll(spec, pageable);

		List<RolePermissionHistoryResponse> items = resultPage.getContent().stream()
			.map(RolePermissionHistoryResponse::fromEntity)
			.toList();

		return new RolePermissionHistoryPageResponse(
			items,
			resultPage.getTotalElements(),
			pageNumber + 1,
			size,
			resultPage.getTotalPages(),
			resultPage.hasNext()
		);
	}
}
