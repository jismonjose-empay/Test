package com.empay.user_management.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "APP_ROLE")
public class AppRoleEntity {

	@Id
	@Column(name = "role_code", length = 40, nullable = false)
	private String roleCode;

	@Column(name = "name", length = 100, nullable = false)
	private String name;

	@Column(name = "description", length = 300)
	private String description;

	@Column(name = "is_system", length = 1, nullable = false)
	private String isSystem;

	@Column(name = "created_by", length = 50, nullable = false)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "ROLE_PERMISSION",
		joinColumns = @JoinColumn(name = "role_code"),
		inverseJoinColumns = @JoinColumn(name = "permission_code")
	)
	@Builder.Default
	private Set<PermissionEntity> permissions = new HashSet<>();

	public boolean isSystemRole() {
		return "Y".equalsIgnoreCase(this.isSystem);
	}
}
