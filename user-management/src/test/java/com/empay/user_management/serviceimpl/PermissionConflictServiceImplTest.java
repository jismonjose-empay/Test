package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.empay.user_management.dto.PermissionConflictRequest;
import com.empay.user_management.dto.PermissionConflictResponse;
import com.empay.user_management.entities.PermissionConflictEntity;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.PermissionConflictRepository;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.RoleConflictRepository;
import com.empay.user_management.repository.RolePermissionRepository;

/**
 * Unit tests for permission conflict service implementation.
 * Verifies validation rules, self-conflict checks, and repository interactions.
 */
@ExtendWith(MockitoExtension.class)
class PermissionConflictServiceImplTest {

	@Mock
	private PermissionConflictRepository permissionConflictRepository;

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private RoleConflictRepository roleConflictRepository;

	@Mock
	private RolePermissionRepository rolePermissionRepository;

	@InjectMocks
	private PermissionConflictServiceImpl permissionConflictService;

	private PermissionConflictRequest createRequest(String codeA, String codeB, String severity, String rationale) {
		PermissionConflictRequest request = new PermissionConflictRequest();
		request.setPermissionCodeA(codeA);
		request.setPermissionCodeB(codeB);
		request.setSeverity(severity);
		request.setRationale(rationale);
		return request;
	}

	@Test
	void testCreatePermissionConflict_Success() {
		PermissionConflictRequest request = createRequest("USER.CREATE", "USER.DELETE", "BLOCK", "Separation of duties");

		when(permissionRepository.existsByPermissionCode("USER.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("USER.DELETE")).thenReturn(true);
		when(permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB("USER.CREATE", "USER.DELETE")).thenReturn(false);
		when(rolePermissionRepository.existsRoleWithBothPermissions("USER.CREATE", "USER.DELETE")).thenReturn(false);

		PermissionConflictEntity saved = PermissionConflictEntity.builder()
			.conflictId(1L)
			.permissionCodeA("USER.CREATE")
			.permissionCodeB("USER.DELETE")
			.severity("BLOCK")
			.rationale("Separation of duties")
			.build();
		when(permissionConflictRepository.saveAndFlush(any(PermissionConflictEntity.class))).thenReturn(saved);

		PermissionConflictResponse response = permissionConflictService.createPermissionConflict(request);

		assertNotNull(response);
		assertEquals("USER.CREATE", response.getPermissionCodeA());
		assertEquals("USER.DELETE", response.getPermissionCodeB());
		verify(permissionConflictRepository).saveAndFlush(any(PermissionConflictEntity.class));
	}

	@Test
	void testCreatePermissionConflict_SamePermission_ThrowsIllegalArgument() {
		PermissionConflictRequest request = createRequest("USER.CREATE", "USER.CREATE", "BLOCK", "Separation of duties");

		assertThrows(IllegalArgumentException.class, () ->
			permissionConflictService.createPermissionConflict(request));
	}

	@Test
	void testCreatePermissionConflict_PermissionNotFound_ThrowsResourceNotFound() {
		PermissionConflictRequest request = createRequest("USER.CREATE", "USER.DELETE", "BLOCK", "Separation of duties");

		when(permissionRepository.existsByPermissionCode("USER.CREATE")).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () ->
			permissionConflictService.createPermissionConflict(request));
	}

	@Test
	void testCreatePermissionConflict_AlreadyExists_ThrowsConflict() {
		PermissionConflictRequest request = createRequest("USER.CREATE", "USER.DELETE", "BLOCK", "Separation of duties");

		when(permissionRepository.existsByPermissionCode("USER.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("USER.DELETE")).thenReturn(true);
		when(permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB("USER.CREATE", "USER.DELETE")).thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class, () ->
			permissionConflictService.createPermissionConflict(request));
	}

	@Test
	void testCreatePermissionConflict_BothAssignedToSameRole_ThrowsIllegalArgument() {
		PermissionConflictRequest request = createRequest("USER.CREATE", "USER.DELETE", "BLOCK", "Separation of duties");

		when(permissionRepository.existsByPermissionCode("USER.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("USER.DELETE")).thenReturn(true);
		when(permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB("USER.CREATE", "USER.DELETE")).thenReturn(false);
		when(rolePermissionRepository.existsRoleWithBothPermissions("USER.CREATE", "USER.DELETE")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () ->
			permissionConflictService.createPermissionConflict(request));
	}

	@Test
	void testGetAllPermissionConflicts() {
		PermissionConflictEntity entity = PermissionConflictEntity.builder()
			.conflictId(1L)
			.permissionCodeA("USER.CREATE")
			.permissionCodeB("USER.DELETE")
			.severity("BLOCK")
			.rationale("SoD")
			.build();
		when(permissionConflictRepository.findAll()).thenReturn(List.of(entity));

		List<PermissionConflictResponse> result = permissionConflictService.getAllPermissionConflicts();
		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	void testDeletePermissionConflict_Success() {
		PermissionConflictEntity entity = PermissionConflictEntity.builder()
			.conflictId(1L)
			.permissionCodeA("USER.CREATE")
			.permissionCodeB("USER.DELETE")
			.build();
		when(permissionConflictRepository.findById(1L)).thenReturn(Optional.of(entity));

		permissionConflictService.deletePermissionConflict(1L);

		verify(permissionConflictRepository).delete(entity);
	}

	@Test
	void testCreatePermissionConflict_CodeAGreaterThanCodeB_SortedProperly() {
		PermissionConflictRequest request = createRequest("USER.DELETE", "USER.CREATE", "BLOCK", "Separation of duties");

		when(permissionRepository.existsByPermissionCode("USER.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("USER.DELETE")).thenReturn(true);
		when(permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB("USER.CREATE", "USER.DELETE")).thenReturn(false);
		when(rolePermissionRepository.existsRoleWithBothPermissions("USER.CREATE", "USER.DELETE")).thenReturn(false);

		PermissionConflictEntity saved = PermissionConflictEntity.builder()
			.conflictId(2L)
			.permissionCodeA("USER.CREATE")
			.permissionCodeB("USER.DELETE")
			.severity("BLOCK")
			.rationale("Separation of duties")
			.build();
		when(permissionConflictRepository.saveAndFlush(any(PermissionConflictEntity.class))).thenReturn(saved);

		PermissionConflictResponse response = permissionConflictService.createPermissionConflict(request);
		assertNotNull(response);
		assertEquals("USER.CREATE", response.getPermissionCodeA());
	}

	@Test
	void testCreatePermissionConflict_PermissionBNotFound_ThrowsResourceNotFound() {
		PermissionConflictRequest request = createRequest("USER.CREATE", "USER.DELETE", "BLOCK", "Separation of duties");

		when(permissionRepository.existsByPermissionCode("USER.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("USER.DELETE")).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () ->
			permissionConflictService.createPermissionConflict(request));
	}

	@Test
	void testCreatePermissionConflict_WithDerivedRoleConflict_Success() {
		PermissionConflictRequest request = createRequest("USER.CREATE", "USER.DELETE", "WARN", "Separation of duties");
		request.setDerivedFromRoleConflict(10L);

		when(permissionRepository.existsByPermissionCode("USER.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("USER.DELETE")).thenReturn(true);
		when(roleConflictRepository.existsById(10L)).thenReturn(true);
		when(permissionConflictRepository.existsByPermissionCodeAAndPermissionCodeB("USER.CREATE", "USER.DELETE")).thenReturn(false);
		when(rolePermissionRepository.existsRoleWithBothPermissions("USER.CREATE", "USER.DELETE")).thenReturn(false);

		PermissionConflictEntity saved = PermissionConflictEntity.builder()
			.conflictId(3L)
			.permissionCodeA("USER.CREATE")
			.permissionCodeB("USER.DELETE")
			.severity("WARN")
			.rationale("Separation of duties")
			.derivedFromRoleConflict(10L)
			.build();
		when(permissionConflictRepository.saveAndFlush(any(PermissionConflictEntity.class))).thenReturn(saved);

		PermissionConflictResponse response = permissionConflictService.createPermissionConflict(request);
		assertNotNull(response);
		assertEquals(10L, response.getDerivedFromRoleConflict());
	}

	@Test
	void testCreatePermissionConflict_DerivedRoleConflictNotFound_ThrowsResourceNotFound() {
		PermissionConflictRequest request = createRequest("USER.CREATE", "USER.DELETE", "BLOCK", "Separation of duties");
		request.setDerivedFromRoleConflict(99L);

		when(permissionRepository.existsByPermissionCode("USER.CREATE")).thenReturn(true);
		when(permissionRepository.existsByPermissionCode("USER.DELETE")).thenReturn(true);
		when(roleConflictRepository.existsById(99L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () ->
			permissionConflictService.createPermissionConflict(request));
	}

	@Test
	void testDeletePermissionConflict_InvalidId_ThrowsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () ->
			permissionConflictService.deletePermissionConflict(0L));
	}

	@Test
	void testDeletePermissionConflict_NotFound_ThrowsResourceNotFound() {
		when(permissionConflictRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () ->
			permissionConflictService.deletePermissionConflict(999L));
	}
}
