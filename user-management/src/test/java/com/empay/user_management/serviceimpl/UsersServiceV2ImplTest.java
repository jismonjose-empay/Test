package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.empay.user_management.dto.UserStatusHistoryVo;
import com.empay.user_management.entities.RoleConflictEntity;
import com.empay.user_management.entities.UserCredentialEntity;
import com.empay.user_management.entities.UserRoleEntityV2;
import com.empay.user_management.entities.UserStatusHistoryEntity;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.RoleConflictRepository;
import com.empay.user_management.repository.UserCredentialRepository;
import com.empay.user_management.repository.UserRoleRepositoryV2;
import com.empay.user_management.repository.UserStatusHistoryRepository;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.valueobjects.UsersVOV2;

@ExtendWith(MockitoExtension.class)
class UsersServiceV2ImplTest {

	@Mock
	private UsersRepositoryV2 usersRepositoryV2;

	@Mock
	private UserRoleRepositoryV2 userRoleRepository;

	@Mock
	private UserStatusHistoryRepository userStatusHistoryRepository;

	@Mock
	private UserCredentialRepository userCredentialRepository;

	@Mock
	private RoleConflictRepository roleConflictRepository;

	@InjectMocks
	private UsersServiceV2Impl usersService;

	private UsersEntityV2 buildEntity(int serial, String userId, String email, String status) {
		UsersEntityV2 entity = new UsersEntityV2();
		entity.setSerialNumber(serial);
		entity.setUserId(userId);
		entity.setDisplayName("Test User " + userId);
		entity.setEmail(email);
		entity.setStatus(status);
		entity.setInstitutionCode("001");
		entity.setIsBreakGlass('N');
		entity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
		return entity;
	}

	private UsersVOV2 buildVo(String userId, String email, List<String> roles, Character isBreakGlass) {
		UsersVOV2 vo = new UsersVOV2();
		vo.setUserId(userId);
		vo.setDisplayName("Test User " + userId);
		vo.setEmail(email);
		vo.setRoleCodes(roles);
		vo.setIsBreakGlass(isBreakGlass);
		vo.setInsCode("001");
		return vo;
	}

	@Test
	@DisplayName("Should soft delete user by updating status to CLOSED and recording status history")
	void testDeleteUser_SoftDeleteSuccess() {
		String userId = "testuser";
		UsersEntityV2 user = buildEntity(101, userId, "test@empay.com", "ACTIVE");
		when(usersRepositoryV2.findByUserId(userId)).thenReturn(user);

		ResponseEntity<Void> response = usersService.deleteUser(userId);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		assertEquals("CLOSED", user.getStatus());
		assertNotNull(user.getUpdatedAt());

		verify(usersRepositoryV2).save(user);

		ArgumentCaptor<UserStatusHistoryEntity> historyCaptor = ArgumentCaptor.forClass(UserStatusHistoryEntity.class);
		verify(userStatusHistoryRepository).save(historyCaptor.capture());

		UserStatusHistoryEntity history = historyCaptor.getValue();
		assertEquals(101, history.getUserSerialNo());
		assertEquals("ACTIVE", history.getFromStatus());
		assertEquals("CLOSED", history.getToStatus());
		assertEquals("Soft delete user", history.getReason());
	}

	@Test
	@DisplayName("Should throw ResourceNotFoundException when user to delete does not exist")
	void testDeleteUser_NotFoundThrowsException() {
		when(usersRepositoryV2.findByUserId("unknown")).thenReturn(null);
		assertThrows(ResourceNotFoundException.class, () -> usersService.deleteUser("unknown"));
	}

	@Test
	@DisplayName("getUser returns user details when user exists")
	void testGetUser_Success() {
		UsersEntityV2 user = buildEntity(101, "johndoe", "john@empay.com", "ACTIVE");
		when(usersRepositoryV2.findByUserId("johndoe")).thenReturn(user);
		when(userStatusHistoryRepository.findByUserSerialNo(101)).thenReturn(List.of());

		ResponseEntity<Object> response = usersService.getUser("johndoe");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().toString().contains("johndoe"));
	}

	@Test
	@DisplayName("getUser throws ResourceNotFoundException when user does not exist")
	void testGetUser_NotFound() {
		when(usersRepositoryV2.findByUserId("notfound")).thenReturn(null);
		assertThrows(ResourceNotFoundException.class, () -> usersService.getUser("notfound"));
	}

	@Test
	@DisplayName("addUser returns 409 Conflict when userId already exists")
	void testAddUser_DuplicateUserId() {
		UsersVOV2 vo = buildVo("existingUser", "new@empay.com", List.of("MAKER"), 'N');
		when(usersRepositoryV2.findByUserId("existingUser")).thenReturn(new UsersEntityV2());

		ResponseEntity<Object> response = usersService.addUser(vo);
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("User already exists with user ID"));
	}

	@Test
	@DisplayName("addUser returns 409 Conflict when email already exists")
	void testAddUser_DuplicateEmail() {
		UsersVOV2 vo = buildVo("newUser", "existing@empay.com", List.of("MAKER"), 'N');
		when(usersRepositoryV2.findByUserId("newUser")).thenReturn(null);
		when(usersRepositoryV2.findByEmail("existing@empay.com")).thenReturn(new UsersEntityV2());

		ResponseEntity<Object> response = usersService.addUser(vo);
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("User already exists with email ID"));
	}

	@Test
	@DisplayName("addUser returns 400 Bad Request when non-break-glass user has multiple roles")
	void testAddUser_MultipleRolesForNormalUser() {
		UsersVOV2 vo = buildVo("newUser", "new@empay.com", List.of("MAKER", "CHECKER"), 'N');
		when(usersRepositoryV2.findByUserId("newUser")).thenReturn(null);
		when(usersRepositoryV2.findByEmail("new@empay.com")).thenReturn(null);

		ResponseEntity<Object> response = usersService.addUser(vo);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("Multiple roles are not allowed for normal users"));
	}

	@Test
	@DisplayName("addUser returns 409 Conflict when SOD role conflict detected")
	void testAddUser_RoleConflict() {
		UsersVOV2 vo = buildVo("bgUser", "bg@empay.com", List.of("MAKER", "CHECKER"), 'Y');
		when(usersRepositoryV2.findByUserId("bgUser")).thenReturn(null);
		when(usersRepositoryV2.findByEmail("bg@empay.com")).thenReturn(null);

		RoleConflictEntity conflict = new RoleConflictEntity();
		conflict.setRoleCodeA("MAKER");
		conflict.setRoleCodeB("CHECKER");
		when(roleConflictRepository.findConflictsByRoles(vo.getRoleCodes())).thenReturn(List.of(conflict));

		ResponseEntity<Object> response = usersService.addUser(vo);
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("Role conflict detected"));
	}

	@Test
	@DisplayName("addUser creates user, credentials, and roles successfully")
	void testAddUser_Success() {
		UsersVOV2 vo = buildVo("newUser", "new@empay.com", List.of("MAKER"), 'N');
		when(usersRepositoryV2.findByUserId("newUser")).thenReturn(null);
		when(usersRepositoryV2.findByEmail("new@empay.com")).thenReturn(null);
		when(roleConflictRepository.findConflictsByRoles(vo.getRoleCodes())).thenReturn(List.of());

		UsersEntityV2 saved = buildEntity(201, "newUser", "new@empay.com", "ACTIVE");
		when(usersRepositoryV2.save(any(UsersEntityV2.class))).thenReturn(saved);

		ResponseEntity<Object> response = usersService.addUser(vo);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		verify(userCredentialRepository).save(any(UserCredentialEntity.class));
		verify(userRoleRepository).saveAll(any());
	}

	@Test
	@DisplayName("updateUser throws ResourceNotFoundException when user does not exist")
	void testUpdateUser_NotFound() {
		UsersVOV2 vo = buildVo("unknown", "unk@empay.com", List.of("MAKER"), 'N');
		when(usersRepositoryV2.findByUserId("unknown")).thenReturn(null);

		assertThrows(ResourceNotFoundException.class, () -> usersService.updateUser(vo, "unknown"));
	}

	@Test
	@DisplayName("updateUser rejects multiple roles for non-break-glass user")
	void testUpdateUser_MultipleRolesRejected() {
		UsersEntityV2 existing = buildEntity(101, "user1", "user1@empay.com", "ACTIVE");
		when(usersRepositoryV2.findByUserId("user1")).thenReturn(existing);

		UsersVOV2 vo = buildVo("user1", "user1@empay.com", List.of("MAKER", "CHECKER"), 'N');
		ResponseEntity<Object> response = usersService.updateUser(vo, "user1");

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	@DisplayName("updateUser updates user details and role diffs")
	void testUpdateUser_SuccessWithRoleChanges() {
		UsersEntityV2 existing = buildEntity(101, "user1", "old@empay.com", "ACTIVE");
		when(usersRepositoryV2.findByUserId("user1")).thenReturn(existing);

		UserRoleEntityV2 oldRole = new UserRoleEntityV2();
		oldRole.setSerialNumber(101);
		oldRole.setRoleCode("OLD_ROLE");
		when(userRoleRepository.findBySerialNumber(101)).thenReturn(new ArrayList<>(List.of(oldRole)));

		UsersVOV2 updateVo = buildVo("user1", "new@empay.com", List.of("NEW_ROLE"), 'N');
		updateVo.setDisplayName("Updated Name");

		ResponseEntity<Object> response = usersService.updateUser(updateVo, "user1");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(usersRepositoryV2).save(existing);
		verify(userRoleRepository).deleteAll(any());
		verify(userRoleRepository).saveAll(any());
		verify(userStatusHistoryRepository).save(any(UserStatusHistoryEntity.class));
	}

	@Test
	@DisplayName("suspendUser suspends an active user and records reason")
	void testSuspendUser_SuspendActive() {
		UsersEntityV2 user = buildEntity(101, "user1", "user1@empay.com", "ACTIVE");
		when(usersRepositoryV2.findByUserIdAndStatus("user1", "ACTIVE")).thenReturn(user);

		String request = "{\"reason\": \"Suspicious activity\"}";
		ResponseEntity<Object> response = usersService.suspendUser("user1", "SUSPENDED", request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("SUSPENDED", user.getStatus());
		verify(usersRepositoryV2).save(user);
		verify(userStatusHistoryRepository).save(any(UserStatusHistoryEntity.class));
	}

	@Test
	@DisplayName("suspendUser activates a suspended user")
	void testSuspendUser_ActivateSuspended() {
		UsersEntityV2 user = buildEntity(101, "user1", "user1@empay.com", "SUSPENDED");
		when(usersRepositoryV2.findByUserIdAndStatusIn(any(), any())).thenReturn(user);

		String request = "{\"reason\": \"Re-enabled by admin\"}";
		ResponseEntity<Object> response = usersService.suspendUser("user1", "ACTIVE", request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("ACTIVE", user.getStatus());
		verify(usersRepositoryV2).save(user);
	}

	@Test
	@DisplayName("suspendUser closes an account and removes assigned roles")
	void testSuspendUser_CloseAccountDeletesRoles() {
		UsersEntityV2 user = buildEntity(101, "user1", "user1@empay.com", "ACTIVE");
		when(usersRepositoryV2.findByUserIdAndStatus("user1", "ACTIVE")).thenReturn(user);

		UserRoleEntityV2 role = new UserRoleEntityV2();
		role.setSerialNumber(101);
		role.setRoleCode("MAKER");
		when(userRoleRepository.findBySerialNumber(101)).thenReturn(List.of(role));

		String request = "{\"reason\": \"Employee exit\"}";
		ResponseEntity<Object> response = usersService.suspendUser("user1", "CLOSED", request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("CLOSED", user.getStatus());
		verify(userRoleRepository).deleteAll(any());
	}

	@Test
	@DisplayName("suspendUser throws ResourceNotFoundException when user in required status not found")
	void testSuspendUser_NotFound() {
		when(usersRepositoryV2.findByUserIdAndStatus("missing", "ACTIVE")).thenReturn(null);
		assertThrows(ResourceNotFoundException.class, () -> usersService.suspendUser("missing", "SUSPENDED", "{}"));
	}

	@Test
	@DisplayName("getUsers retrieves paged users with specification")
	@SuppressWarnings("unchecked")
	void testGetUsers_Paged() {
		UsersEntityV2 user = buildEntity(101, "u1", "u1@empay.com", "ACTIVE");
		when(usersRepositoryV2.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(user)));

		Object result = usersService.getUsers("u1", "ACTIVE", "MAKER", 1, 10);
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?, ?> map = (Map<?, ?>) result;
		assertEquals(1L, map.get("total"));
		assertEquals(1, map.get("page"));
		assertEquals(10, map.get("pageSize"));
	}

	@Test
	@DisplayName("getUsersStats calculates active, suspended, and closed percentages")
	void testGetUsersStats_Success() {
		UsersEntityV2 active = buildEntity(1, "a", "a@empay.com", "ACTIVE");
		UsersEntityV2 suspended = buildEntity(2, "b", "b@empay.com", "SUSPENDED");
		UsersEntityV2 closed = buildEntity(3, "c", "c@empay.com", "CLOSED");

		when(usersRepositoryV2.findAll()).thenReturn(List.of(active, suspended, closed));

		Map<String, Object> stats = usersService.getUsersStats();
		assertNotNull(stats);
		assertEquals(3L, stats.get("totalUsers"));
		assertEquals(1L, stats.get("activeUsers"));
		assertEquals(1L, stats.get("suspendedUsers"));
		assertEquals(1L, stats.get("closedUsers"));
		assertEquals(33L, stats.get("activePercentage"));
	}

	@Test
	@DisplayName("getUsersStats throws ResourceNotFoundException when no users found")
	void testGetUsersStats_EmptyThrows() {
		when(usersRepositoryV2.findAll()).thenReturn(List.of());
		assertThrows(ResourceNotFoundException.class, () -> usersService.getUsersStats());
	}

	@Test
	@DisplayName("getUserStatusHistory maps entity list to DTO list")
	void testGetUserStatusHistory() {
		UserStatusHistoryEntity history = new UserStatusHistoryEntity();
		history.setHistoryId(1);
		history.setUserSerialNo(101);
		history.setFromStatus("ACTIVE");
		history.setToStatus("SUSPENDED");
		history.setReason("Test reason");
		history.setChangedBy("ADMIN");
		history.setChangedAt(LocalDateTime.of(2026, 11, 1, 10, 30));

		when(userStatusHistoryRepository.findByUserSerialNo(101)).thenReturn(List.of(history));

		List<UserStatusHistoryVo> list = usersService.getUserStatusHistory(101);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("01/11/2026 10:30", list.get(0).getChangedAt());
		assertEquals("ACTIVE", list.get(0).getFromStatus());
		assertEquals("SUSPENDED", list.get(0).getToStatus());
	}
}
