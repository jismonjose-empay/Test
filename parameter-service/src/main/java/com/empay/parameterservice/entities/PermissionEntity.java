package com.empay.parameterservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "PERMISSION")
public class PermissionEntity {

	@Id
	@Column(name = "permission_code", length = 60, nullable = false)
	private String permissionCode;

	@Column(name = "module", length = 30, nullable = false)
	private String module;

	@Column(name = "resource", length = 40, nullable = false)
	private String resource;

	@Column(name = "action_name", length = 20, nullable = false)
	private String actionName;

	@Column(name = "description", length = 200, nullable = false)
	private String description;

	@Column(name = "is_sensitive", length = 1, nullable = false)
	private String isSensitive;

	@Column(name = "legacy_record_id")
	private Integer legacyRecordId;

	public boolean isSensitivePermission() {
		return "Y".equalsIgnoreCase(this.isSensitive);
	}
}
