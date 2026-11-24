package com.empay.mars_gateway.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class UserActivityTrackingFilterPermissionTest {

	private UserActivityTrackingFilter filter;

	@BeforeEach
	void setUp() {
		WebClient webClient = mock(WebClient.class);
		filter = new UserActivityTrackingFilter(webClient);
	}

	@Test
	void resolvePermissionCode_parameterEndpoints_resolvesCorrectPermissionCode() {
		assertEquals("PARAMS.PARAM_CITY.CREATE", filter.resolvePermissionCode("POST", "/api/params/add-city"));
		assertEquals("PARAMS.PARAM_CITY.VIEW", filter.resolvePermissionCode("GET", "/api/params/get-cities"));
		assertEquals("PARAMS.PARAM_CITY.VIEW", filter.resolvePermissionCode("GET", "/api/params/get-all-cities"));
		assertEquals("PARAMS.PARAM_CITY.VIEW", filter.resolvePermissionCode("GET", "/api/params/get-city-data/101"));
		assertEquals("PARAMS.PARAM_CITY.EDIT", filter.resolvePermissionCode("PUT", "/api/params/edit-city/101"));
		assertEquals("PARAMS.PARAM_CITY.DELETE", filter.resolvePermissionCode("DELETE", "/api/params/delete-city/101"));

		assertEquals("PARAMS.PARAM_COUNTRY.CREATE", filter.resolvePermissionCode("POST", "/api/params/save-country"));
		assertEquals("PARAMS.PARAM_COUNTRY.VIEW", filter.resolvePermissionCode("GET", "/api/params/find-country/5"));
		assertEquals("PARAMS.PARAM_COUNTRY.EDIT", filter.resolvePermissionCode("PUT", "/api/params/update-country/5"));
		assertEquals("PARAMS.PARAM_COUNTRY.DELETE", filter.resolvePermissionCode("DELETE", "/api/params/delete-country/5"));

		assertEquals("PARAMS.PARAM_ACQUIRER_BIN.CREATE", filter.resolvePermissionCode("POST", "/api/params/save-acquirerbin"));
		assertEquals("PARAMS.PARAM_MCC_SURCHARGE.VIEW", filter.resolvePermissionCode("GET", "/api/params/get-mccsurcharge"));
		assertEquals("PARAMS.PARAM_TERMINAL_MAKE.VIEW", filter.resolvePermissionCode("GET", "/api/params/get-terminalmakes"));
	}

	@Test
	void resolveResourceType_parameterEndpoints_derivesResourceFromPermissionCode() {
		String perm = filter.resolvePermissionCode("POST", "/api/params/add-city");
		assertEquals("PARAMS.PARAM_CITY.CREATE", perm);
		assertEquals("PARAM_CITY", filter.resolveResourceType(perm));

		String binPerm = filter.resolvePermissionCode("POST", "/api/params/save-acquirerbin");
		assertEquals("PARAM_ACQUIRER_BIN", filter.resolveResourceType(binPerm));
	}

	@Test
	void resolvePermissionCode_userManagementEndpoints_resolvesCorrectPermissionCode() {
		assertEquals("ADMIN.UM_USERS.VIEW", filter.resolvePermissionCode("GET", "/api/users/v2/get-users"));
		assertEquals("ADMIN.UM_USERS.CREATE", filter.resolvePermissionCode("POST", "/api/users/v2/add-user"));
		assertEquals("ADMIN.UM_USERS.EDIT", filter.resolvePermissionCode("PUT", "/api/users/v2/add-user/usr1"));
		assertEquals("ADMIN.UM_USERS.SUSPEND", filter.resolvePermissionCode("PUT", "/api/users/v2/usr1/SUSPENDED/changeStatus"));
		assertEquals("ADMIN.UM_USERS.DELETE", filter.resolvePermissionCode("DELETE", "/api/users/v2/delete/usr1"));

		assertEquals("ADMIN.UM_ROLES.VIEW", filter.resolvePermissionCode("GET", "/api/roles"));
		assertEquals("ADMIN.UM_ROLES.CREATE", filter.resolvePermissionCode("POST", "/api/roles"));
		assertEquals("ADMIN.UM_ROLES.EDIT", filter.resolvePermissionCode("PUT", "/api/roles/ADMIN_ROLE"));
		assertEquals("ADMIN.UM_ROLES.DELETE", filter.resolvePermissionCode("DELETE", "/api/roles/OLD_ROLE"));
		assertEquals("ADMIN.UM_ROLES.VIEW_HISTORY", filter.resolvePermissionCode("GET", "/api/roles/permissions/history"));

		assertEquals("ADMIN.UM_PASSWORD_POLICY.VIEW", filter.resolvePermissionCode("GET", "/api/password-policies"));
		assertEquals("ADMIN.UM_PASSWORD_POLICY.CREATE", filter.resolvePermissionCode("POST", "/api/password-policies"));
		assertEquals("ADMIN.UM_PASSWORD_POLICY.ACTIVATE", filter.resolvePermissionCode("POST", "/api/password-policies/1/activate"));

		assertEquals("ADMIN.UM_SOD.VIEW", filter.resolvePermissionCode("GET", "/api/sod/permission-conflicts"));
		assertEquals("ADMIN.UM_SOD.GRANT_OVERRIDE", filter.resolvePermissionCode("POST", "/api/sod/override"));

		assertEquals("ADMIN.UM_AUDIT.VIEW", filter.resolvePermissionCode("GET", "/api/users/get-user-activity-list"));
		assertEquals("AUTH.LOGIN.LOGIN", filter.resolvePermissionCode("POST", "/api/auth/login"));
	}

	@Test
	void resolveResourceType_userManagementEndpoints_derivesResourceFromPermissionCode() {
		String perm = filter.resolvePermissionCode("GET", "/api/users/v2/get-users");
		assertEquals("UM_USERS", filter.resolveResourceType(perm));

		String rolePerm = filter.resolvePermissionCode("POST", "/api/roles");
		assertEquals("UM_ROLES", filter.resolveResourceType(rolePerm));

		String policyPerm = filter.resolvePermissionCode("POST", "/api/password-policies");
		assertEquals("UM_PASSWORD_POLICY", filter.resolveResourceType(policyPerm));
	}

	@Test
	void resolvePermissionCode_unmappedPath_returnsNullAndEndpointResource() {
		String perm = filter.resolvePermissionCode("GET", "/api/unknown-service/something");
		assertNull(perm);
		assertEquals("ENDPOINT", filter.resolveResourceType(perm));
	}
}
