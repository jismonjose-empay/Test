package com.empay.user_management.dto;

import java.time.LocalDateTime;

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
public class AuthProviderConfigDto {

	private String providerId;
	private String name;
	private String type; // PASSWORD, OAUTH2, OIDC, SAML2
	private String clientId;
	private String clientSecretMasked;
	private boolean hasClientSecret;
	private String authorizationUrl;
	private String tokenUrl;
	private String userInfoUrl;
	private String issuerUrl;
	private String scopes;
	private String redirectUri;

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

	private String icon;
	private String description;
	private int displayOrder;
	private LocalDateTime updatedAt;
	private String updatedBy;
}

