package com.empay.user_management.service;

import com.empay.user_management.dto.AuditLogPageResponse;
import com.empay.user_management.valueobjects.UserActivityTrackingVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserActivityTrackingService {

	default void saveUserActivity(UserActivityTrackingVO vo, HttpServletRequest request) {
		saveUserActivity(vo, request, null);
	}

	void saveUserActivity(UserActivityTrackingVO vo, HttpServletRequest request, HttpServletResponse response);

	AuditLogPageResponse getUserActivityList(int page, int size, String sortBy, String sortDir, String search);

}
