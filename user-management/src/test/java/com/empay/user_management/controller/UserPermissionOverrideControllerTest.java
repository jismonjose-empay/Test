package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.empay.user_management.dto.UserPermissionOverridePageResponse;
import com.empay.user_management.dto.UserPermissionOverrideResponse;
import com.empay.user_management.exceptions.GlobalExceptionHandler;
import com.empay.user_management.service.UserPermissionOverrideService;

@ExtendWith(MockitoExtension.class)
class UserPermissionOverrideControllerTest {

	private MockMvc mockMvc;

	@Mock
	private UserPermissionOverrideService overrideService;

	@InjectMocks
	private UserPermissionOverrideController controller;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	@DisplayName("POST /api/sod/override creates an override")
	void testCreateOverride() throws Exception {
		LocalDateTime expires = LocalDateTime.now(ZoneOffset.UTC).plusDays(7);
		UserPermissionOverrideResponse resp = new UserPermissionOverrideResponse(
				1L, 101L, "ADMIN.USER.VIEW", "GRANT", "Need access for annual audit",
				"admin", LocalDateTime.now(ZoneOffset.UTC), expires);

		when(overrideService.createOverride(any())).thenReturn(resp);

		String requestJson = "{" +
				"\"userSerialNo\": 101," +
				"\"permissionCode\": \"ADMIN.USER.VIEW\"," +
				"\"effect\": \"GRANT\"," +
				"\"justification\": \"Need access for annual audit\"," +
				"\"grantedBy\": \"admin\"," +
				"\"expiresAt\": \"" + expires + "\"" +
				"}";

		mockMvc.perform(post("/api/sod/override")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.overrideId").value(1))
				.andExpect(jsonPath("$.effect").value("GRANT"));
	}

	@Test
	@DisplayName("GET /api/sod/override returns paged overrides")
	void testGetAllOverrides() throws Exception {
		UserPermissionOverrideResponse item = new UserPermissionOverrideResponse(
				1L, 101L, "ADMIN.USER.VIEW", "GRANT", "Audit", "admin",
				LocalDateTime.now(ZoneOffset.UTC), null);
		UserPermissionOverridePageResponse page = new UserPermissionOverridePageResponse(
				List.of(item), 1L, 1, 20, 1, false);

		when(overrideService.getAllOverrides("VIEW", "overrideId", "DESC", 1, 20))
				.thenReturn(page);

		mockMvc.perform(get("/api/sod/override")
						.param("search", "VIEW")
						.param("page", "1")
						.param("pageSize", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.items[0].overrideId").value(1));
	}

	@Test
	@DisplayName("GET /api/sod/override/{id} returns override details")
	void testGetOverrideById() throws Exception {
		UserPermissionOverrideResponse resp = new UserPermissionOverrideResponse(
				5L, 101L, "ADMIN.USER.VIEW", "GRANT", "Audit", "admin",
				LocalDateTime.now(ZoneOffset.UTC), null);

		when(overrideService.getOverrideById(5L)).thenReturn(resp);

		mockMvc.perform(get("/api/sod/override/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.overrideId").value(5))
				.andExpect(jsonPath("$.permissionCode").value("ADMIN.USER.VIEW"));
	}

	@Test
	@DisplayName("DELETE /api/sod/override/{overrideId} deletes override")
	void testDeleteOverride() throws Exception {
		mockMvc.perform(delete("/api/sod/override/10")
						.param("changedBy", "admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.overrideId").value("10"))
				.andExpect(jsonPath("$.message").value("Permission override deleted successfully"));

		verify(overrideService).deleteOverride(10L, "admin");
	}
}
