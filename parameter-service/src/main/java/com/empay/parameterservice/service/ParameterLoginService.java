package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

public interface ParameterLoginService {

	ResponseEntity<Object> validateParameterLogin(HttpServletRequest request);
}