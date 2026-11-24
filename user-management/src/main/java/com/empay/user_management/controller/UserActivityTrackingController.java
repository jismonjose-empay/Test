package com.empay.user_management.controller;

import com.empay.user_management.dto.AuditLogPageResponse;
import com.empay.user_management.service.UserActivityTrackingService;
import com.empay.user_management.valueobjects.UserActivityTrackingVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserActivityTrackingController {

	private final UserActivityTrackingService userActivityTrackingService;

	@PostMapping(value = {"/save-user-activity"})
	public ResponseEntity<Object> saveUserActivity(@RequestBody(required = false) UserActivityTrackingVO vo,
			HttpServletRequest request, HttpServletResponse response) {
		userActivityTrackingService.saveUserActivity(vo, request, response);
		return new ResponseEntity<>("Activity Logged Successfully", HttpStatus.OK);
	}
	
	@PreAuthorize("hasAuthority('ADMIN.UM_AUDIT.VIEW')")
	@GetMapping(value = "/get-user-activity-list")
	public AuditLogPageResponse getUserActivityList(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
													@RequestParam(defaultValue = "serialNumber") String sortBy,
                                                    @RequestParam(defaultValue = "asc") String sortDir,
													@RequestParam(required = false) String search) {
		return userActivityTrackingService.getUserActivityList(page, size, sortBy, sortDir, search);
	}
}
