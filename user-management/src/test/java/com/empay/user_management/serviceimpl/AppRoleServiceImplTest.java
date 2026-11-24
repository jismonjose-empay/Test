package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.empay.user_management.repository.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.empay.user_management.dto.BulkRolePermissionSummary;
import com.empay.user_management.dto.BulkRolePermissionsRequest;
import com.empay.user_management.dto.ConflictCheckRequest;
import com.empay.user_management.dto.ConflictCheckResponse;
import com.empay.user_management.dto.CreatePermissionConflictRequest;
import com.empay.user_management.dto.CreateRoleRequest;
import com.empay.user_management.dto.PermissionResponse;
import com.empay.user_management.dto.RolePermissionAssignment;
import com.empay.user_management.dto.RolePermissionHistoryPageResponse;
import com.empay.user_management.dto.RolePermissionPair;
import com.empay.user_management.dto.RolePermissionRequest;
import com.empay.user_management.dto.RolePermissionsResponse;
import com.empay.user_management.dto.RoleResponse;
import com.empay.user_management.dto.RolesAndPermissionsResponse;
import com.empay.user_management.dto.SodConflictDetail;
import com.empay.user_management.dto.UpdateRoleRequest;
import com.empay.user_management.entities.AppRoleEntity;
import com.empay.user_management.entities.PermissionConflictEntity;
import com.empay.user_management.entities.PermissionEntity;
import com.empay.user_management.entities.RoleConflictEntity;
import com.empay.user_management.entities.RolePermissionEntity;
import com.empay.user_management.entities.RolePermissionHistoryEntity;
import com.empay.user_management.entities.RolePermissionId;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.exceptions.SodConflictException;
import com.empay.user_management.utils.JwtUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AppRoleServiceImplTest {

	@Mock
	private AppRoleRepository appRoleRepository;

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private RolePermissionRepository rolePermissionRepository;

	@Mock
	private RoleConflictRepository roleConflictRepository;

	@Mock
	private PermissionConflictRepository permissionConflictRepository;

	@Mock
	private RolePermissionHistoryRepository rolePermissionHistoryRepository;

	@Mock
	private UserRoleRepositoryV2 userRoleRepositoryV2;

	@Mock
	private JwtUtils jwtUtils;

	@InjectMocks
	private AppRoleServiceImpl appRoleService;

	private AppRoleEntity sampleRole;
	private PermissionEntity samplePermission;

	@BeforeEach
	void setUp() {
		sampleRole = AppRoleEntity.builder()
			.roleCode("USER_ADMIN")
			.name("User Administrator")
			.description("Creates and maintains users")
			.isSystem("N")
			.createdBy("SEED")
			.createdAt(LocalDateTime.now())
			.build();

		samplePermission = PermissionEntity.builder()
			.permissionCode("ADMIN.USER.VIEW")
			.module("ADMIN")
			.resource("USER")
			.actionName("VIEW")
			.description("View user accounts")
			.isSensitive("N")
			.legacyRecordId(101)
			.build();
	}

	@Test
	void testGetAllRoles() {
		when(appRoleRepository.findAllByOrderByRoleCodeAsc()).thenReturn(List.of(sampleRole));

		List<RoleResponse> results = appRoleService.getAllRoles();

		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("USER_ADMIN", results.getFirst().roleCode());
		assertEquals("User Administrator", results.getFirst().name());
		assertFalse(results.getFirst().isSystem());
	}

	@Test
	void testGetRoleByCode_Success() {
		when(appRoleRepository.findByRoleCode("USER_ADMIN")).thenReturn(Optional.of(sampleRole));

		RoleResponse result = appRoleService.getRoleByCode("USER_ADMIN");

		assertNotNull(result);
		assertEquals("USER_ADMIN", result.roleCode());
		assertEquals("User Administrator", result.name());
	}

	@Test
	void testGetRoleByCode_NotFound() {
		when(appRoleRepository.findByRoleCode("UNKNOWN")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> appRoleService.getRoleByCode("UNKNOWN"));
	}

	@Test
	void testGetPermissionsForRole_Success() {
		when(appRoleRepository.existsByRoleCode("USER_ADMIN")).thenReturn(true);
		when(permissionRepository.findPermissionsByRoleCode("USER_ADMIN")).thenReturn(List.of(samplePermission));

		List<PermissionResponse> permissions = appRoleService.getPermissionsForRole("USER_ADMIN");

		assertNotNull(permissions);
		assertEquals(1, permissions.size());
		assertEquals("ADMIN.USER.VIEW", permissions.getFirst().permissionCode());
		assertEquals("ADMIN", permissions.getFirst().module());
		assertFalse(permissions.getFirst().isSensitive());
	}

	@Test
	void testGetPermissionsForRole_RoleNotFound() {
		when(appRoleRepository.existsByRoleCode("UNKNOWN")).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> appRoleService.getPermissionsForRole("UNKNOWN"));
	}

	@Test
	void testGetRolesAndPermissions() {
		when(appRoleRepository.findAllByOrderByRoleCodeAsc()).thenReturn(List.of(sampleRole));
		when(permissionRepository.findAllByOrderByModuleAscResourceAscActionNameAsc()).thenReturn(List.of(samplePermission));

		RolesAndPermissionsResponse response = appRoleService.getRolesAndPermissions();

		assertNotNull(response);
		assertEquals(1, response.roles().size());
		assertEquals(1, response.permissions().size());
		assertEquals("USER_ADMIN", response.roles().getFirst().roleCode());
		assertEquals("ADMIN.USER.VIEW", response.permissions().getFirst().permissionCode());
	}

	@Test
	void testGetBulkRolePermissions_AllRoles() {
		var assignment = RolePermissionEntity.builder()
			.id(new RolePermissionId("USER_ADMIN", "ADMIN.USER.VIEW"))
			.role(sampleRole)
			.permission(samplePermission)
			.grantedBy("SEED")
			.grantedAt(LocalDateTime.now())
			.build();

		when(appRoleRepository.findAllByOrderByRoleCodeAsc()).thenReturn(List.of(sampleRole));
		when(rolePermissionRepository.findAssignmentsByRoleCodes(List.of("USER_ADMIN"))).thenReturn(List.of(assignment));

		List<RolePermissionsResponse> results = appRoleService.getBulkRolePermissions(null);

		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("USER_ADMIN", results.getFirst().roleCode());
		assertEquals("User Administrator", results.getFirst().roleName());
		assertEquals(List.of("ADMIN.USER.VIEW"), results.getFirst().permissionCodes());
	}

	@Test
	void testGetBulkRolePermissions_FilteredRoles() {
		var assignment = RolePermissionEntity.builder()
			.id(new RolePermissionId("USER_ADMIN", "ADMIN.USER.VIEW"))
			.role(sampleRole)
			.permission(samplePermission)
			.grantedBy("SEED")
			.grantedAt(LocalDateTime.now())
			.build();

		when(appRoleRepository.findByRoleCodeInOrderByRoleCodeAsc(List.of("USER_ADMIN"))).thenReturn(List.of(sampleRole));
		when(rolePermissionRepository.findAssignmentsByRoleCodes(List.of("USER_ADMIN"))).thenReturn(List.of(assignment));

		List<RolePermissionsResponse> results = appRoleService.getBulkRolePermissions(List.of("USER_ADMIN"));

		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("USER_ADMIN", results.getFirst().roleCode());
	}

	@Test
	void testGetBulkRolePermissions_EmptyRoles() {
		when(appRoleRepository.findAllByOrderByRoleCodeAsc()).thenReturn(List.of());

		List<RolePermissionsResponse> results = appRoleService.getBulkRolePermissions(null);

		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	void testAssignPermissionsToRole_Success() {
		var request = new RolePermissionRequest("USER_ADMIN", List.of("ADMIN.USER.VIEW"), "ADMIN_USER");

		when(appRoleRepository.existsByRoleCode("USER_ADMIN")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("ADMIN.USER.VIEW")).thenReturn(true);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("USER_ADMIN", "ADMIN.USER.VIEW")).thenReturn(false);
		when(permissionRepository.findPermissionsByRoleCode("USER_ADMIN")).thenReturn(List.of(samplePermission));

		List<PermissionResponse> result = appRoleService.assignPermissionsToRole(request, null);

		assertNotNull(result);
		assertEquals(1, result.size());
		verify(rolePermissionRepository).save(any(RolePermissionEntity.class));
	}

	@Test
	void testUpdateRolePermissions_Success() {
		when(appRoleRepository.existsByRoleCode("USER_ADMIN")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("ADMIN.USER.VIEW")).thenReturn(true);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("USER_ADMIN")).thenReturn(Set.of());
		when(permissionRepository.findPermissionsByRoleCode("USER_ADMIN")).thenReturn(List.of(samplePermission));

		List<PermissionResponse> result = appRoleService.updateRolePermissions("USER_ADMIN", List.of("ADMIN.USER.VIEW"), null);

		assertNotNull(result);
		assertEquals(1, result.size());
		verify(rolePermissionRepository).save(any(RolePermissionEntity.class));
	}

	@Test
	void testAssignPermissionsToRole_DuplicateAssignmentThrowsException() {
		var request = new RolePermissionRequest("USER_ADMIN", List.of("ADMIN.USER.VIEW"), "ADMIN_USER");

		when(appRoleRepository.existsByRoleCode("USER_ADMIN")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("ADMIN.USER.VIEW")).thenReturn(true);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("USER_ADMIN", "ADMIN.USER.VIEW")).thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class, () -> appRoleService.assignPermissionsToRole(request, null));
	}

	@Test
	void testAssignPermissionsToRole_DuplicateInPayloadThrowsException() {
		var request = new RolePermissionRequest("USER_ADMIN", List.of("ADMIN.USER.VIEW", "ADMIN.USER.VIEW"), "ADMIN_USER");

		when(appRoleRepository.existsByRoleCode("USER_ADMIN")).thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class, () -> appRoleService.assignPermissionsToRole(request, null));
	}

	@Test
	void testDeleteRole_Success() {
		when(appRoleRepository.findByRoleCode("USER_ADMIN")).thenReturn(Optional.of(sampleRole));
		when(rolePermissionRepository.existsByIdRoleCode("USER_ADMIN")).thenReturn(false);

		appRoleService.deleteRole("USER_ADMIN");

		verify(appRoleRepository).delete(sampleRole);
	}

	@Test
	void testDeleteRole_HasUserRolesThrowsException() {
		when(appRoleRepository.findByRoleCode("USER_ADMIN")).thenReturn(Optional.of(sampleRole));
		when(userRoleRepositoryV2.existsByRoleCode("USER_ADMIN")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> appRoleService.deleteRole("USER_ADMIN"));
	}

	@Test
	void testDeleteRole_SystemRoleThrowsException() {
		AppRoleEntity systemRole = AppRoleEntity.builder()
			.roleCode("SYS_ADMIN")
			.name("System Administrator")
			.isSystem("Y")
			.createdBy("SEED")
			.createdAt(LocalDateTime.now())
			.build();

		when(appRoleRepository.findByRoleCode("SYS_ADMIN")).thenReturn(Optional.of(systemRole));

		assertThrows(IllegalArgumentException.class, () -> appRoleService.deleteRole("SYS_ADMIN"));
	}

	@Test
	void testDeleteRole_NotFound() {
		when(appRoleRepository.findByRoleCode("INVALID")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> appRoleService.deleteRole("INVALID"));
	}

	@Test
	void testRevokePermissionFromRole_Success() {
		when(appRoleRepository.existsByRoleCode("USER_ADMIN")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("ADMIN.USER.VIEW")).thenReturn(true);
		var id = new RolePermissionId("USER_ADMIN", "ADMIN.USER.VIEW");
		when(rolePermissionRepository.existsById(id)).thenReturn(true);

		appRoleService.revokePermissionFromRole("USER_ADMIN", "ADMIN.USER.VIEW");

		verify(rolePermissionRepository).deleteById(id);
	}

	@Test
	void testRevokePermissionsFromRole_Success() {
		when(appRoleRepository.existsByRoleCode("USER_ADMIN")).thenReturn(true);

		appRoleService.revokePermissionsFromRole("USER_ADMIN", List.of("ADMIN.USER.VIEW"));

		verify(rolePermissionRepository).deleteByRoleCodeAndPermissionCodes("USER_ADMIN", List.of("ADMIN.USER.VIEW"));
	}

	@Test
	void testAssignPermissionsToRole_PermissionConflict_ThrowsSodConflictException() {
		var request = new RolePermissionRequest("MAKER", List.of("PAYMENT.SETTLEMENT.CREATE", "PAYMENT.SETTLEMENT.APPROVE"), "ADMIN");

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.APPROVE")).thenReturn(true);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("MAKER", "PAYMENT.SETTLEMENT.CREATE")).thenReturn(false);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("MAKER", "PAYMENT.SETTLEMENT.APPROVE")).thenReturn(false);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER")).thenReturn(Set.of());

		PermissionConflictEntity conflict = PermissionConflictEntity.builder()
			.conflictId(1L)
			.permissionCodeA("PAYMENT.SETTLEMENT.APPROVE")
			.permissionCodeB("PAYMENT.SETTLEMENT.CREATE")
			.severity("BLOCK")
			.rationale("Maker-checker violation: raising and approving settlement")
			.build();

		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any())).thenReturn(List.of(conflict));

		assertThrows(SodConflictException.class, () -> appRoleService.assignPermissionsToRole(request, null));
	}

	@Test
	void testAssignPermissionsToRole_DoesNotCheckRoleConflict() {
		var request = new RolePermissionRequest("MAKER", List.of("PAYMENT.SETTLEMENT.APPROVE"), "ADMIN");

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.APPROVE")).thenReturn(true);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("MAKER", "PAYMENT.SETTLEMENT.APPROVE")).thenReturn(false);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER")).thenReturn(Set.of("PAYMENT.SETTLEMENT.CREATE"));
		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any())).thenReturn(List.of());

		PermissionEntity perm = PermissionEntity.builder()
			.permissionCode("PAYMENT.SETTLEMENT.APPROVE")
			.module("PAYMENT")
			.resource("SETTLEMENT")
			.actionName("APPROVE")
			.description("Approve payment")
			.isSensitive("N")
			.build();
		when(permissionRepository.findPermissionsByRoleCode("MAKER")).thenReturn(List.of(perm));

		var response = appRoleService.assignPermissionsToRole(request, null);

		assertNotNull(response);
		assertEquals(1, response.size());
		verify(rolePermissionRepository).save(any(RolePermissionEntity.class));
		verifyNoInteractions(roleConflictRepository);
	}

	@Test
	void testUpdateRolePermissions_PermissionConflict_ThrowsSodConflictException() {
		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.APPROVE")).thenReturn(true);

		PermissionConflictEntity conflict = PermissionConflictEntity.builder()
			.conflictId(1L)
			.permissionCodeA("PAYMENT.SETTLEMENT.APPROVE")
			.permissionCodeB("PAYMENT.SETTLEMENT.CREATE")
			.severity("BLOCK")
			.rationale("Maker-checker violation")
			.build();

		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any())).thenReturn(List.of(conflict));

		List<String> targetPermissions = List.of("PAYMENT.SETTLEMENT.CREATE", "PAYMENT.SETTLEMENT.APPROVE");
		assertThrows(SodConflictException.class, () ->
			appRoleService.updateRolePermissions("MAKER", targetPermissions, null)
		);
	}

	@Test
	void testCreateRole_Success() {
		var request = new CreateRoleRequest("NEW_ROLE", "New Custom Role", "Description", List.of());

		when(appRoleRepository.existsByRoleCode("NEW_ROLE")).thenReturn(false);
		when(appRoleRepository.save(any(AppRoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RoleResponse response = appRoleService.createRole(request, null);

		assertNotNull(response);
		assertEquals("NEW_ROLE", response.roleCode());
		assertEquals("New Custom Role", response.name());
	}

	@Test
	void testCreateRole_ConflictThrowsSodConflictException() {
		var request = new CreateRoleRequest(
			"COMPLIANCE_OPS",
			"Compliance Operations",
			"Description",
			List.of("ADMIN.OVERRIDE.GRANT", "ADMIN.SOD.MANAGE")
		);

		when(appRoleRepository.existsByRoleCode("COMPLIANCE_OPS")).thenReturn(false);
		when(permissionRepository.existsByPermissionCode("ADMIN.OVERRIDE.GRANT")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("ADMIN.SOD.MANAGE")).thenReturn(true);

		PermissionConflictEntity conflict = PermissionConflictEntity.builder()
			.conflictId(2L)
			.permissionCodeA("ADMIN.OVERRIDE.GRANT")
			.permissionCodeB("ADMIN.SOD.MANAGE")
			.severity("BLOCK")
			.rationale("Cannot both grant overrides and edit SoD rules")
			.build();

		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any())).thenReturn(List.of(conflict));

		assertThrows(SodConflictException.class, () -> appRoleService.createRole(request, null));
	}

	@Test
	void testUpdateRole_Success() {
		var request = new UpdateRoleRequest("Updated User Administrator", "New description");

		when(appRoleRepository.findByRoleCode("USER_ADMIN")).thenReturn(Optional.of(sampleRole));
		when(appRoleRepository.save(any(AppRoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RoleResponse response = appRoleService.updateRole("USER_ADMIN", request, null);

		assertNotNull(response);
		assertEquals("Updated User Administrator", response.name());
		assertEquals("New description", response.description());
	}

	@Test
	void testCheckConflicts_HasConflicts() {
		var request = new ConflictCheckRequest("MAKER", List.of("PAYMENT.SETTLEMENT.CREATE", "PAYMENT.SETTLEMENT.APPROVE"));

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);

		PermissionConflictEntity conflict = PermissionConflictEntity.builder()
			.conflictId(1L)
			.permissionCodeA("PAYMENT.SETTLEMENT.APPROVE")
			.permissionCodeB("PAYMENT.SETTLEMENT.CREATE")
			.severity("BLOCK")
			.rationale("Maker-checker violation")
			.build();

		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any())).thenReturn(List.of(conflict));

		ConflictCheckResponse response = appRoleService.checkConflicts(request);

		assertNotNull(response);
		assertTrue(response.hasConflicts());
		assertTrue(response.hasBlockingConflicts());
		assertEquals(1, response.conflicts().size());
	}

	@Test
	void testCheckConflicts_NoConflicts() {
		var request = new ConflictCheckRequest("MAKER", List.of("ADMIN.USER.VIEW"));

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);

		ConflictCheckResponse response = appRoleService.checkConflicts(request);

		assertNotNull(response);
		assertFalse(response.hasConflicts());
		assertFalse(response.hasBlockingConflicts());
	}

	@Test
	void testCheckConflicts_MergesExistingAssignedPermissions() {
		var request = new ConflictCheckRequest("MAKER", List.of("PAYMENT.SETTLEMENT.APPROVE"));

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER"))
			.thenReturn(Set.of("PAYMENT.SETTLEMENT.CREATE"));

		PermissionConflictEntity conflict = PermissionConflictEntity.builder()
			.conflictId(1L)
			.permissionCodeA("PAYMENT.SETTLEMENT.APPROVE")
			.permissionCodeB("PAYMENT.SETTLEMENT.CREATE")
			.severity("BLOCK")
			.rationale("Maker-checker violation: raising and approving settlement")
			.derivedFromRoleConflict(null)
			.build();

		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any()))
			.thenReturn(List.of(conflict));

		ConflictCheckResponse response = appRoleService.checkConflicts(request);

		assertNotNull(response);
		assertTrue(response.hasConflicts());
		assertTrue(response.hasBlockingConflicts());
		assertEquals(1, response.conflicts().size());
	}

	@Test
	void testAssignPermissionsToRole_StandalonePermissionConflict_ThrowsSodConflictException() {
		var request = new RolePermissionRequest("OFFICER", List.of("MERCHANT.CREATE", "MERCHANT.APPROVE"), "ADMIN");

		when(appRoleRepository.existsByRoleCode("OFFICER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("MERCHANT.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("MERCHANT.APPROVE")).thenReturn(true);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("OFFICER", "MERCHANT.CREATE")).thenReturn(false);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("OFFICER", "MERCHANT.APPROVE")).thenReturn(false);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("OFFICER")).thenReturn(Set.of());

		PermissionConflictEntity standaloneConflict = PermissionConflictEntity.builder()
			.conflictId(5L)
			.permissionCodeA("MERCHANT.APPROVE")
			.permissionCodeB("MERCHANT.CREATE")
			.severity("BLOCK")
			.rationale("Standalone conflict: Cannot create and approve merchants")
			.derivedFromRoleConflict(null)
			.derivedRoleConflict(null)
			.build();

		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any()))
			.thenReturn(List.of(standaloneConflict));

		assertThrows(SodConflictException.class, () -> appRoleService.assignPermissionsToRole(request, null));
	}

	@Test
	void testCreatePermissionConflict_Standalone_Success() {
		var request = new CreatePermissionConflictRequest(
			"MERCHANT.CREATE",
			"MERCHANT.APPROVE",
			"BLOCK",
			"Cannot create and approve merchants",
			null
		);

		when(permissionRepository.existsByPermissionCode("MERCHANT.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("MERCHANT.APPROVE")).thenReturn(true);
		when(permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB("MERCHANT.APPROVE", "MERCHANT.CREATE"))
			.thenReturn(false);

		PermissionConflictEntity saved = PermissionConflictEntity.builder()
			.conflictId(10L)
			.permissionCodeA("MERCHANT.APPROVE")
			.permissionCodeB("MERCHANT.CREATE")
			.severity("BLOCK")
			.rationale("Cannot create and approve merchants")
			.derivedFromRoleConflict(null)
			.build();

		when(permissionConflictRepository.save(any(PermissionConflictEntity.class))).thenReturn(saved);

		SodConflictDetail result = appRoleService.createPermissionConflict(request);

		assertNotNull(result);
		assertEquals("PERMISSION_CONFLICT", result.conflictType());
		assertEquals("MERCHANT.APPROVE", result.codeA());
		assertEquals("MERCHANT.CREATE", result.codeB());
		assertEquals("BLOCK", result.severity());
		assertEquals(null, result.derivedFromRoleConflictId());
	}

	@Test
	void testCreatePermissionConflict_Derived_Success() {
		var request = new CreatePermissionConflictRequest(
			"PARAM.UPDATE",
			"PARAM.APPROVE",
			"BLOCK",
			"Second eye rule",
			1L
		);

		when(permissionRepository.existsByPermissionCode("PARAM.UPDATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PARAM.APPROVE")).thenReturn(true);
		when(permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB("PARAM.APPROVE", "PARAM.UPDATE"))
			.thenReturn(false);

		var roleConflict = RoleConflictEntity.builder()
			.conflictId(1L)
			.roleCodeA("CHECKER")
			.roleCodeB("MAKER")
			.severity("BLOCK")
			.rationale("Maker-checker")
			.build();
		when(roleConflictRepository.findById(1L)).thenReturn(Optional.of(roleConflict));

		PermissionConflictEntity saved = PermissionConflictEntity.builder()
			.conflictId(11L)
			.permissionCodeA("PARAM.APPROVE")
			.permissionCodeB("PARAM.UPDATE")
			.severity("BLOCK")
			.rationale("Second eye rule")
			.derivedFromRoleConflict(1L)
			.derivedRoleConflict(roleConflict)
			.build();

		when(permissionConflictRepository.save(any(PermissionConflictEntity.class))).thenReturn(saved);

		SodConflictDetail result = appRoleService.createPermissionConflict(request);

		assertNotNull(result);
		assertEquals(1L, result.derivedFromRoleConflictId());
		assertEquals("CHECKER vs MAKER", result.derivedRoleConflict());
	}

	@Test
	void testCreatePermissionConflict_AlreadyExists_ThrowsResourceAlreadyExistsException() {
		var request = new CreatePermissionConflictRequest(
			"PARAM.UPDATE",
			"PARAM.APPROVE",
			"BLOCK",
			"Second eye rule",
			null
		);

		when(permissionRepository.existsByPermissionCode("PARAM.UPDATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PARAM.APPROVE")).thenReturn(true);
		when(permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB("PARAM.APPROVE", "PARAM.UPDATE"))
			.thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class, () -> appRoleService.createPermissionConflict(request));
	}

	@Test
	void testCreatePermissionConflict_PermissionNotFound_ThrowsResourceNotFoundException() {
		var request = new CreatePermissionConflictRequest(
			"UNKNOWN.PERM",
			"PARAM.APPROVE",
			"BLOCK",
			"Second eye rule",
			null
		);

		when(permissionRepository.existsByPermissionCode("UNKNOWN.PERM")).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> appRoleService.createPermissionConflict(request));
	}

	@Test
	void testCreatePermissionConflict_SameCodes_ThrowsIllegalArgumentException() {
		var request = new CreatePermissionConflictRequest(
			"PARAM.APPROVE",
			"PARAM.APPROVE",
			"BLOCK",
			"Same code rule",
			null
		);

		assertThrows(IllegalArgumentException.class, () -> appRoleService.createPermissionConflict(request));
	}

	@Test
	void testBulkConfigureRolePermissions_SyncSuccess() {
		var makerAssignment = new RolePermissionAssignment("MAKER", List.of("PAYMENT.SETTLEMENT.CREATE", "MERCHANT.CREATE"));
		var checkerAssignment = new RolePermissionAssignment("CHECKER", List.of("PAYMENT.SETTLEMENT.APPROVE"));
		var bulkRequest = new BulkRolePermissionsRequest(List.of(makerAssignment, checkerAssignment), null, null);

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(appRoleRepository.existsByRoleCode("CHECKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("MERCHANT.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.APPROVE")).thenReturn(true);

		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER"))
			.thenReturn(Set.of("ADMIN.USER.VIEW"));
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("CHECKER"))
			.thenReturn(Set.of());

		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any()))
			.thenReturn(List.of());

		AppRoleEntity makerEntity = AppRoleEntity.builder().roleCode("MAKER").name("Maker").isSystem("N").build();
		AppRoleEntity checkerEntity = AppRoleEntity.builder().roleCode("CHECKER").name("Checker").isSystem("N").build();
		when(appRoleRepository.findByRoleCode("MAKER")).thenReturn(Optional.of(makerEntity));
		when(appRoleRepository.findByRoleCode("CHECKER")).thenReturn(Optional.of(checkerEntity));

		BulkRolePermissionSummary summary = appRoleService.bulkConfigureRolePermissions(bulkRequest, null);

		assertNotNull(summary);
		assertEquals(2, summary.totalRolesProcessed());
		assertEquals(3, summary.totalPermissionsGranted());
		assertEquals(1, summary.totalPermissionsRevoked());
		verify(rolePermissionRepository).deleteByRoleCodeAndPermissionCodes("MAKER", List.of("ADMIN.USER.VIEW"));
		verify(rolePermissionRepository, times(2)).saveAll(any());
	}

	@Test
	void testBulkConfigureRolePermissions_SodConflict_ThrowsSodConflictException() {
		var makerAssignment = new RolePermissionAssignment(
			"MAKER",
			List.of("PAYMENT.SETTLEMENT.CREATE", "PAYMENT.SETTLEMENT.APPROVE")
		);
		var bulkRequest = new BulkRolePermissionsRequest(List.of(makerAssignment), null, null);

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.APPROVE")).thenReturn(true);

		PermissionConflictEntity conflict = PermissionConflictEntity.builder()
			.conflictId(1L)
			.permissionCodeA("PAYMENT.SETTLEMENT.APPROVE")
			.permissionCodeB("PAYMENT.SETTLEMENT.CREATE")
			.severity("BLOCK")
			.rationale("Maker-checker violation")
			.build();

		when(permissionConflictRepository.findByPermissionCodeAInAndPermissionCodeBIn(any(), any()))
			.thenReturn(List.of(conflict));

		assertThrows(SodConflictException.class, () -> appRoleService.bulkConfigureRolePermissions(bulkRequest, null));
	}

	@Test
	void testBulkConfigureRolePermissions_EmptyPayload_ThrowsIllegalArgumentException() {
		var bulkRequest = new BulkRolePermissionsRequest(null, null, null);
		assertThrows(IllegalArgumentException.class, () -> appRoleService.bulkConfigureRolePermissions(bulkRequest, null));
	}

	@Test
	void testBulkConfigureRolePermissions_RoleNotFound_ThrowsResourceNotFoundException() {
		var makerAssignment = new RolePermissionAssignment("UNKNOWN", List.of("ADMIN.USER.VIEW"));
		var bulkRequest = new BulkRolePermissionsRequest(List.of(makerAssignment), null, null);

		when(appRoleRepository.existsByRoleCode("UNKNOWN")).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> appRoleService.bulkConfigureRolePermissions(bulkRequest, null));
	}

	@Test
	void testBulkConfigureRolePermissions_GrantsAndRevocations() {
		var grant = new RolePermissionPair("MAKER", "PAYMENT.SETTLEMENT.CREATE");
		var revoke = new RolePermissionPair("CHECKER", "ADMIN.USER.VIEW");
		var bulkRequest = new BulkRolePermissionsRequest(null, List.of(grant), List.of(revoke));

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("MAKER", "PAYMENT.SETTLEMENT.CREATE")).thenReturn(false);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER")).thenReturn(Set.of());

		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("CHECKER", "ADMIN.USER.VIEW")).thenReturn(true);

		AppRoleEntity makerEntity = AppRoleEntity.builder().roleCode("MAKER").name("Maker").isSystem("N").build();
		AppRoleEntity checkerEntity = AppRoleEntity.builder().roleCode("CHECKER").name("Checker").isSystem("N").build();
		when(appRoleRepository.findByRoleCode("MAKER")).thenReturn(Optional.of(makerEntity));
		when(appRoleRepository.findByRoleCode("CHECKER")).thenReturn(Optional.of(checkerEntity));

		BulkRolePermissionSummary summary = appRoleService.bulkConfigureRolePermissions(bulkRequest, null);

		assertNotNull(summary);
		assertEquals(1, summary.totalPermissionsGranted());
		assertEquals(1, summary.totalPermissionsRevoked());
		verify(rolePermissionRepository).save(any(RolePermissionEntity.class));
		verify(rolePermissionRepository).deleteById(any(RolePermissionId.class));
	}

	@Test
	void testBulkConfigureRolePermissions_ActionGrant_Success() {
		var bulkRequest = BulkRolePermissionsRequest.ofAction(
			"GRANT",
			"MAKER",
			List.of("PAYMENT.SETTLEMENT.CREATE")
		);

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("MAKER", "PAYMENT.SETTLEMENT.CREATE")).thenReturn(false);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER")).thenReturn(Set.of());

		AppRoleEntity makerEntity = AppRoleEntity.builder().roleCode("MAKER").name("Maker").isSystem("N").build();
		when(appRoleRepository.findByRoleCode("MAKER")).thenReturn(Optional.of(makerEntity));

		BulkRolePermissionSummary summary = appRoleService.bulkConfigureRolePermissions(bulkRequest, null);

		assertNotNull(summary);
		assertEquals(1, summary.totalPermissionsGranted());
		assertEquals(0, summary.totalPermissionsRevoked());
		verify(rolePermissionRepository).save(any(RolePermissionEntity.class));
	}

	@Test
	void testBulkConfigureRolePermissions_ActionRevoke_Success() {
		var bulkRequest = BulkRolePermissionsRequest.ofAction(
			"REVOKE",
			"CHECKER",
			List.of("ADMIN.USER.VIEW")
		);

		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("CHECKER", "ADMIN.USER.VIEW")).thenReturn(true);

		AppRoleEntity checkerEntity = AppRoleEntity.builder().roleCode("CHECKER").name("Checker").isSystem("N").build();
		when(appRoleRepository.findByRoleCode("CHECKER")).thenReturn(Optional.of(checkerEntity));

		BulkRolePermissionSummary summary = appRoleService.bulkConfigureRolePermissions(bulkRequest, null);

		assertNotNull(summary);
		assertEquals(0, summary.totalPermissionsGranted());
		assertEquals(1, summary.totalPermissionsRevoked());
		verify(rolePermissionRepository).deleteById(any(RolePermissionId.class));
	}

	@Test
	void testBulkConfigureRolePermissions_InvalidAction_ThrowsIllegalArgumentException() {
		var bulkRequest = BulkRolePermissionsRequest.ofAction(
			"INVALID_ACTION",
			"CHECKER",
			List.of("ADMIN.USER.VIEW")
		);

		assertThrows(IllegalArgumentException.class, () -> appRoleService.bulkConfigureRolePermissions(bulkRequest, null));
	}

	@Test
	void testAssignPermissionsToRole_RecordsHistory() {
		var request = new RolePermissionRequest("MAKER", List.of("PAYMENT.SETTLEMENT.CREATE"), "ADMIN_USER");

		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);
		when(rolePermissionRepository.existsByIdRoleCodeAndIdPermissionCode("MAKER", "PAYMENT.SETTLEMENT.CREATE")).thenReturn(false);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER")).thenReturn(Set.of());

		PermissionEntity perm = PermissionEntity.builder()
			.permissionCode("PAYMENT.SETTLEMENT.CREATE")
			.module("PAYMENT")
			.resource("SETTLEMENT")
			.actionName("CREATE")
			.description("Create payment")
			.isSensitive("N")
			.build();
		when(permissionRepository.findPermissionsByRoleCode("MAKER")).thenReturn(List.of(perm));

		var response = appRoleService.assignPermissionsToRole(request, null);

		assertNotNull(response);
		verify(rolePermissionRepository).save(any(RolePermissionEntity.class));
		verify(rolePermissionHistoryRepository).save(any(RolePermissionHistoryEntity.class));
	}

	@Test
	void testRevokePermissionFromRole_RecordsHistory() {
		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);
		when(rolePermissionRepository.existsById(any(RolePermissionId.class))).thenReturn(true);

		appRoleService.revokePermissionFromRole("MAKER", "PAYMENT.SETTLEMENT.CREATE", null);

		verify(rolePermissionRepository).deleteById(any(RolePermissionId.class));
		verify(rolePermissionHistoryRepository).save(any(RolePermissionHistoryEntity.class));
	}

	@Test
	void testRevokePermissionsFromRole_RecordsHistory() {
		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);

		appRoleService.revokePermissionsFromRole("MAKER", List.of("PAYMENT.SETTLEMENT.CREATE"), null);

		verify(rolePermissionRepository).deleteByRoleCodeAndPermissionCodes("MAKER", List.of("PAYMENT.SETTLEMENT.CREATE"));
		verify(rolePermissionHistoryRepository).save(any(RolePermissionHistoryEntity.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetRolePermissionHistory_Success() {
		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);

		RolePermissionHistoryEntity historyEntity = RolePermissionHistoryEntity.builder()
			.historyId(1L)
			.roleCode("MAKER")
			.permissionCode("PAYMENT.SETTLEMENT.CREATE")
			.actionTaken("ADDED")
			.changedBy("SYSTEM")
			.changedAt(LocalDateTime.now())
			.build();

		Page<RolePermissionHistoryEntity> page = new PageImpl<>(List.of(historyEntity));
		when(rolePermissionHistoryRepository.findAll(any(Specification.class), any(Pageable.class)))
			.thenAnswer(invocation -> {
				Specification<RolePermissionHistoryEntity> spec = invocation.getArgument(0);
				if (spec != null) {
					Root<RolePermissionHistoryEntity> root = mock(Root.class);
					CriteriaQuery<?> cq = mock(CriteriaQuery.class);
					CriteriaBuilder cb = mock(CriteriaBuilder.class);
					Path<Object> path = mock(Path.class);
					Predicate pred = mock(Predicate.class);
					when(root.get(anyString())).thenReturn(path);
					when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(pred);
					when(cb.and(any(Predicate[].class))).thenReturn(pred);
					spec.toPredicate(root, cq, cb);
				}
				return page;
			});

		RolePermissionHistoryPageResponse response = appRoleService.getRolePermissionHistory(
			"MAKER", "PAYMENT.SETTLEMENT.CREATE", "ADDED", 0, 0);

		assertNotNull(response);
		assertEquals(1, response.total());
		assertEquals(1, response.history().size());
		assertEquals("ADDED", response.history().get(0).actionTaken());
	}

	@Test
	void testGetRolePermissionHistory_BlankRole_ThrowsException() {
		assertThrows(IllegalArgumentException.class, () ->
			appRoleService.getRolePermissionHistory("   ", null, null, 1, 10));
	}

	@Test
	void testGetRolePermissionHistory_RoleNotFound_ThrowsException() {
		when(appRoleRepository.existsByRoleCode("NON_EXISTING")).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () ->
			appRoleService.getRolePermissionHistory("NON_EXISTING", null, null, 1, 10));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetAllRolePermissionHistory_Success() {
		RolePermissionHistoryEntity historyEntity = RolePermissionHistoryEntity.builder()
			.historyId(1L)
			.roleCode("MAKER")
			.permissionCode("PAYMENT.SETTLEMENT.CREATE")
			.actionTaken("ADDED")
			.changedBy("SYSTEM")
			.changedAt(LocalDateTime.now())
			.build();

		Page<RolePermissionHistoryEntity> page = new PageImpl<>(List.of(historyEntity));
		when(rolePermissionHistoryRepository.findAll(any(Specification.class), any(Pageable.class)))
			.thenAnswer(invocation -> {
				Specification<RolePermissionHistoryEntity> spec = invocation.getArgument(0);
				if (spec != null) {
					CriteriaBuilder cb = mock(CriteriaBuilder.class);
					Predicate pred = mock(Predicate.class);
					when(cb.and(any(Predicate[].class))).thenReturn(pred);
					spec.toPredicate(mock(Root.class), mock(CriteriaQuery.class), cb);
				}
				return page;
			});

		RolePermissionHistoryPageResponse response = appRoleService.getAllRolePermissionHistory(
			null, null, null, 1, 20);

		assertNotNull(response);
		assertEquals(1, response.total());
		assertEquals(1, response.history().size());
	}

	@Test
	void testRevokePermissionFromRole_RoleNotFound_ThrowsException() {
		when(appRoleRepository.existsByRoleCode("UNKNOWN")).thenReturn(false);
		assertThrows(ResourceNotFoundException.class, () ->
			appRoleService.revokePermissionFromRole("UNKNOWN", "PERM_A", null));
	}

	@Test
	void testRevokePermissionFromRole_PermissionNotFound_ThrowsException() {
		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("UNKNOWN_PERM")).thenReturn(false);
		assertThrows(ResourceNotFoundException.class, () ->
			appRoleService.revokePermissionFromRole("MAKER", "UNKNOWN_PERM", null));
	}

	@Test
	void testRevokePermissionsFromRole_RoleNotFound_ThrowsException() {
		when(appRoleRepository.existsByRoleCode("UNKNOWN")).thenReturn(false);
		List<String> permissionCodes = List.of("PERM_A");
		assertThrows(ResourceNotFoundException.class, () ->
			appRoleService.revokePermissionsFromRole("UNKNOWN", permissionCodes, null));
	}

	@Test
	void testUpdateRolePermissions_WithRemovals_RecordsRemovalHistory() {
		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER")).thenReturn(Set.of("OLD_PERM", "KEEP_PERM"));
		when(permissionRepository.existsByPermissionCode("KEEP_PERM")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("NEW_PERM")).thenReturn(true);

		appRoleService.updateRolePermissions("MAKER", List.of("KEEP_PERM", "NEW_PERM"), null);

		verify(rolePermissionRepository).deleteByRoleCodeAndPermissionCodes("MAKER", List.of("OLD_PERM"));
		verify(rolePermissionRepository).save(any(RolePermissionEntity.class));
		verify(rolePermissionHistoryRepository).save(argThat(h -> "REMOVED".equals(h.getActionTaken())));
		verify(rolePermissionHistoryRepository).save(argThat(h -> "ADDED".equals(h.getActionTaken())));
	}

	@Test
	void testBulkConfigureRolePermissions_SyncAction() {
		AppRoleEntity roleEntity = AppRoleEntity.builder().roleCode("MAKER").name("Maker").build();
		BulkRolePermissionsRequest bulkReq = BulkRolePermissionsRequest.ofAction("SYNC", "MAKER", List.of("PAYMENT.SETTLEMENT.CREATE"));
		when(appRoleRepository.existsByRoleCode("MAKER")).thenReturn(true);
		when(appRoleRepository.findByRoleCode("MAKER")).thenReturn(Optional.of(roleEntity));
		when(rolePermissionRepository.findAssignedPermissionCodesByRoleCode("MAKER")).thenReturn(Set.of());
		when(permissionRepository.existsByPermissionCode("PAYMENT.SETTLEMENT.CREATE")).thenReturn(true);

		BulkRolePermissionSummary summary = appRoleService.bulkConfigureRolePermissions(bulkReq, null);
		assertNotNull(summary);
	}
}

