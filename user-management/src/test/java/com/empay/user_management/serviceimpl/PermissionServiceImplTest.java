package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.empay.user_management.dto.PermissionResponse;
import com.empay.user_management.entities.PermissionEntity;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.RolePermissionRepository;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private RolePermissionRepository rolePermissionRepository;

	@InjectMocks
	private PermissionServiceImpl permissionService;

	private PermissionEntity perm1;
	private PermissionEntity perm2;

	@BeforeEach
	void setUp() {
		perm1 = PermissionEntity.builder()
			.permissionCode("ADMIN.USER.CREATE")
			.module("ADMIN")
			.resource("USER")
			.actionName("CREATE")
			.description("Create a user account")
			.isSensitive("Y")
			.legacyRecordId(null)
			.build();

		perm2 = PermissionEntity.builder()
			.permissionCode("PAYMENT.SETTLEMENT.VIEW")
			.module("PAYMENT")
			.resource("SETTLEMENT")
			.actionName("VIEW")
			.description("View settlement instructions")
			.isSensitive("N")
			.legacyRecordId(null)
			.build();
	}

	@Test
	void testGetAllPermissions_NoFilter() {
		when(permissionRepository.findAllByOrderByModuleAscResourceAscActionNameAsc()).thenReturn(List.of(perm1, perm2));

		List<PermissionResponse> list = permissionService.getAllPermissions(null);

		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals("ADMIN.USER.CREATE", list.getFirst().permissionCode());
		assertTrue(list.getFirst().isSensitive());
	}

	@Test
	void testGetAllPermissions_WithModuleFilter() {
		when(permissionRepository.findByModuleOrderByResourceAscActionNameAsc("ADMIN")).thenReturn(List.of(perm1));

		List<PermissionResponse> list = permissionService.getAllPermissions("ADMIN");

		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("ADMIN.USER.CREATE", list.getFirst().permissionCode());
	}

	@Test
	void testGetPermissionByCode_Success() {
		when(permissionRepository.findByPermissionCode("ADMIN.USER.CREATE")).thenReturn(Optional.of(perm1));

		PermissionResponse res = permissionService.getPermissionByCode("ADMIN.USER.CREATE");

		assertNotNull(res);
		assertEquals("ADMIN.USER.CREATE", res.permissionCode());
		assertEquals("ADMIN", res.module());
		assertTrue(res.isSensitive());
	}

	@Test
	void testGetPermissionByCode_NotFound() {
		when(permissionRepository.findByPermissionCode("INVALID")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> permissionService.getPermissionByCode("INVALID"));
	}

	@Test
	void testGetAllModules() {
		when(permissionRepository.findDistinctModules()).thenReturn(List.of("ADMIN", "PAYMENT"));

		List<String> modules = permissionService.getAllModules();

		assertNotNull(modules);
		assertEquals(2, modules.size());
		assertEquals("ADMIN", modules.get(0));
		assertEquals("PAYMENT", modules.get(1));
	}

	@Test
	void testDeletePermission_Success() {
		when(permissionRepository.findByPermissionCode("ADMIN.USER.CREATE")).thenReturn(Optional.of(perm1));
		when(rolePermissionRepository.existsByIdPermissionCode("ADMIN.USER.CREATE")).thenReturn(false);

		permissionService.deletePermission("ADMIN.USER.CREATE");

		verify(permissionRepository).delete(perm1);
	}

	@Test
	void testDeletePermission_HasRolePermissionsThrowsException() {
		when(permissionRepository.findByPermissionCode("ADMIN.USER.CREATE")).thenReturn(Optional.of(perm1));
		when(rolePermissionRepository.existsByIdPermissionCode("ADMIN.USER.CREATE")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> permissionService.deletePermission("ADMIN.USER.CREATE"));
	}

	@Test
	void testDeletePermission_NotFound() {
		when(permissionRepository.findByPermissionCode("INVALID")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> permissionService.deletePermission("INVALID"));
	}
}
