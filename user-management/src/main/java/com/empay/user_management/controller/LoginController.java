package com.empay.user_management.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.service.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/auth", "/api/auth/"})
@RequiredArgsConstructor
public class LoginController {

	private final LoginService loginService;

	@PreAuthorize("permitAll()")
	@PostMapping("/login")
	public ResponseEntity<Object> userLogin(@RequestBody Map<String, String> inputDataMap, HttpServletRequest request) {
		return loginService.validateUserLogin(inputDataMap, request);
	}

	@PreAuthorize("permitAll()")
	@PostMapping("/change-password")
	public ResponseEntity<Object> changePassword(@RequestBody Map<String, String> inputDataMap) {
		return loginService.changeUserPassword(inputDataMap);
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_AUDIT.VIEW')")
	@GetMapping("/login-attempts")
	public ResponseEntity<Object> getLoginAttempts() {
		return ResponseEntity.ok(loginService.getLoginAttempts());
	}

	@PreAuthorize("permitAll()")
	@PostMapping({"/logout", "logout"})
	public ResponseEntity<Object> logout(HttpServletRequest request) {
		return loginService.logout(request);
	}

	@PreAuthorize("permitAll()")
	@GetMapping({"/logout", "logout"})
	public ResponseEntity<Object> logoutGet(HttpServletRequest request) {
		return loginService.logout(request);
	}

	@PreAuthorize("permitAll()")
	@GetMapping({"/me", "me", "/session", "session"})
	public ResponseEntity<Object> getCurrentUser(HttpServletRequest request) {
		return loginService.getCurrentUserSession(request);
	}
}
