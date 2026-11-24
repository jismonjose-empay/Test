package com.empay.user_management.dto;

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
public class AuthProviderDto {
	private String id;
	private String name;
	private String type; // PASSWORD, OAUTH2, OIDC
	private String icon; // github, google, microsoft, lock, key

	@JsonProperty("isEnabled")
	@JsonAlias({"enabled", "is_enabled"})
	private boolean isEnabled;

	public boolean isEnabled() {
		return isEnabled;
	}

	@JsonProperty("enabled")
	public boolean getEnabled() {
		return isEnabled();
	}

	private String description;
	private String authorizationUrl;
}

