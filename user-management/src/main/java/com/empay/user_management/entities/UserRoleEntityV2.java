package com.empay.user_management.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "USER_ROLE")
@IdClass(UserRoleIdV2.class)
public class UserRoleEntityV2 {

	@Id
	@Column(name = "USER_SERIAL_NO")
	private Integer serialNumber;

	@Column(name = "ROLE_CODE")
	private String roleCode;

	@Column(name = "GRANTED_BY")
	private String grantedBy;

	@Column(name = "GRANTED_AT")
	private LocalDateTime grantedAt;
	
	@ManyToOne
	@JoinColumn(name = "role_code", referencedColumnName = "ROLE_CODE", insertable = false, updatable = false)
	private AppRoleEntity appRole;
}
