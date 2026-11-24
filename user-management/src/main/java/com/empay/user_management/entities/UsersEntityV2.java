package com.empay.user_management.entities;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "MARS_USER")
public class UsersEntityV2 {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "USER_SERIAL_NO")
	private Integer serialNumber;

	@Column(name = "USER_ID", updatable = false, insertable = true)
	private String userId;

	@Column(name = "DISPLAY_NAME")
	private String displayName;

	@Column(name = "EMAIL")
	private String email;

	@Column(name = "INSTITUTION_CODE")
	private String institutionCode;

	@Column(name = "STATUS")
	private String status;

	@Column(name = "IS_BREAK_GLASS")
	private Character isBreakGlass;

	@Column(name = "IDP_SUBJECT")
	private String idpSubject;

	@Column(name = "AUTH_MODE")
	private String authMode;

	@Column(name = "CREATED_BY")
	private String createdBy;

	@Column(name = "CREATED_AT")
	private LocalDateTime createdAt;

	@Column(name = "UPDATED_BY")
	private String updatedBy;

	@Column(name = "UPDATED_AT")
	private LocalDateTime updatedAt;

	@Column(name = "ROW_VERSION")
	private String rowVersion;

	@OneToMany
	@JoinColumn(name = "USER_SERIAL_NO", referencedColumnName = "USER_SERIAL_NO", insertable = false, updatable = false)
	private List<UserRoleEntityV2> userRoles;

}