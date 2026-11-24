package com.empay.user_management.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

public interface LoginService {

	Object getLoginAttempts();

	ResponseEntity<Object> changeUserPassword(Map<String, String> inputDataMap);

	ResponseEntity<Object> validateUserLogin(Map<String, String> inputDataMap, HttpServletRequest request);

	ResponseEntity<Object> logout(HttpServletRequest request);

	ResponseEntity<Object> getCurrentUserSession(HttpServletRequest request);
}
