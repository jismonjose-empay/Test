package com.empay.user_management.entities;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RolePermissionId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "role_code", length = 40, nullable = false)
	private String roleCode;

	@Column(name = "permission_code", length = 60, nullable = false)
	private String permissionCode;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RolePermissionId that)) return false;
		return Objects.equals(roleCode, that.roleCode) && Objects.equals(permissionCode, that.permissionCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(roleCode, permissionCode);
	}
}
