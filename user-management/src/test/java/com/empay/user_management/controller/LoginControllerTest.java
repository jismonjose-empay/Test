package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.empay.user_management.exceptions.GlobalExceptionHandler;
import com.empay.user_management.service.LoginService;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

	private MockMvc mockMvc;

	@Mock
	private LoginService loginService;

	@InjectMocks
	private LoginController loginController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(loginController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void testLogout_Post_Success() throws Exception {
		when(loginService.logout(any())).thenReturn(ResponseEntity.ok(Map.of("message", "Logout successful", "userId", "admin")));

		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Logout successful"))
			.andExpect(jsonPath("$.userId").value("admin"));
	}

	@Test
	void testLogout_Get_Success() throws Exception {
		when(loginService.logout(any())).thenReturn(ResponseEntity.ok(Map.of("message", "Logout successful")));

		mockMvc.perform(get("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Logout successful"));
	}

	@Test
	void testUserLogin() throws Exception {
		when(loginService.validateUserLogin(any(), any())).thenReturn(ResponseEntity.ok(Map.of("message", "Login successful")));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"inputData\":\"61646d696e\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Login successful"));
	}

	@Test
	void testChangePassword() throws Exception {
		when(loginService.changeUserPassword(any())).thenReturn(ResponseEntity.ok(Map.of("message", "Password changed successfully")));

		mockMvc.perform(post("/api/auth/change-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"inputData\":\"pwd_data\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Password changed successfully"));
	}

	@Test
	void testChangePassword_PolicyViolation() throws Exception {
		when(loginService.changeUserPassword(any())).thenReturn(ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"message", "Password does not meet active policy requirements: Must contain at least one uppercase letter",
				"violations", List.of("Must contain at least one uppercase letter")
		)));

		mockMvc.perform(post("/api/auth/change-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userId\":\"admin\",\"newPassword\":\"weak\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.violations[0]").value("Must contain at least one uppercase letter"));
	}

	@Test
	void testGetLoginAttempts() throws Exception {
		when(loginService.getLoginAttempts()).thenReturn(List.of());

		mockMvc.perform(get("/api/auth/login-attempts")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	void testGetCurrentUser_Success() throws Exception {
		when(loginService.getCurrentUserSession(any())).thenReturn(ResponseEntity.ok(Map.of("authenticated", true, "userId", "admin")));

		mockMvc.perform(get("/api/auth/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.userId").value("admin"));
	}
}

