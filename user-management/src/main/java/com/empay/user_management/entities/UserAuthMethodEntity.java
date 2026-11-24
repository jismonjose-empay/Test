package com.empay.user_management.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a user-specific authentication method link (e.g. Local Password, GitHub OAuth, Google, Microsoft, Keycloak SSO).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "USER_AUTH_METHOD")
public class UserAuthMethodEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "auth_method_id")
	private Long authMethodId;

	@Column(name = "user_serial_no", nullable = false)
	private Integer userSerialNo;

	@Column(name = "provider", nullable = false, length = 40)
	private String provider;

	@Column(name = "external_subject", length = 255)
	private String externalSubject;

	@Column(name = "external_username", length = 100)
	private String externalUsername;

	@Column(name = "external_email", length = 150)
	private String externalEmail;

	@Column(name = "is_enabled", nullable = false, length = 1)
	@Builder.Default
	private Character isEnabled = 'Y';

	@Column(name = "is_primary", nullable = false, length = 1)
	@Builder.Default
	private Character isPrimary = 'N';

	@Column(name = "linked_by", nullable = false, length = 50)
	private String linkedBy;

	@Column(name = "linked_at", nullable = false)
	private LocalDateTime linkedAt;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "metadata_json", length = 1000)
	private String metadataJson;
}

