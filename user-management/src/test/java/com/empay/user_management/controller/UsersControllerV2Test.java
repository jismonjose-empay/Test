package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.empay.user_management.exceptions.GlobalExceptionHandler;
import com.empay.user_management.service.UsersServiceV2;

@ExtendWith(MockitoExtension.class)
class UsersControllerV2Test {

	private MockMvc mockMvc;

	@Mock
	private UsersServiceV2 usersService;

	@InjectMocks
	private UsersControllerV2 controller;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	@DisplayName("GET /api/users/v2/get-users returns users list")
	void testGetUsers() throws Exception {
		when(usersService.getUsers("john", "ACTIVE", "MAKER", 1, 20))
				.thenReturn(Map.of("total", 1, "items", java.util.List.of()));

		mockMvc.perform(get("/api/users/v2/get-users")
						.param("search", "john")
						.param("status", "ACTIVE")
						.param("roleCode", "MAKER")
						.param("page", "1")
						.param("pageSize", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1));
	}

	@Test
	@DisplayName("GET /api/users/v2/get-users/{userId} returns user details")
	void testGetUserById() throws Exception {
		when(usersService.getUser("johndoe")).thenReturn(ResponseEntity.ok("{\"userId\":\"johndoe\"}"));

		mockMvc.perform(get("/api/users/v2/get-users/johndoe"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("POST /api/users/v2/add-user adds new user")
	void testAddUser() throws Exception {
		when(usersService.addUser(any())).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body("{\"userId\":\"newuser\"}"));

		String json = "{\"userId\":\"newuser\",\"email\":\"new@empay.com\",\"displayName\":\"New User\"}";

		mockMvc.perform(post("/api/users/v2/add-user")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("PUT /api/users/v2/add-user/{userId} updates user")
	void testUpdateUser() throws Exception {
		when(usersService.updateUser(any(), eq("johndoe"))).thenReturn(ResponseEntity.ok("{\"userId\":\"johndoe\"}"));

		String json = "{\"userId\":\"johndoe\",\"email\":\"john@empay.com\",\"displayName\":\"John Doe\"}";

		mockMvc.perform(put("/api/users/v2/add-user/johndoe")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("PUT /api/users/v2/{userId}/{status}/changeStatus suspends/activates user")
	void testChangeStatus() throws Exception {
		when(usersService.suspendUser(eq("johndoe"), eq("SUSPENDED"), any()))
				.thenReturn(ResponseEntity.ok("{\"status\":\"SUSPENDED\"}"));

		mockMvc.perform(put("/api/users/v2/johndoe/SUSPENDED/changeStatus")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"policy review\"}"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("GET /api/users/v2/stats returns user stats")
	void testUsersStats() throws Exception {
		when(usersService.getUsersStats()).thenReturn(Map.of("totalUsers", 10, "activeUsers", 8));

		mockMvc.perform(get("/api/users/v2/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalUsers").value(10));
	}

	@Test
	@DisplayName("DELETE /api/users/v2/delete/{userId} soft deletes user")
	void testDeleteUser() throws Exception {
		when(usersService.deleteUser("johndoe")).thenReturn(ResponseEntity.noContent().build());

		mockMvc.perform(delete("/api/users/v2/delete/johndoe"))
				.andExpect(status().isNoContent());

		verify(usersService).deleteUser("johndoe");
	}
}
