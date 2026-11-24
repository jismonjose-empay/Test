package com.empay.user_management.entities;

import java.time.LocalDateTime;

import com.empay.user_management.utils.SecretCryptoConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing system-level configuration for an authentication provider / SSO / OAuth method.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AUTH_PROVIDER_CONFIG")
public class AuthProviderConfigEntity {

	@Id
	@Column(name = "provider_id", nullable = false, length = 50)
	private String providerId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "type", nullable = false, length = 30)
	private String type; // PASSWORD, OAUTH2, OIDC, SAML2

	@Column(name = "client_id", length = 255)
	private String clientId;

	@Convert(converter = SecretCryptoConverter.class)
	@Column(name = "client_secret", length = 500)
	private String clientSecret;

	@Column(name = "authorization_url", length = 500)
	private String authorizationUrl;

	@Column(name = "token_url", length = 500)
	private String tokenUrl;

	@Column(name = "user_info_url", length = 500)
	private String userInfoUrl;

	@Column(name = "issuer_url", length = 500)
	private String issuerUrl;

	@Column(name = "scopes", length = 255)
	private String scopes;

	@Column(name = "redirect_uri", length = 500)
	private String redirectUri;

	@Column(name = "is_enabled", nullable = false, length = 1)
	@Builder.Default
	private Character isEnabled = 'Y';

	@Column(name = "icon", length = 50)
	private String icon;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "display_order", nullable = false)
	@Builder.Default
	private Integer displayOrder = 1;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "updated_by", length = 50)
	private String updatedBy;
}

