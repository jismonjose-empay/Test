package com.empay.user_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureAuthMethodRequest {

	@NotBlank(message = "Provider identifier is required")
	private String provider;

	private String externalSubject;

	private String externalUsername;

	private String externalEmail;

	@Builder.Default
	private Boolean isEnabled = true;

	@Builder.Default
	private Boolean isPrimary = false;

	private String metadataJson;
}

