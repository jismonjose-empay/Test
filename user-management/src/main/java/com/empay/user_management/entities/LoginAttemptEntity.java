package com.empay.user_management.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "LOGIN_ATTEMPT")
public class LoginAttemptEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "attempt_id")
	private Long attemptId;

	@Column(name = "user_id", length = 50, nullable = false)
	private String userId;

	@Column(name = "user_serial_no")
	private Integer userSerialNumber;

	@Column(name = "succeeded", nullable = false)
	private Character succeeded;

	@Column(name = "failure_reason", length = 40)
	private String failureReason;

	@Column(name = "source_ip", length = 45)
	private String sourceIp;

	@Column(name = "user_agent", length = 300)
	private String userAgent;

	@Column(name = "session_id", length = 64)
	private String sessionId;

	@Column(name = "attempted_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime attemptedAt;
}
