package com.empay.user_management.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusHistoryVo {

	private Integer historyId;

	private Integer userSerialNo;

	private String fromStatus;

	private String toStatus;

	private String reason;

	private String changedBy;

	private String changedAt;

}
