package com.empay.user_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OAuth authentication requests.
 * Supports both standard authorization code exchange (code) and linked identity verification (identity).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequest {

	@NotBlank(message = "Provider is required (e.g. GITHUB, GOOGLE, MICROSOFT)")
	private String provider;

	private String identity;

	private String code;

	private String state;

	private String redirectUri;
}
