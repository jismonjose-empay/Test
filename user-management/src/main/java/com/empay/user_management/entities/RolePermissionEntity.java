package com.empay.user_management.entities;

import java.time.LocalDateTime;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Junction entity representing the assignment of a permission to a role.
 * Maps to the ROLE_PERMISSION table with audit tracking for who granted the permission.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ROLE_PERMISSION")
public class RolePermissionEntity {

	@EmbeddedId
	private RolePermissionId id;

	@Column(name = "granted_by", length = 50, nullable = false)
	private String grantedBy;

	@Column(name = "granted_at", nullable = false)
	private LocalDateTime grantedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_code", insertable = false, updatable = false)
	private AppRoleEntity role;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "permission_code", insertable = false, updatable = false)
	private PermissionEntity permission;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RolePermissionEntity that = (RolePermissionEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
