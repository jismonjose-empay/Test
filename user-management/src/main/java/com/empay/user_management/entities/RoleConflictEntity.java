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
@Table(name = "ROLE_CONFLICT")
public class RoleConflictEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "conflict_id")
	private Long conflictId;

	@Column(name = "role_code_a", length = 40, nullable = false)
	private String roleCodeA;

	@Column(name = "role_code_b", length = 40, nullable = false)
	private String roleCodeB;

	@Column(name = "severity", length = 10, nullable = false)
	private String severity;

	@Column(name = "rationale", length = 400, nullable = false)
	private String rationale;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_code_a", insertable = false, updatable = false)
	private AppRoleEntity roleA;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_code_b", insertable = false, updatable = false)
	private AppRoleEntity roleB;

	public boolean isBlocking() {
		return "BLOCK".equalsIgnoreCase(this.severity);
	}
}
