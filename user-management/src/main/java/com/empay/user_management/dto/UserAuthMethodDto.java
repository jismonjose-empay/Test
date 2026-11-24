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
public class UserAuthMethodDto {
	private String provider;
	private String providerName;
	private String providerType; // PASSWORD, OAUTH2, OIDC

	@JsonProperty("isLinked")
	@JsonAlias({"linked", "is_linked"})
	private boolean isLinked;

	public boolean isLinked() {
		return isLinked;
	}

	@JsonProperty("linked")
	public boolean getLinked() {
		return isLinked();
	}

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

	@JsonProperty("isPrimary")
	@JsonAlias({"primary", "is_primary"})
	private boolean isPrimary;

	public boolean isPrimary() {
		return isPrimary;
	}

	@JsonProperty("primary")
	public boolean getPrimary() {
		return isPrimary();
	}

	private String externalSubject;
	private String externalUsername;
	private String externalEmail;
	private LocalDateTime linkedAt;
	private LocalDateTime lastLoginAt;
	private String icon;

	public static class UserAuthMethodDtoBuilder {
		public UserAuthMethodDtoBuilder linkedAt(LocalDateTime linkedAt) {
			this.linkedAt = linkedAt;
			return this;
		}

		public UserAuthMethodDtoBuilder linkedAt(String value) {
			if (value != null && !value.isBlank()) {
				try {
					this.linkedAt = LocalDateTime.parse(value);
				} catch (Exception ignored) {
					// Ignore invalid format
				}
			}
			return this;
		}

		public UserAuthMethodDtoBuilder lastLoginAt(LocalDateTime lastLoginAt) {
			this.lastLoginAt = lastLoginAt;
			return this;
		}

		public UserAuthMethodDtoBuilder lastLoginAt(String value) {
			if (value != null && !value.isBlank()) {
				try {
					this.lastLoginAt = LocalDateTime.parse(value);
				} catch (Exception ignored) {
					// Ignore invalid format
				}
			}
			return this;
		}
	}
}

