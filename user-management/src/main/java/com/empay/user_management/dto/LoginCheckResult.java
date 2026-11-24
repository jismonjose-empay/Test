package com.empay.user_management.dto;

public record LoginCheckResult(boolean authenticated, Integer userSerialNumber, String failureReason, String userRole) {

	public LoginCheckResult(boolean authenticated, Integer userSerialNumber, String failureReason) {
		this(authenticated, userSerialNumber, failureReason, null);
	}
}
