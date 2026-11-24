package com.empay.user_management.valueobjects;

import java.time.LocalDateTime;
import java.util.List;

import com.empay.user_management.dto.UserStatusHistoryVo;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsersVOV2 {

	private Integer serialNumber;

	private String userId;

	private String displayName;

	@Email(message = "Invalid email format")
    @Size(max = 60, message = "Email must not exceed 60 characters")
	private String email;

	private String insCode;

	private String status;
	
	private Character isBreakGlass;

	private String specialBadge;

	private String idpSubject;

	private String createdBy;

	private LocalDateTime createdAt;

	private String updatedBy;

	private LocalDateTime updatedAt;

	private String rowVersion;
	
	private List<String> permissions;
	
	private String roleCode;
	
	private String roleName;
	
	private List<String> roleCodes;
	
	private String credentialStatus;
	
	private List<UserStatusHistoryVo> userStatusHistory;

}