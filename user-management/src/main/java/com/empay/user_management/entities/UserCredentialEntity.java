package com.empay.user_management.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "USER_CREDENTIAL")
public class UserCredentialEntity {

	@Id
	@Column(name = "user_serial_no", nullable = false)
	private Integer userSerialNo;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "hash_cost", nullable = false)
	private Integer hashCost = 12;

	@Column(name = "password_changed_at", nullable = false)
	private LocalDateTime passwordChangedAt;

	@Column(name = "password_expires_at")
	private LocalDateTime passwordExpiresAt;

	@Column(name = "must_change", nullable = false, length = 1)
	private String mustChange = "Y";

	@Column(name = "must_change_reason", length = 40)
	private String mustChangeReason;

	@Column(name = "failed_attempts", nullable = false)
	private Integer failedAttempts = 0;

	@Column(name = "first_failed_at")
	private LocalDateTime firstFailedAt;

	@Column(name = "locked_until")
	private LocalDateTime lockedUntil;

	@Column(name = "lockout_count", nullable = false)
	private Integer lockoutCount = 0;

	@Column(name = "last_success_at")
	private LocalDateTime lastSuccessAt;

	@Column(name = "last_success_ip", length = 45)
	private String lastSuccessIp;

	@Column(name = "previous_success_at")
	private LocalDateTime previousSuccessAt;

	@Column(name = "previous_success_ip", length = 45)
	private String previousSuccessIp;
}
