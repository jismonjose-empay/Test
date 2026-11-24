package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.empay.user_management.dto.CreatePermissionConflictRequest;
import com.empay.user_management.dto.PermissionResponse;
import com.empay.user_management.dto.SodConflictDetail;
import com.empay.user_management.exceptions.GlobalExceptionHandler;
import com.empay.user_management.service.AppRoleService;
import com.empay.user_management.service.PermissionService;

@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

	private MockMvc mockMvc;

	@Mock
	private PermissionService permissionService;

	@Mock
	private AppRoleService appRoleService;

	@InjectMocks
	private PermissionController permissionController;

	private PermissionResponse samplePermission;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(permissionController)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

		samplePermission = new PermissionResponse(
			"ADMIN.USER.VIEW",
			"ADMIN",
			"USER",
			"VIEW",
			"View user accounts",
			false,
			null
		);
	}

	@Test
	void testGetAllPermissions() throws Exception {
		when(permissionService.getAllPermissions(null)).thenReturn(List.of(samplePermission));

		mockMvc.perform(get("/api/permissions")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].permissionCode").value("ADMIN.USER.VIEW"));
	}

	@Test
	void testGetAllPermissions_WithModule() throws Exception {
		when(permissionService.getAllPermissions("ADMIN")).thenReturn(List.of(samplePermission));

		mockMvc.perform(get("/api/permissions?module=ADMIN")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].module").value("ADMIN"));
	}

	@Test
	void testGetPermissionByCode() throws Exception {
		when(permissionService.getPermissionByCode("ADMIN.USER.VIEW")).thenReturn(samplePermission);

		mockMvc.perform(get("/api/permissions/ADMIN.USER.VIEW")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.permissionCode").value("ADMIN.USER.VIEW"))
			.andExpect(jsonPath("$.actionName").value("VIEW"));
	}

	@Test
	void testGetAllModules() throws Exception {
		when(permissionService.getAllModules()).thenReturn(List.of("ADMIN", "PAYMENT", "MERCHANT"));

		mockMvc.perform(get("/api/permissions/modules")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0]").value("ADMIN"));
	}

	@Test
	void testDeletePermission() throws Exception {
		mockMvc.perform(delete("/api/permissions/ADMIN.USER.VIEW"))
			.andExpect(status().isNoContent());
	}

	@Test
	void testDeletePermission_HasRolePermissions_ThrowsBadRequest() throws Exception {
		doThrow(new IllegalArgumentException("Cannot delete permission: Permission 'ADMIN.USER.VIEW' is currently assigned to one or more roles in ROLE_PERMISSION. Revoke all assignments before deleting the permission."))
			.when(permissionService).deletePermission("ADMIN.USER.VIEW");

		mockMvc.perform(delete("/api/permissions/ADMIN.USER.VIEW"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.type").value("Invalid Request"))
			.andExpect(jsonPath("$.message").value("Cannot delete permission: Permission 'ADMIN.USER.VIEW' is currently assigned to one or more roles in ROLE_PERMISSION. Revoke all assignments before deleting the permission."));
	}

	@Test
	void testGetPermissionConflicts() throws Exception {
		var conflictDetail = new SodConflictDetail(
			"PERMISSION_CONFLICT",
			"MERCHANT.MERCHANT.APPROVE",
			"MERCHANT.MERCHANT.CREATE",
			"BLOCK",
			"Cannot create and approve merchants",
			null,
			null
		);
		when(appRoleService.getPermissionConflicts()).thenReturn(List.of(conflictDetail));

		mockMvc.perform(get("/api/permissions/conflicts")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].codeA").value("MERCHANT.MERCHANT.APPROVE"))
			.andExpect(jsonPath("$[0].codeB").value("MERCHANT.MERCHANT.CREATE"))
			.andExpect(jsonPath("$[0].severity").value("BLOCK"));
	}

	@Test
	void testCreatePermissionConflict() throws Exception {
		var conflictDetail = new SodConflictDetail(
			"PERMISSION_CONFLICT",
			"MERCHANT.MERCHANT.APPROVE",
			"MERCHANT.MERCHANT.CREATE",
			"BLOCK",
			"Cannot create and approve merchants",
			null,
			null
		);
		when(appRoleService.createPermissionConflict(any(CreatePermissionConflictRequest.class))).thenReturn(conflictDetail);

		String payload = """
			{
				"permissionCodeA": "MERCHANT.MERCHANT.CREATE",
				"permissionCodeB": "MERCHANT.MERCHANT.APPROVE",
				"severity": "BLOCK",
				"rationale": "Cannot create and approve merchants"
			}
			""";

		mockMvc.perform(post("/api/permissions/conflicts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.conflictType").value("PERMISSION_CONFLICT"))
			.andExpect(jsonPath("$.codeA").value("MERCHANT.MERCHANT.APPROVE"))
			.andExpect(jsonPath("$.codeB").value("MERCHANT.MERCHANT.CREATE"))
			.andExpect(jsonPath("$.severity").value("BLOCK"));
	}
}
