package com.empay.mars_gateway.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserManagementPermissionUtilsTest {

	@Test
	void resolvePermissionCode_usersEndpoints() {
		assertEquals(UserManagementPermissionUtils.UM_USERS_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/users/v2/get-users"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_CREATE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/users/v2/add-user"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_EDIT,
				UserManagementPermissionUtils.resolvePermissionCode("PUT", "/api/users/v2/add-user/usr1"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_EDIT,
				UserManagementPermissionUtils.resolvePermissionCode("PUT", "/api/users/edit-user/usr1"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_DELETE,
				UserManagementPermissionUtils.resolvePermissionCode("DELETE", "/api/users/v2/delete/usr1"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_DELETE,
				UserManagementPermissionUtils.resolvePermissionCode("DELETE", "/api/users/delete-user/usr1"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_SUSPEND,
				UserManagementPermissionUtils.resolvePermissionCode("PUT", "/api/users/v2/usr1/SUSPENDED/changeStatus"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_SUSPEND,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/users/suspend-user/usr1"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/navigation"));
		assertEquals(UserManagementPermissionUtils.UM_USERS_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/menu"));
	}

	@Test
	void resolvePermissionCode_rolesEndpoints() {
		assertEquals(UserManagementPermissionUtils.UM_ROLES_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/roles"));
		assertEquals(UserManagementPermissionUtils.UM_ROLES_CREATE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/roles"));
		assertEquals(UserManagementPermissionUtils.UM_ROLES_EDIT,
				UserManagementPermissionUtils.resolvePermissionCode("PUT", "/api/roles/ADMIN_ROLE"));
		assertEquals(UserManagementPermissionUtils.UM_ROLES_DELETE,
				UserManagementPermissionUtils.resolvePermissionCode("DELETE", "/api/roles/OLD_ROLE"));
		assertEquals(UserManagementPermissionUtils.UM_ROLES_VIEW_HISTORY,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/roles/permissions/history"));
		assertEquals(UserManagementPermissionUtils.UM_ROLES_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/roles/1/permissions"));
		assertEquals(UserManagementPermissionUtils.UM_ROLES_EDIT,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/roles/1/permissions"));
	}

	@Test
	void resolvePermissionCode_passwordPolicyEndpoints() {
		assertEquals(UserManagementPermissionUtils.UM_PASSWORD_POLICY_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/password-policies"));
		assertEquals(UserManagementPermissionUtils.UM_PASSWORD_POLICY_CREATE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/password-policies"));
		assertEquals(UserManagementPermissionUtils.UM_PASSWORD_POLICY_EDIT,
				UserManagementPermissionUtils.resolvePermissionCode("PUT", "/api/password-policies/1"));
		assertEquals(UserManagementPermissionUtils.UM_PASSWORD_POLICY_DELETE,
				UserManagementPermissionUtils.resolvePermissionCode("DELETE", "/api/password-policies/1"));
		assertEquals(UserManagementPermissionUtils.UM_PASSWORD_POLICY_ACTIVATE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/password-policies/1/activate"));
	}

	@Test
	void resolvePermissionCode_sodEndpoints() {
		assertEquals(UserManagementPermissionUtils.UM_SOD_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/sod/permission-conflicts"));
		assertEquals(UserManagementPermissionUtils.UM_SOD_CREATE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/sod/permission-conflicts"));
		assertEquals(UserManagementPermissionUtils.UM_SOD_DELETE,
				UserManagementPermissionUtils.resolvePermissionCode("DELETE", "/api/sod/permission-conflicts/1"));
		assertEquals(UserManagementPermissionUtils.UM_SOD_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/sod/override"));
		assertEquals(UserManagementPermissionUtils.UM_SOD_GRANT_OVERRIDE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/sod/override"));
		assertEquals(UserManagementPermissionUtils.UM_SOD_GRANT_OVERRIDE,
				UserManagementPermissionUtils.resolvePermissionCode("DELETE", "/api/sod/override/1"));
		assertEquals(UserManagementPermissionUtils.UM_SOD_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/sod/override/history"));
	}

	@Test
	void resolvePermissionCode_authProvidersAndMethods() {
		assertEquals(UserManagementPermissionUtils.UM_AUTH_METHODS_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/auth-providers"));
		assertEquals(UserManagementPermissionUtils.UM_AUTH_METHODS_CONFIG_PROVIDER,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth-providers"));
		assertEquals(UserManagementPermissionUtils.UM_AUTH_METHODS_CONFIG_PROVIDER,
				UserManagementPermissionUtils.resolvePermissionCode("PUT", "/api/auth-providers/1"));
		assertEquals(UserManagementPermissionUtils.UM_AUTH_METHODS_DELETE_PROVIDER,
				UserManagementPermissionUtils.resolvePermissionCode("DELETE", "/api/auth-providers/1"));
		assertEquals(UserManagementPermissionUtils.UM_AUTH_METHODS_TOGGLE_PROVIDER,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth-providers/1/status"));

		assertEquals(UserManagementPermissionUtils.UM_AUTH_METHODS_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/auth-methods"));
		assertEquals(UserManagementPermissionUtils.UM_AUTH_METHODS_CONFIGURE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth-methods"));
		assertEquals(UserManagementPermissionUtils.UM_AUTH_METHODS_UNLINK,
				UserManagementPermissionUtils.resolvePermissionCode("DELETE", "/api/auth-methods/1"));
	}

	@Test
	void resolvePermissionCode_auditAndAuthEndpoints() {
		assertEquals(UserManagementPermissionUtils.UM_AUDIT_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/users/get-user-activity-list"));
		assertEquals(UserManagementPermissionUtils.UM_AUDIT_VIEW,
				UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/users/login-attempts"));

		assertEquals(UserManagementPermissionUtils.AUTH_LOGIN,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth/login"));
		assertEquals(UserManagementPermissionUtils.AUTH_LOGOUT,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth/logout"));
		assertEquals(UserManagementPermissionUtils.AUTH_PASSWORD_CHANGE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth/change-password"));
		assertEquals(UserManagementPermissionUtils.AUTH_PASSWORD_FORGOT,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth/forgot-password"));
		assertEquals(UserManagementPermissionUtils.AUTH_PASSWORD_RESET,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth/reset-password"));
		assertEquals(UserManagementPermissionUtils.AUTH_TOKEN_REFRESH,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth/refresh-token"));
		assertEquals(UserManagementPermissionUtils.AUTH_TOKEN_VALIDATE,
				UserManagementPermissionUtils.resolvePermissionCode("POST", "/api/auth/validate"));
	}

	@Test
	void resolvePermissionCode_nullBlankOrUnmappedPaths() {
		assertNull(UserManagementPermissionUtils.resolvePermissionCode("GET", null));
		assertNull(UserManagementPermissionUtils.resolvePermissionCode("GET", ""));
		assertNull(UserManagementPermissionUtils.resolvePermissionCode("GET", "   "));
		assertNull(UserManagementPermissionUtils.resolvePermissionCode("GET", "/api/unknown-service/xyz"));
	}

	@Test
	void resolveResourceType_extractsCorrectResource() {
		assertEquals("UM_USERS",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.UM_USERS_VIEW));
		assertEquals("UM_ROLES",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.UM_ROLES_CREATE));
		assertEquals("UM_PASSWORD_POLICY",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.UM_PASSWORD_POLICY_ACTIVATE));
		assertEquals("UM_SOD",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.UM_SOD_GRANT_OVERRIDE));
		assertEquals("UM_AUTH_METHODS",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.UM_AUTH_METHODS_CONFIGURE));
		assertEquals("UM_AUDIT",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.UM_AUDIT_VIEW));
		assertEquals("LOGIN",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.AUTH_LOGIN));
		assertEquals("PASSWORD",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.AUTH_PASSWORD_CHANGE));
		assertEquals("TOKEN",
				UserManagementPermissionUtils.resolveResourceType(UserManagementPermissionUtils.AUTH_TOKEN_REFRESH));

		assertNull(UserManagementPermissionUtils.resolveResourceType(null));
		assertNull(UserManagementPermissionUtils.resolveResourceType("PARAMS.PARAM_CITY.CREATE"));
		assertNull(UserManagementPermissionUtils.resolveResourceType("INVALID"));
	}
}
