package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.empay.user_management.dto.ModuleMenuNode;
import com.empay.user_management.dto.ResourceSubmenuNode;
import com.empay.user_management.dto.UserNavigationResponse;
import com.empay.user_management.entities.PermissionEntity;
import com.empay.user_management.entities.UserPermissionOverrideEntity;
import com.empay.user_management.entities.UserRoleEntityV2;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.UserPermissionOverrideRepository;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.utils.JwtUtils;

/**
 * Unit tests for UserNavigationServiceImpl.
 * Validates dynamic hierarchy generation, Gatekeeper VIEW checks, and 3-tier menu trees.
 */
@ExtendWith(MockitoExtension.class)
class UserNavigationServiceImplTest {

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private UsersRepositoryV2 usersRepository;

	@Mock
	private UserPermissionOverrideRepository overrideRepository;

	@Mock
	private JwtUtils jwtUtils;

	@InjectMocks
	private UserNavigationServiceImpl userNavigationService;

	@Test
	void testGetNavigationForRole_TwoTierHierarchy_Success() {
		PermissionEntity viewUser = PermissionEntity.builder()
				.permissionCode("ADMIN.USER.VIEW")
				.module("ADMIN")
				.resource("USER")
				.actionName("VIEW")
				.description("View user accounts")
				.legacyRecordId(6001)
				.build();

		PermissionEntity createUser = PermissionEntity.builder()
				.permissionCode("ADMIN.USER.CREATE")
				.module("ADMIN")
				.resource("USER")
				.actionName("CREATE")
				.description("Create user account")
				.legacyRecordId(6001)
				.build();

		when(permissionRepository.findPermissionsByRoleCode("SYS_ADMIN"))
				.thenReturn(List.of(viewUser, createUser));

		UsersEntityV2 adminUser = new UsersEntityV2();
		adminUser.setUserId("SYS_ADMIN");
		adminUser.setDisplayName("System Administrator");
		when(usersRepository.findByUserId("SYS_ADMIN")).thenReturn(adminUser);

		UserNavigationResponse response = userNavigationService.getNavigationForRole("SYS_ADMIN", "SYS_ADMIN");

		assertNotNull(response);
		assertEquals("SYS_ADMIN", response.userId());
		assertEquals("System Administrator", response.displayName());
		assertEquals(List.of("SYS_ADMIN"), response.roles());
		assertEquals(1, response.menus().size());

		ModuleMenuNode adminModule = response.menus().get(0);
		assertEquals("ADMIN", adminModule.module());
		assertEquals("Admin", adminModule.title());
		assertEquals("settings", adminModule.iconName());
		assertEquals(1, adminModule.submenus().size());

		ResourceSubmenuNode userSubmenu = adminModule.submenus().get(0);
		assertEquals("USER", userSubmenu.resource());
		assertEquals("User accounts", userSubmenu.title());
		assertEquals("/admin/user", userSubmenu.routePath());
		assertEquals(6001, userSubmenu.legacyRecordId());
		assertEquals(List.of("CREATE", "VIEW"), userSubmenu.actions());
		assertTrue(userSubmenu.children().isEmpty());
	}

	@Test
	void testGetNavigationForRole_ThreeTierHierarchy_Success() {
		PermissionEntity viewPolicy = PermissionEntity.builder()
				.permissionCode("ADMIN.SECURITY.POLICY.VIEW")
				.module("ADMIN")
				.resource("SECURITY.POLICY")
				.actionName("VIEW")
				.description("View password policy configuration")
				.legacyRecordId(6010)
				.build();

		PermissionEntity updatePolicy = PermissionEntity.builder()
				.permissionCode("ADMIN.SECURITY.POLICY.UPDATE")
				.module("ADMIN")
				.resource("SECURITY.POLICY")
				.actionName("UPDATE")
				.description("Update password policies")
				.legacyRecordId(6010)
				.build();

		PermissionEntity viewSod = PermissionEntity.builder()
				.permissionCode("ADMIN.SECURITY.SOD.VIEW")
				.module("ADMIN")
				.resource("SECURITY.SOD")
				.actionName("VIEW")
				.description("View segregation of duties matrix")
				.legacyRecordId(6020)
				.build();

		when(permissionRepository.findPermissionsByRoleCode("SYS_ADMIN"))
				.thenReturn(List.of(viewPolicy, updatePolicy, viewSod));

		UserNavigationResponse response = userNavigationService.getNavigationForRole("SYS_ADMIN", "SYS_ADMIN");

		assertNotNull(response);
		assertEquals(1, response.menus().size());

		ModuleMenuNode adminModule = response.menus().get(0);
		assertEquals(1, adminModule.submenus().size());

		ResourceSubmenuNode securityFolder = adminModule.submenus().get(0);
		assertEquals("SECURITY", securityFolder.resource());
		assertEquals("Security", securityFolder.title());
		assertEquals("lock", securityFolder.iconName());
		assertEquals(2, securityFolder.children().size());

		ResourceSubmenuNode policyScreen = securityFolder.children().get(0);
		assertEquals("SECURITY.POLICY", policyScreen.resource());
		assertEquals("Password policy configuration", policyScreen.title());
		assertEquals("/admin/security/policy", policyScreen.routePath());
		assertEquals(List.of("UPDATE", "VIEW"), policyScreen.actions());

		ResourceSubmenuNode sodScreen = securityFolder.children().get(1);
		assertEquals("SECURITY.SOD", sodScreen.resource());
		assertEquals("/admin/security/sod", sodScreen.routePath());
	}

	@Test
	void testGetNavigationForRole_GatekeeperViewFilter_OmitsUnauthorizedScreens() {
		// Screen with only CREATE permission, but no VIEW permission
		PermissionEntity createOnly = PermissionEntity.builder()
				.permissionCode("ADMIN.AUDIT.CREATE")
				.module("ADMIN")
				.resource("AUDIT")
				.actionName("CREATE")
				.description("Create audit records")
				.legacyRecordId(9001)
				.build();

		when(permissionRepository.findPermissionsByRoleCode("OPERATOR"))
				.thenReturn(List.of(createOnly));

		UserNavigationResponse response = userNavigationService.getNavigationForRole("operator_1", "OPERATOR");

		assertNotNull(response);
		// Entire module is pruned because no screens have VIEW action
		assertTrue(response.menus().isEmpty());
	}

	@Test
	void testGetNavigationForRole_EmptyRolePermissions_ReturnsEmpty() {
		when(permissionRepository.findPermissionsByRoleCode("CUSTOM_ROLE"))
				.thenReturn(List.of());

		UserNavigationResponse response = userNavigationService.getNavigationForRole("custom_user", "CUSTOM_ROLE");

		assertNotNull(response);
		assertTrue(response.menus().isEmpty());
	}

	@Test
	void testGetNavigationForRole_EmptyCatalog_ReturnsEmpty() {
		when(permissionRepository.findPermissionsByRoleCode("EMPTY_ROLE"))
				.thenReturn(List.of());

		UserNavigationResponse response = userNavigationService.getNavigationForRole("empty_user", "EMPTY_ROLE");

		assertNotNull(response);
		assertTrue(response.menus().isEmpty());
	}

	@Test
	void testGetUserNavigationMenu_SysAdminUser() {
		PermissionEntity perm = PermissionEntity.builder()
				.permissionCode("PARAM.RATE.VIEW")
				.module("PARAM")
				.resource("RATE")
				.actionName("VIEW")
				.description("View rate matrix")
				.legacyRecordId(5001)
				.build();

		when(permissionRepository.findPermissionsByRoleCode("SYS_ADMIN"))
				.thenReturn(List.of(perm));

		UserNavigationResponse response = userNavigationService.getUserNavigationMenu("SYS_ADMIN");

		assertNotNull(response);
		assertEquals("SYS_ADMIN", response.userId());
		assertEquals(1, response.menus().size());
		assertEquals("PARAM", response.menus().get(0).module());
	}

	@Test
	void testGetUserNavigationMenu_UnknownUser_ReturnsEmpty() {
		when(usersRepository.findByUserId("unknown_user")).thenReturn(null);

		UserNavigationResponse response = userNavigationService.getUserNavigationMenu("unknown_user");

		assertNotNull(response);
		assertEquals("unknown_user", response.userId());
		assertTrue(response.menus().isEmpty());
	}

	@Test
	void testGetUserNavigationMenu_WithResolvedUserRole() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("cashier_01");
		user.setDisplayName("Front Cashier");
		UserRoleEntityV2 role = new UserRoleEntityV2();
		role.setRoleCode("CASHIER_ROLE");
		user.setUserRoles(List.of(role));

		PermissionEntity perm = PermissionEntity.builder()
				.permissionCode("POS.BILLING.VIEW")
				.module("POS")
				.resource("BILLING")
				.actionName("VIEW")
				.description("View billing screen")
				.legacyRecordId(7001)
				.build();

		when(usersRepository.findByUserId("cashier_01")).thenReturn(user);
		when(permissionRepository.findPermissionsByRoleCode("CASHIER_ROLE")).thenReturn(List.of(perm));

		UserNavigationResponse response = userNavigationService.getUserNavigationMenu("cashier_01");

		assertNotNull(response);
		assertEquals("cashier_01", response.userId());
		assertEquals("Front Cashier", response.displayName());
		assertEquals(List.of("CASHIER_ROLE"), response.roles());
		assertEquals(1, response.menus().size());
		assertEquals("POS", response.menus().get(0).module());
	}

	@Test
	void testResolveDisplayName_FromUsersRepository() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setUserId("john.doe");
		user.setDisplayName("John Doe");

		when(usersRepository.findByUserId("john.doe")).thenReturn(user);
		when(permissionRepository.findPermissionsByRoleCode("SYS_ADMIN")).thenReturn(List.of());

		UserNavigationResponse response = userNavigationService.getNavigationForRole("john.doe", "SYS_ADMIN");

		assertNotNull(response);
		assertEquals("John Doe", response.displayName());
	}

	@Test
	void testGetUserNavigationMenu_NullOrBlank_ReturnsEmpty() {
		UserNavigationResponse resNull = userNavigationService.getUserNavigationMenu(null);
		assertNotNull(resNull);
		assertTrue(resNull.menus().isEmpty());

		UserNavigationResponse resBlank = userNavigationService.getUserNavigationMenu("   ");
		assertNotNull(resBlank);
		assertTrue(resBlank.menus().isEmpty());
	}

	@Test
	void testGetUserNavigationMenu_InactiveUser_ReturnsEmpty() {
		UsersEntityV2 inactive = new UsersEntityV2();
		inactive.setUserId("suspended_user");
		inactive.setStatus("SUSPENDED");
		inactive.setDisplayName("Suspended Officer");

		when(usersRepository.findByUserId("suspended_user")).thenReturn(inactive);

		UserNavigationResponse response = userNavigationService.getUserNavigationMenu("suspended_user");
		assertNotNull(response);
		assertTrue(response.menus().isEmpty());
	}

	@Test
	void testGetUserNavigationMenu_JwtTokenResolution() {
		String eyJwt = "eyJhbGciOiJIUzI1NiJ9.test";
		when(jwtUtils.getUserIdFromToken(eyJwt)).thenReturn("token_user");
		when(usersRepository.findByUserId("token_user")).thenReturn(null);
		when(permissionRepository.findPermissionsByRoleCode("token_user")).thenReturn(List.of());

		UserNavigationResponse response = userNavigationService.getUserNavigationMenu(eyJwt);
		assertNotNull(response);
		assertEquals("token_user", response.userId());

		String nonEyJwt = "opaque.token.val";
		when(jwtUtils.validateToken(nonEyJwt)).thenReturn(true);
		when(jwtUtils.getUserIdFromToken(nonEyJwt)).thenReturn("opaque_user");
		when(usersRepository.findByUserId("opaque_user")).thenReturn(null);
		when(permissionRepository.findPermissionsByRoleCode("opaque_user")).thenReturn(List.of());

		UserNavigationResponse response2 = userNavigationService.getUserNavigationMenu(nonEyJwt);
		assertNotNull(response2);
		assertEquals("opaque_user", response2.userId());
	}

	@Test
	void testGetUserNavigationMenu_WithOverrides_GrantAndDeny() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setSerialNumber(101);
		user.setUserId("auditor_01");
		user.setDisplayName("Auditor One");
		user.setStatus("ACTIVE");

		UserRoleEntityV2 role = new UserRoleEntityV2();
		role.setRoleCode("AUDITOR");
		user.setUserRoles(List.of(role));

		PermissionEntity basePerm = PermissionEntity.builder()
				.permissionCode("AUDIT.LOG.VIEW")
				.module("AUDIT")
				.resource("LOG")
				.actionName("VIEW")
				.description("View audit logs")
				.build();

		PermissionEntity deniedPerm = PermissionEntity.builder()
				.permissionCode("AUDIT.EXPORT.VIEW")
				.module("AUDIT")
				.resource("EXPORT")
				.actionName("VIEW")
				.description("Export audit logs")
				.build();

		when(usersRepository.findByUserId("auditor_01")).thenReturn(user);
		when(permissionRepository.findPermissionsByRoleCode("AUDITOR"))
				.thenReturn(new java.util.ArrayList<>(List.of(basePerm, deniedPerm)));

		PermissionEntity grantedPerm = PermissionEntity.builder()
				.permissionCode("ADMIN.CONFIG.VIEW")
				.module("ADMIN")
				.resource("CONFIG")
				.actionName("VIEW")
				.description("View admin configs")
				.build();

		UserPermissionOverrideEntity grantOverride = new UserPermissionOverrideEntity();
		grantOverride.setUserSerialNo(101L);
		grantOverride.setPermissionCode("ADMIN.CONFIG.VIEW");
		grantOverride.setEffect("GRANT");
		grantOverride.setExpiresAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(2));

		UserPermissionOverrideEntity denyOverride = new UserPermissionOverrideEntity();
		denyOverride.setUserSerialNo(101L);
		denyOverride.setPermissionCode("AUDIT.EXPORT.VIEW");
		denyOverride.setEffect("DENY");
		denyOverride.setExpiresAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(2));

		UserPermissionOverrideEntity expiredOverride = new UserPermissionOverrideEntity();
		expiredOverride.setUserSerialNo(101L);
		expiredOverride.setPermissionCode("SOME.EXPIRED.VIEW");
		expiredOverride.setEffect("GRANT");
		expiredOverride.setExpiresAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(1));

		when(overrideRepository.findByUserSerialNo(101L))
				.thenReturn(List.of(grantOverride, denyOverride, expiredOverride));
		when(permissionRepository.findByPermissionCode("ADMIN.CONFIG.VIEW"))
				.thenReturn(java.util.Optional.of(grantedPerm));

		UserNavigationResponse response = userNavigationService.getUserNavigationMenu("auditor_01");
		assertNotNull(response);

		// DENY override removed AUDIT.EXPORT.VIEW, GRANT override added ADMIN.CONFIG.VIEW
		boolean hasDenied = response.menus().stream()
				.flatMap(m -> m.submenus().stream())
				.anyMatch(s -> "EXPORT".equalsIgnoreCase(s.resource()));
		assertFalse(hasDenied);

		boolean hasGranted = response.menus().stream()
				.anyMatch(m -> "ADMIN".equalsIgnoreCase(m.module()));
		assertTrue(hasGranted);
	}
}

