package com.empay.user_management.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence entity for role permission audit history records.
 * Tracks grant and revoke actions performed on role permission mappings.
 */
@Entity
@Table(name = "ROLE_PERMISSION_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionHistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "history_id")
	private Long historyId;

	@Column(name = "role_code", length = 40, nullable = false)
	private String roleCode;

	@Column(name = "permission_code", length = 60, nullable = false)
	private String permissionCode;

	@Column(name = "action_taken", length = 10, nullable = false)
	private String actionTaken;

	@Column(name = "changed_by", length = 50, nullable = false)
	private String changedBy;

	@Column(name = "changed_at", nullable = false)
	private LocalDateTime changedAt;
}
