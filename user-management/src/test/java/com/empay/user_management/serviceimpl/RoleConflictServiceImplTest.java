package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.empay.user_management.dto.CreateRoleConflictRequest;
import com.empay.user_management.dto.RoleConflictResponse;
import com.empay.user_management.entities.RoleConflictEntity;
import com.empay.user_management.repository.AppRoleRepository;
import com.empay.user_management.repository.RoleConflictRepository;
import com.empay.user_management.repository.UserRoleRepositoryV2;

/**
 * Unit tests for the role conflict rule service.
 * Verifies validation rules, duplication checks, and user assignment constraints.
 */
@ExtendWith(MockitoExtension.class)
class RoleConflictServiceImplTest {

	@Mock
	private RoleConflictRepository roleConflictRepository;

	@Mock
	private AppRoleRepository appRoleRepository;

	@Mock
	private UserRoleRepositoryV2 userRoleRepository;

	@InjectMocks
	private RoleConflictServiceImpl roleConflictService;

	@Test
	void testGetAllRules() {
		RoleConflictEntity entity = RoleConflictEntity.builder()
			.conflictId(1L)
			.roleCodeA("CHECKER")
			.roleCodeB("MAKER")
			.severity("BLOCK")
			.rationale("Maker-checker violation")
			.build();
		when(roleConflictRepository.findAll()).thenReturn(List.of(entity));

		List<RoleConflictResponse> response = roleConflictService.getAllRules();

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals("rcr-1", response.get(0).id());
		assertEquals("BLOCK", response.get(0).severity());
	}

	@Test
	void testGetRuleById_Success() {
		RoleConflictEntity entity = RoleConflictEntity.builder()
			.conflictId(1L)
			.roleCodeA("CHECKER")
			.roleCodeB("MAKER")
			.severity("BLOCK")
			.rationale("Maker-checker violation")
			.build();
		when(roleConflictRepository.findById(1L)).thenReturn(Optional.of(entity));

		RoleConflictResponse response = roleConflictService.getRuleById(1L);

		assertNotNull(response);
		assertEquals("rcr-1", response.id());
	}

	@Test
	void testGetRuleById_NotFound_ThrowsException() {
		when(roleConflictRepository.findById(99L)).thenReturn(Optional.empty());

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.getRuleById(99L));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	private CreateRoleConflictRequest buildRequest(List<String> pair, String severity, String rationale) {
		CreateRoleConflictRequest request = new CreateRoleConflictRequest();
		request.setPair(pair);
		request.setSeverity(severity);
		request.setRationale(rationale);
		return request;
	}

	@Test
	void testCreateRule_Success() {
		CreateRoleConflictRequest request = buildRequest(
			List.of("MAKER", "CHECKER"),
			"BLOCK",
			"Maker cannot also be Checker"
		);

		when(appRoleRepository.existsById("MAKER")).thenReturn(true);
		when(appRoleRepository.existsById("CHECKER")).thenReturn(true);
		when(roleConflictRepository.existsByRoleCodeAAndRoleCodeB("CHECKER", "MAKER")).thenReturn(false);
		when(userRoleRepository.existsUserWithBothRoles("CHECKER", "MAKER")).thenReturn(false);

		RoleConflictEntity saved = RoleConflictEntity.builder()
			.conflictId(5L)
			.roleCodeA("CHECKER")
			.roleCodeB("MAKER")
			.severity("BLOCK")
			.rationale("Maker cannot also be Checker")
			.build();
		when(roleConflictRepository.saveAndFlush(any(RoleConflictEntity.class))).thenReturn(saved);

		RoleConflictResponse response = roleConflictService.createRule(request);

		assertNotNull(response);
		assertEquals("rcr-5", response.id());
		assertEquals("BLOCK", response.severity());
		verify(roleConflictRepository).saveAndFlush(any(RoleConflictEntity.class));
	}

	@Test
	void testCreateRule_SelfConflict_ThrowsBadRequest() {
		CreateRoleConflictRequest request = buildRequest(
			List.of("MAKER", "MAKER"),
			"BLOCK",
			"A role cannot conflict with itself"
		);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testCreateRule_RoleDoesNotExist_ThrowsUnprocessableEntity() {
		CreateRoleConflictRequest request = buildRequest(
			List.of("UNKNOWN_A", "MAKER"),
			"BLOCK",
			"Role does not exist"
		);

		when(appRoleRepository.existsById("UNKNOWN_A")).thenReturn(false);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
	}

	@Test
	void testCreateRule_ExistingPair_ThrowsConflict() {
		CreateRoleConflictRequest request = buildRequest(
			List.of("MAKER", "CHECKER"),
			"BLOCK",
			"Duplicate rule"
		);

		when(appRoleRepository.existsById("MAKER")).thenReturn(true);
		when(appRoleRepository.existsById("CHECKER")).thenReturn(true);
		when(roleConflictRepository.existsByRoleCodeAAndRoleCodeB("CHECKER", "MAKER")).thenReturn(true);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
	}

	@Test
	void testCreateRule_UserAlreadyAssignedBothRoles_ThrowsConflict() {
		CreateRoleConflictRequest request = buildRequest(
			List.of("MAKER", "CHECKER"),
			"BLOCK",
			"Users already have both roles"
		);

		when(appRoleRepository.existsById("MAKER")).thenReturn(true);
		when(appRoleRepository.existsById("CHECKER")).thenReturn(true);
		when(roleConflictRepository.existsByRoleCodeAAndRoleCodeB("CHECKER", "MAKER")).thenReturn(false);
		when(userRoleRepository.existsUserWithBothRoles("CHECKER", "MAKER")).thenReturn(true);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
		assertEquals("Cannot create role conflict because one or more users are already assigned both roles: CHECKER and MAKER", ex.getReason());
	}

	@Test
	void testDeleteRule_Success() {
		RoleConflictEntity entity = RoleConflictEntity.builder()
			.conflictId(1L)
			.roleCodeA("CHECKER")
			.roleCodeB("MAKER")
			.severity("BLOCK")
			.rationale("Maker-checker violation")
			.build();
		when(roleConflictRepository.findById(1L)).thenReturn(Optional.of(entity));

		roleConflictService.deleteRule(1L);

		verify(roleConflictRepository).delete(entity);
		verify(roleConflictRepository).flush();
	}

	@Test
	void testGetRuleById_InvalidId_ThrowsBadRequest() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.getRuleById(0L));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testGetRuleById_NotFound_ThrowsNotFound() {
		when(roleConflictRepository.findById(999L)).thenReturn(Optional.empty());
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.getRuleById(999L));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void testDeleteRule_InvalidId_ThrowsBadRequest() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.deleteRule(0L));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testDeleteRule_NotFound_ThrowsNotFound() {
		when(roleConflictRepository.findById(999L)).thenReturn(Optional.empty());
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.deleteRule(999L));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void testCreateRule_NullRequest_ThrowsBadRequest() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(null));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testCreateRule_InvalidPairSize_ThrowsBadRequest() {
		CreateRoleConflictRequest request = buildRequest(List.of("MAKER"), "BLOCK", "Invalid size");
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testCreateRule_RoleBNotFound_ThrowsUnprocessable() {
		CreateRoleConflictRequest request = buildRequest(List.of("MAKER", "UNKNOWN_B"), "BLOCK", "Role B missing");
		when(appRoleRepository.existsById("MAKER")).thenReturn(true);
		when(appRoleRepository.existsById("UNKNOWN_B")).thenReturn(false);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
	}

	@Test
	void testCreateRule_RoleAAlphabeticalBeforeRoleB_Success() {
		CreateRoleConflictRequest request = buildRequest(List.of("ADMIN", "MAKER"), "BLOCK", "Natural order");
		when(appRoleRepository.existsById("ADMIN")).thenReturn(true);
		when(appRoleRepository.existsById("MAKER")).thenReturn(true);
		when(roleConflictRepository.existsByRoleCodeAAndRoleCodeB("ADMIN", "MAKER")).thenReturn(false);
		when(userRoleRepository.existsUserWithBothRoles("ADMIN", "MAKER")).thenReturn(false);

		RoleConflictEntity saved = RoleConflictEntity.builder()
			.conflictId(2L)
			.roleCodeA("ADMIN")
			.roleCodeB("MAKER")
			.severity("BLOCK")
			.rationale("Natural order")
			.build();
		when(roleConflictRepository.saveAndFlush(any(RoleConflictEntity.class))).thenReturn(saved);

		RoleConflictResponse response = roleConflictService.createRule(request);
		assertNotNull(response);
		assertEquals("ADMIN", response.pair().get(0));
	}

	@Test
	void testDeleteRule_DataIntegrityViolation_ThrowsConflict() {
		RoleConflictEntity entity = RoleConflictEntity.builder()
			.conflictId(10L)
			.roleCodeA("CHECKER")
			.roleCodeB("MAKER")
			.build();
		when(roleConflictRepository.findById(10L)).thenReturn(Optional.of(entity));
		doThrow(new DataIntegrityViolationException("foreign key"))
			.when(roleConflictRepository).flush();

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.deleteRule(10L));
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
		assertTrue(ex.getReason().contains("referenced by other records"));
	}

	@Test
	void testCreateRule_DataIntegrityViolation_ThrowsConflict() {
		CreateRoleConflictRequest request = buildRequest(List.of("ADMIN", "MAKER"), "BLOCK", "Constraint test");
		when(appRoleRepository.existsById("ADMIN")).thenReturn(true);
		when(appRoleRepository.existsById("MAKER")).thenReturn(true);
		when(roleConflictRepository.existsByRoleCodeAAndRoleCodeB("ADMIN", "MAKER")).thenReturn(false);
		when(userRoleRepository.existsUserWithBothRoles("ADMIN", "MAKER")).thenReturn(false);
		when(roleConflictRepository.saveAndFlush(any(RoleConflictEntity.class)))
			.thenThrow(new DataIntegrityViolationException("unique constraint"));

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
		assertTrue(ex.getReason().contains("violates a database constraint"));
	}

	@Test
	void testCreateRule_RoleCodeBlank_ThrowsBadRequest() {
		CreateRoleConflictRequest request = buildRequest(List.of("  ", "MAKER"), "BLOCK", "Blank role");
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		assertEquals("Role codes cannot be blank", ex.getReason());
	}

	@Test
	void testCreateRule_RoleCodeExceeds40Chars_ThrowsBadRequest() {
		String longCode = "A".repeat(41);
		CreateRoleConflictRequest request = buildRequest(List.of(longCode, "MAKER"), "BLOCK", "Long role");
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		assertEquals("Role code cannot exceed 40 characters", ex.getReason());
	}

	@Test
	void testCreateRule_SeverityBlank_ThrowsBadRequest() {
		CreateRoleConflictRequest request = buildRequest(List.of("ADMIN", "MAKER"), "  ", "Blank severity");
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		assertEquals("Severity is required", ex.getReason());
	}

	@Test
	void testCreateRule_SeverityInvalid_ThrowsBadRequest() {
		CreateRoleConflictRequest request = buildRequest(List.of("ADMIN", "MAKER"), "INFO", "Invalid severity");
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		assertEquals("Severity must be BLOCK or WARN", ex.getReason());
	}

	@Test
	void testCreateRule_RationaleBlank_ThrowsBadRequest() {
		CreateRoleConflictRequest request = buildRequest(List.of("ADMIN", "MAKER"), "BLOCK", "   ");
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		assertEquals("Rationale must contain between 1 and 400 characters", ex.getReason());
	}

	@Test
	void testCreateRule_RationaleExceeds400Chars_ThrowsBadRequest() {
		String longRationale = "A".repeat(401);
		CreateRoleConflictRequest request = buildRequest(List.of("ADMIN", "MAKER"), "BLOCK", longRationale);
		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
			() -> roleConflictService.createRule(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		assertEquals("Rationale must contain between 1 and 400 characters", ex.getReason());
	}
}
