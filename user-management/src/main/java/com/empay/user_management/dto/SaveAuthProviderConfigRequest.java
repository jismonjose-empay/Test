package com.empay.user_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAuthProviderConfigRequest {

	@NotBlank(message = "Provider ID is required")
	@Size(max = 50, message = "Provider ID must not exceed 50 characters")
	private String providerId;

	@NotBlank(message = "Provider name is required")
	@Size(max = 100, message = "Provider name must not exceed 100 characters")
	private String name;

	@NotBlank(message = "Provider type is required")
	@Size(max = 30, message = "Provider type must not exceed 30 characters")
	private String type; // PASSWORD, OAUTH2, OIDC, SAML2

	@Size(max = 255, message = "Client ID must not exceed 255 characters")
	private String clientId;

	@Size(max = 500, message = "Client secret must not exceed 500 characters")
	private String clientSecret;

	@Size(max = 500, message = "Authorization URL must not exceed 500 characters")
	private String authorizationUrl;

	@Size(max = 500, message = "Token URL must not exceed 500 characters")
	private String tokenUrl;

	@Size(max = 500, message = "User Info URL must not exceed 500 characters")
	private String userInfoUrl;

	@Size(max = 500, message = "Issuer URL must not exceed 500 characters")
	private String issuerUrl;

	@Size(max = 255, message = "Scopes must not exceed 255 characters")
	private String scopes;

	@Size(max = 500, message = "Redirect URI must not exceed 500 characters")
	private String redirectUri;

	private Boolean isEnabled;

	@Size(max = 50, message = "Icon must not exceed 50 characters")
	private String icon;

	@Size(max = 500, message = "Description must not exceed 500 characters")
	private String description;

	private Integer displayOrder;
}

