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
@Table(name = "USER_STATUS_HISTORY")
@Getter
@Setter
public class UserStatusHistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "history_id")
	private Integer historyId;

	@Column(name = "user_serial_no", nullable = false)
	private Integer userSerialNo;

	@Column(name = "from_status", length = 20)
	private String fromStatus;

	@Column(name = "to_status", nullable = false, length = 20)
	private String toStatus;

	@Column(name = "reason", nullable = false, length = 400)
	private String reason;

	@Column(name = "changed_by", nullable = false, length = 50)
	private String changedBy;

	@Column(name = "changed_at", insertable = false, updatable = false)
	private LocalDateTime changedAt;
}