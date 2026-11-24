package com.empay.user_management.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.service.UsersServiceV2;
import com.empay.user_management.valueobjects.UsersVOV2;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/users/v2")
@RequiredArgsConstructor
public class UsersControllerV2 {

	private final UsersServiceV2 usersService;

	@PreAuthorize("hasAuthority('ADMIN.UM_USERS.VIEW')")
	@GetMapping("/get-users")
	public ResponseEntity<Object> getUser(@RequestParam(required = false) String search,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "roleCode", required = false) String roleCode,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
		return ResponseEntity.ok(usersService.getUsers(search, status, roleCode, page, pageSize));
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_USERS.VIEW')")
	@GetMapping(value = "/get-users/{userId}")
	public ResponseEntity<Object> getUserById(@PathVariable("userId") String userId) {
		return usersService.getUser(userId);
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_USERS.CREATE')")
	@PostMapping("add-user")
	public ResponseEntity<Object> addUser(@RequestBody @Valid UsersVOV2 usersVO) {
		return usersService.addUser(usersVO);
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_USERS.EDIT')")
	@PutMapping("add-user/{userId}")
	public ResponseEntity<Object> updateUser(@RequestBody @Valid UsersVOV2 usersVO,
			@PathVariable("userId") String userId) {
		return usersService.updateUser(usersVO, userId);
	}

	@PreAuthorize("hasAuthority('ADMIN.UM_USERS.SUSPEND')")
	@PutMapping("{userId}/{status}/changeStatus")
	public ResponseEntity<Object> changeStatus(@PathVariable("userId") String userId,
											   @PathVariable("status") String status, @RequestBody String request) {

		String decodedUserId = URLDecoder.decode(userId, StandardCharsets.UTF_8);
		return usersService.suspendUser(decodedUserId, status, request);
	}
	
	@PreAuthorize("hasAuthority('ADMIN.UM_USERS.VIEW')")
	@GetMapping("stats")
	public ResponseEntity<Object> usersStats() {
		return ResponseEntity.ok(usersService.getUsersStats());
	}
	
	@PreAuthorize("hasAuthority('ADMIN.UM_USERS.DELETE')")
	@DeleteMapping("delete/{userId}")
	public ResponseEntity<Void> deleteUser(@PathVariable("userId") String userId) {
		return usersService.deleteUser(userId);
	}
}
