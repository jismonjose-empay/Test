package com.empay.user_management.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "PERMISSION_CONFLICT")
public class PermissionConflictEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "conflict_id")
	private Long conflictId;

	@Column(name = "permission_code_a", length = 60, nullable = false)
	private String permissionCodeA;

	@Column(name = "permission_code_b", length = 60, nullable = false)
	private String permissionCodeB;

	@Column(name = "severity", length = 10, nullable = false)
	private String severity;

	@Column(name = "rationale", length = 400, nullable = false)
	private String rationale;

	@Column(name = "derived_from_role_conflict")
	private Long derivedFromRoleConflict;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "derived_from_role_conflict", insertable = false, updatable = false)
	private RoleConflictEntity derivedRoleConflict;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "permission_code_a", insertable = false, updatable = false)
	private PermissionEntity permissionA;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "permission_code_b", insertable = false, updatable = false)
	private PermissionEntity permissionB;

	public boolean isBlocking() {
		return "BLOCK".equalsIgnoreCase(this.severity);
	}
}
