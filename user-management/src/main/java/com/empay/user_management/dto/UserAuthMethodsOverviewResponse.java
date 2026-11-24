package com.empay.user_management.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthMethodsOverviewResponse {
	private String userId;
	private Integer userSerialNumber;
	private String displayName;
	private String email;
	private String status;

	@JsonProperty("isBreakGlass")
	@JsonAlias({"breakGlass", "is_break_glass"})
	private Boolean isBreakGlass;

	@JsonProperty("breakGlass")
	public Boolean getBreakGlass() {
		return isBreakGlass;
	}

	private String primaryMethod;
	private List<UserAuthMethodDto> methods;
}

