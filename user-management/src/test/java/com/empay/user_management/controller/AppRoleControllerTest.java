package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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

import com.empay.user_management.dto.BulkRolePermissionSummary;
import com.empay.user_management.dto.BulkRolePermissionsRequest;
import com.empay.user_management.dto.ConflictCheckRequest;
import com.empay.user_management.dto.ConflictCheckResponse;
import com.empay.user_management.dto.CreatePermissionConflictRequest;
import com.empay.user_management.dto.CreateRoleRequest;
import com.empay.user_management.dto.PermissionResponse;
import com.empay.user_management.dto.RolePermissionHistoryPageResponse;
import com.empay.user_management.dto.RolePermissionHistoryResponse;
import com.empay.user_management.dto.RolePermissionRequest;
import com.empay.user_management.dto.RolePermissionsResponse;
import com.empay.user_management.dto.RoleResponse;
import com.empay.user_management.dto.RolesAndPermissionsResponse;
import com.empay.user_management.dto.SodConflictDetail;
import com.empay.user_management.dto.UpdateRoleRequest;
import com.empay.user_management.exceptions.GlobalExceptionHandler;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.exceptions.SodConflictException;
import com.empay.user_management.service.AppRoleService;

@ExtendWith(MockitoExtension.class)
class AppRoleControllerTest {

	private MockMvc mockMvc;

	@Mock
	private AppRoleService appRoleService;

	@InjectMocks
	private AppRoleController appRoleController;

	private RoleResponse sampleRole;
	private PermissionResponse samplePermission;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(appRoleController)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

		sampleRole = new RoleResponse(
			"USER_ADMIN",
			"User Administrator",
			"Maintains user accounts",
			false,
			"SEED",
			LocalDateTime.now()
		);

		samplePermission = new PermissionResponse(
			"ADMIN.USER.VIEW",
			"ADMIN",
			"USER",
			"VIEW",
			"View user accounts",
			false,
			101
		);
	}

	@Test
	void testGetAllRoles() throws Exception {
		when(appRoleService.getAllRoles()).thenReturn(List.of(sampleRole));

		mockMvc.perform(get("/api/roles")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].roleCode").value("USER_ADMIN"))
			.andExpect(jsonPath("$[0].name").value("User Administrator"));
	}

	@Test
	void testGetRolesAndPermissions() throws Exception {
		var combined = new RolesAndPermissionsResponse(List.of(sampleRole), List.of(samplePermission));
		when(appRoleService.getRolesAndPermissions()).thenReturn(combined);

		mockMvc.perform(get("/api/roles/roles-and-permissions")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roles.length()").value(1))
			.andExpect(jsonPath("$.permissions.length()").value(1))
			.andExpect(jsonPath("$.roles[0].roleCode").value("USER_ADMIN"))
			.andExpect(jsonPath("$.permissions[0].permissionCode").value("ADMIN.USER.VIEW"));
	}

	@Test
	void testGetRoleByCode() throws Exception {
		when(appRoleService.getRoleByCode("USER_ADMIN")).thenReturn(sampleRole);

		mockMvc.perform(get("/api/roles/USER_ADMIN")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roleCode").value("USER_ADMIN"))
			.andExpect(jsonPath("$.name").value("User Administrator"));
	}

	@Test
	void testGetRoleByCode_NotFound() throws Exception {
		when(appRoleService.getRoleByCode("UNKNOWN"))
			.thenThrow(new ResourceNotFoundException("APP_ROLE", "roleCode", "Role not found with code: UNKNOWN"));

		mockMvc.perform(get("/api/roles/UNKNOWN")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.type").value("Expectation Failed"))
			.andExpect(jsonPath("$.message").value("Role not found with code: UNKNOWN"));
	}

	@Test
	void testGetPermissionsForRole() throws Exception {
		when(appRoleService.getPermissionsForRole("USER_ADMIN")).thenReturn(List.of(samplePermission));

		mockMvc.perform(get("/api/roles/USER_ADMIN/permissions")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].permissionCode").value("ADMIN.USER.VIEW"))
			.andExpect(jsonPath("$[0].module").value("ADMIN"));
	}

	@Test
	void testGetBulkRolePermissions_All() throws Exception {
		var rolePerm = new RolePermissionsResponse(
			"USER_ADMIN",
			"User Administrator",
			"Maintains user accounts",
			false,
			List.of("ADMIN.USER.VIEW")
		);
		when(appRoleService.getBulkRolePermissions(null)).thenReturn(List.of(rolePerm));

		mockMvc.perform(get("/api/roles/permissions/bulk")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].roleCode").value("USER_ADMIN"))
			.andExpect(jsonPath("$[0].roleName").value("User Administrator"))
			.andExpect(jsonPath("$[0].permissionCodes[0]").value("ADMIN.USER.VIEW"))
			.andExpect(jsonPath("$[0].permissions").doesNotExist());
	}

	@Test
	void testGetBulkRolePermissions_Filtered() throws Exception {
		var rolePerm = new RolePermissionsResponse(
			"USER_ADMIN",
			"User Administrator",
			"Maintains user accounts",
			false,
			List.of("ADMIN.USER.VIEW")
		);
		when(appRoleService.getBulkRolePermissions(List.of("USER_ADMIN"))).thenReturn(List.of(rolePerm));

		mockMvc.perform(get("/api/roles/permissions/bulk")
				.param("roleCodes", "USER_ADMIN")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].roleCode").value("USER_ADMIN"))
			.andExpect(jsonPath("$[0].permissionCodes[0]").value("ADMIN.USER.VIEW"))
			.andExpect(jsonPath("$[0].permissions").doesNotExist());
	}

	@Test
	void testGetBulkRolePermissions_AliasPath() throws Exception {
		var rolePerm = new RolePermissionsResponse(
			"USER_ADMIN",
			"User Administrator",
			"Maintains user accounts",
			false,
			List.of("ADMIN.USER.VIEW")
		);
		when(appRoleService.getBulkRolePermissions(null)).thenReturn(List.of(rolePerm));

		mockMvc.perform(get("/api/roles/permissions")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].roleCode").value("USER_ADMIN"))
			.andExpect(jsonPath("$[0].permissionCodes[0]").value("ADMIN.USER.VIEW"))
			.andExpect(jsonPath("$[0].permissions").doesNotExist());
	}

	@Test
	void testAssignPermissions_Success() throws Exception {
		when(appRoleService.assignPermissionsToRole(any(RolePermissionRequest.class), any())).thenReturn(List.of(samplePermission));

		String payload = """
			{
				"roleCode": "USER_ADMIN",
				"permissionCodes": ["ADMIN.USER.VIEW"],
				"grantedBy": "ADMIN_USER"
			}
			""";

		mockMvc.perform(post("/api/roles/permissions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].permissionCode").value("ADMIN.USER.VIEW"));
	}

	@Test
	void testAssignPermissions_ValidationFailure_BlankRoleCode() throws Exception {
		String invalidPayload = """
			{
				"roleCode": "",
				"permissionCodes": ["ADMIN.USER.VIEW"]
			}
			""";

		mockMvc.perform(post("/api/roles/permissions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidPayload))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.type").value("Validation Error"))
			.andExpect(jsonPath("$.errors[0].fieldName").value("roleCode"))
			.andExpect(jsonPath("$.errors[0].fieldError").value("Role code is mandatory"));
	}

	@Test
	void testAssignPermissions_ValidationFailure_EmptyPermissionCodes() throws Exception {
		String invalidPayload = """
			{
				"roleCode": "USER_ADMIN",
				"permissionCodes": []
			}
			""";

		mockMvc.perform(post("/api/roles/permissions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidPayload))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.type").value("Validation Error"))
			.andExpect(jsonPath("$.errors[0].fieldName").value("permissionCodes"))
			.andExpect(jsonPath("$.errors[0].fieldError").value("Permission codes must not be empty"));
	}

	@Test
	void testUpdateRolePermissions() throws Exception {
		when(appRoleService.updateRolePermissions(eq("USER_ADMIN"), any(), any())).thenReturn(List.of(samplePermission));

		String payload = """
			["ADMIN.USER.VIEW"]
			""";

		mockMvc.perform(put("/api/roles/USER_ADMIN/permissions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].permissionCode").value("ADMIN.USER.VIEW"));
	}

	@Test
	void testDeleteRole() throws Exception {
		mockMvc.perform(delete("/api/roles/USER_ADMIN"))
			.andExpect(status().isNoContent());
	}

	@Test
	void testDeleteRole_HasAssignedUsers_ThrowsBadRequest() throws Exception {
		doThrow(new IllegalArgumentException("Cannot delete role: Role 'USER_ADMIN' has assigned to Users."))
				.when(appRoleService).deleteRole("USER_ADMIN");

		mockMvc.perform(delete("/api/roles/USER_ADMIN"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.type").value("Invalid Request"))
				.andExpect(jsonPath("$.message").value("Cannot delete role: Role 'USER_ADMIN' has assigned to Users."));
	}

	@Test
	void testAssignPermissions_DuplicateRolePermission_ThrowsConflict() throws Exception {
		when(appRoleService.assignPermissionsToRole(any(RolePermissionRequest.class), any()))
			.thenThrow(new ResourceAlreadyExistsException("ROLE_PERMISSION", "permissionCode", "Permission 'ADMIN.USER.VIEW' is already assigned to role 'USER_ADMIN'"));

		String payload = """
			{
				"roleCode": "USER_ADMIN",
				"permissionCodes": ["ADMIN.USER.VIEW"]
			}
			""";

		mockMvc.perform(post("/api/roles/permissions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value("Permission 'ADMIN.USER.VIEW' is already assigned to role 'USER_ADMIN'"));
	}

	@Test
	void testRevokePermission() throws Exception {
		mockMvc.perform(delete("/api/roles/USER_ADMIN/permissions/ADMIN.USER.VIEW"))
			.andExpect(status().isNoContent());
	}

	@Test
	void testRevokePermissions() throws Exception {
		String payload = """
			["ADMIN.USER.VIEW"]
			""";

		mockMvc.perform(delete("/api/roles/USER_ADMIN/permissions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isNoContent());
	}

	@Test
	void testCreateRole() throws Exception {
		when(appRoleService.createRole(any(CreateRoleRequest.class), any())).thenReturn(sampleRole);

		String payload = """
			{
				"roleCode": "USER_ADMIN",
				"name": "User Administrator",
				"description": "Maintains user accounts",
				"permissionCodes": []
			}
			""";

		mockMvc.perform(post("/api/roles")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.roleCode").value("USER_ADMIN"))
			.andExpect(jsonPath("$.name").value("User Administrator"));
	}

	@Test
	void testUpdateRole() throws Exception {
		when(appRoleService.updateRole(eq("USER_ADMIN"), any(UpdateRoleRequest.class), any())).thenReturn(sampleRole);

		String payload = """
			{
				"name": "User Administrator",
				"description": "Updated description"
			}
			""";

		mockMvc.perform(put("/api/roles/USER_ADMIN")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roleCode").value("USER_ADMIN"));
	}

	@Test
	void testCheckConflicts() throws Exception {
		var conflictDetail = new SodConflictDetail(
			"PERMISSION_CONFLICT",
			"PAYMENT.SETTLEMENT.APPROVE",
			"PAYMENT.SETTLEMENT.CREATE",
			"BLOCK",
			"Maker-checker violation",
			1L,
			"CHECKER vs MAKER"
		);
		var response = new ConflictCheckResponse("MAKER", true, true, List.of(conflictDetail));

		when(appRoleService.checkConflicts(any(ConflictCheckRequest.class))).thenReturn(response);

		String payload = """
			{
				"roleCode": "MAKER",
				"permissionCodes": ["PAYMENT.SETTLEMENT.CREATE", "PAYMENT.SETTLEMENT.APPROVE"]
			}
			""";

		mockMvc.perform(post("/api/roles/check-conflicts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hasConflicts").value(true))
			.andExpect(jsonPath("$.hasBlockingConflicts").value(true))
			.andExpect(jsonPath("$.conflicts.length()").value(1))
			.andExpect(jsonPath("$.conflicts[0].conflictType").value("PERMISSION_CONFLICT"));
	}

	@Test
	void testAssignPermissions_SodConflict_ThrowsConflict() throws Exception {
		var conflictDetail = new SodConflictDetail(
			"PERMISSION_CONFLICT",
			"PAYMENT.SETTLEMENT.APPROVE",
			"PAYMENT.SETTLEMENT.CREATE",
			"BLOCK",
			"Maker-checker violation",
			1L,
			"CHECKER vs MAKER"
		);
		var ex = new SodConflictException("Segregation of duties conflict detected", List.of(conflictDetail));

		when(appRoleService.assignPermissionsToRole(any(RolePermissionRequest.class), any())).thenThrow(ex);

		String payload = """
			{
				"roleCode": "MAKER",
				"permissionCodes": ["PAYMENT.SETTLEMENT.CREATE", "PAYMENT.SETTLEMENT.APPROVE"]
			}
			""";

		mockMvc.perform(post("/api/roles/permissions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.type").value("SoD Conflict Error"))
			.andExpect(jsonPath("$.errors[0].conflictType").value("PERMISSION_CONFLICT"))
			.andExpect(jsonPath("$.errors[0].severity").value("BLOCK"));
	}

	@Test
	void testGetRoleConflicts() throws Exception {
		var roleConflict = new SodConflictDetail(
			"ROLE_CONFLICT",
			"CHECKER",
			"MAKER",
			"BLOCK",
			"Maker-checker conflict",
			1L,
			"CHECKER vs MAKER"
		);
		when(appRoleService.getRoleConflicts()).thenReturn(List.of(roleConflict));

		mockMvc.perform(get("/api/roles/conflicts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].codeA").value("CHECKER"))
			.andExpect(jsonPath("$[0].codeB").value("MAKER"));
	}

	@Test
	void testGetPermissionConflicts() throws Exception {
		var permConflict = new SodConflictDetail(
			"PERMISSION_CONFLICT",
			"PAYMENT.SETTLEMENT.APPROVE",
			"PAYMENT.SETTLEMENT.CREATE",
			"BLOCK",
			"Maker-checker violation",
			1L,
			"CHECKER vs MAKER"
		);
		when(appRoleService.getPermissionConflicts()).thenReturn(List.of(permConflict));

		mockMvc.perform(get("/api/roles/permission-conflicts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].codeA").value("PAYMENT.SETTLEMENT.APPROVE"));
	}

	@Test
	void testCreatePermissionConflict_Success() throws Exception {
		var permConflict = new SodConflictDetail(
			"PERMISSION_CONFLICT",
			"MERCHANT.MERCHANT.APPROVE",
			"MERCHANT.MERCHANT.CREATE",
			"BLOCK",
			"Cannot create and approve merchants",
			null,
			null
		);
		when(appRoleService.createPermissionConflict(any(CreatePermissionConflictRequest.class))).thenReturn(permConflict);

		String payload = """
			{
				"permissionCodeA": "MERCHANT.MERCHANT.CREATE",
				"permissionCodeB": "MERCHANT.MERCHANT.APPROVE",
				"severity": "BLOCK",
				"rationale": "Cannot create and approve merchants"
			}
			""";

		mockMvc.perform(post("/api/roles/permission-conflicts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.conflictType").value("PERMISSION_CONFLICT"))
			.andExpect(jsonPath("$.codeA").value("MERCHANT.MERCHANT.APPROVE"))
			.andExpect(jsonPath("$.codeB").value("MERCHANT.MERCHANT.CREATE"))
			.andExpect(jsonPath("$.severity").value("BLOCK"));
	}

	@Test
	void testCreatePermissionConflict_AlreadyExists_ThrowsConflict() throws Exception {
		when(appRoleService.createPermissionConflict(any(CreatePermissionConflictRequest.class)))
			.thenThrow(new ResourceAlreadyExistsException("PERMISSION_CONFLICT", "pair", "Permission conflict already exists"));

		String payload = """
			{
				"permissionCodeA": "MERCHANT.MERCHANT.CREATE",
				"permissionCodeB": "MERCHANT.MERCHANT.APPROVE",
				"severity": "BLOCK",
				"rationale": "Cannot create and approve merchants"
			}
			""";

		mockMvc.perform(post("/api/roles/permission-conflicts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value("Permission conflict already exists"));
	}

	@Test
	void testBulkConfigureRolePermissions_Put_Success() throws Exception {
		var summary = new BulkRolePermissionSummary(
			2, 3, 1, List.of(sampleRole),
			"Bulk role permission configuration processed successfully."
		);
		when(appRoleService.bulkConfigureRolePermissions(any(BulkRolePermissionsRequest.class), any())).thenReturn(summary);

		String payload = """
			{
				"roles": [
					{
						"roleCode": "MAKER",
						"permissionCodes": ["PAYMENT.SETTLEMENT.CREATE"]
					}
				]
			}
			""";

		mockMvc.perform(put("/api/roles/permissions/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalRolesProcessed").value(2))
			.andExpect(jsonPath("$.totalPermissionsGranted").value(3))
			.andExpect(jsonPath("$.totalPermissionsRevoked").value(1));
	}

	@Test
	void testBulkConfigureRolePermissions_Post_Success() throws Exception {
		var summary = new BulkRolePermissionSummary(
			1, 2, 0, List.of(sampleRole),
			"Bulk role permission configuration processed successfully."
		);
		when(appRoleService.bulkConfigureRolePermissions(any(BulkRolePermissionsRequest.class), any())).thenReturn(summary);

		String payload = """
			{
				"roles": [
					{
						"roleCode": "MAKER",
						"permissionCodes": ["PAYMENT.SETTLEMENT.CREATE"]
					}
				]
			}
			""";

		mockMvc.perform(post("/api/roles/permissions/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalRolesProcessed").value(1))
			.andExpect(jsonPath("$.totalPermissionsGranted").value(2));
	}

	@Test
	void testBulkConfigureRolePermissions_SodConflict_Returns409() throws Exception {
		var conflictDetail = new SodConflictDetail(
			"PERMISSION_CONFLICT",
			"PAYMENT.SETTLEMENT.APPROVE",
			"PAYMENT.SETTLEMENT.CREATE",
			"BLOCK",
			"Maker-checker violation",
			null,
			null
		);
		var ex = new SodConflictException("Segregation of duties conflict detected", List.of(conflictDetail));
		when(appRoleService.bulkConfigureRolePermissions(any(BulkRolePermissionsRequest.class), any())).thenThrow(ex);

		String payload = """
			{
				"roles": [
					{
						"roleCode": "MAKER",
						"permissionCodes": ["PAYMENT.SETTLEMENT.CREATE", "PAYMENT.SETTLEMENT.APPROVE"]
					}
				]
			}
			""";

		mockMvc.perform(put("/api/roles/permissions/bulk")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.type").value("SoD Conflict Error"));
	}

	@Test
	void testGetRolePermissionHistory_Returns200() throws Exception {
		var entry = new RolePermissionHistoryResponse(
			1L,
			"MAKER",
			"PAYMENT.SETTLEMENT.CREATE",
			"ADDED",
			"ADMIN_USER",
			LocalDateTime.now()
		);
		var pageResponse = new RolePermissionHistoryPageResponse(
			List.of(entry),
			1L,
			1,
			20,
			1,
			false
		);

		when(appRoleService.getRolePermissionHistory(eq("MAKER"), any(), any(), eq(1), eq(20)))
			.thenReturn(pageResponse);

		mockMvc.perform(get("/api/roles/MAKER/permissions/history?page=1&pageSize=20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(1))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.history[0].roleCode").value("MAKER"))
			.andExpect(jsonPath("$.history[0].actionTaken").value("ADDED"));
	}

	@Test
	void testGetAllRolePermissionHistory_Returns200() throws Exception {
		var entry = new RolePermissionHistoryResponse(
			2L,
			"CHECKER",
			"PAYMENT.SETTLEMENT.APPROVE",
			"REMOVED",
			"ADMIN_USER",
			LocalDateTime.now()
		);
		var pageResponse = new RolePermissionHistoryPageResponse(
			List.of(entry),
			1L,
			1,
			20,
			1,
			false
		);

		when(appRoleService.getAllRolePermissionHistory(any(), any(), any(), eq(1), eq(20)))
			.thenReturn(pageResponse);

		mockMvc.perform(get("/api/roles/permissions/history?page=1&pageSize=20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(1))
			.andExpect(jsonPath("$.history[0].roleCode").value("CHECKER"))
			.andExpect(jsonPath("$.history[0].actionTaken").value("REMOVED"));
	}
}
