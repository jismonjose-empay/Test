package com.empay.user_management.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginAttemptResponse {

	private String id;

	private String when;

	private String user;

	private String result;

	private String reason;

	private String sourceIp;

}
