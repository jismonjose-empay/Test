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
 * Entity representing a password policy configuration.
 * Maps to the PASSWORD_POLICY table and defines password complexity and lockout parameters.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "PASSWORD_POLICY")
public class PasswordPolicyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "policy_id", nullable = false)
	private Integer policyId;

	@Column(name = "policy_code", length = 40, nullable = false, unique = true)
	private String policyCode;

	@Column(name = "name", length = 100, nullable = false)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@Builder.Default
	@Column(name = "is_active", length = 1, nullable = false)
	private String isActive = "N";

	@Builder.Default
	@Column(name = "min_length", nullable = false)
	private Integer minLength = 15;

	@Builder.Default
	@Column(name = "max_length", nullable = false)
	private Integer maxLength = 64;

	@Builder.Default
	@Column(name = "require_uppercase", length = 1, nullable = false)
	private String requireUppercase = "N";

	@Builder.Default
	@Column(name = "require_lowercase", length = 1, nullable = false)
	private String requireLowercase = "N";

	@Builder.Default
	@Column(name = "require_digit", length = 1, nullable = false)
	private String requireDigit = "N";

	@Builder.Default
	@Column(name = "require_special", length = 1, nullable = false)
	private String requireSpecial = "N";

	@Builder.Default
	@Column(name = "min_character_classes", nullable = false)
	private Integer minCharacterClasses = 0;

	@Builder.Default
	@Column(name = "check_common_passwords", length = 1, nullable = false)
	private String checkCommonPasswords = "Y";

	@Builder.Default
	@Column(name = "check_user_similarity", length = 1, nullable = false)
	private String checkUserSimilarity = "Y";

	@Builder.Default
	@Column(name = "check_sequential", length = 1, nullable = false)
	private String checkSequential = "Y";

	@Builder.Default
	@Column(name = "expiry_days", nullable = false)
	private Integer expiryDays = 0;

	@Builder.Default
	@Column(name = "warn_before_days", nullable = false)
	private Integer warnBeforeDays = 14;

	@Builder.Default
	@Column(name = "min_age_hours", nullable = false)
	private Integer minAgeHours = 24;

	@Builder.Default
	@Column(name = "history_depth", nullable = false)
	private Integer historyDepth = 12;

	@Builder.Default
	@Column(name = "max_failed_attempts", nullable = false)
	private Integer maxFailedAttempts = 5;

	@Builder.Default
	@Column(name = "lockout_minutes", nullable = false)
	private Integer lockoutMinutes = 30;

	@Builder.Default
	@Column(name = "attempt_window_minutes", nullable = false)
	private Integer attemptWindowMinutes = 15;

	@Builder.Default
	@Column(name = "lockouts_before_disable", nullable = false)
	private Integer lockoutsBeforeDisable = 3;

	@Builder.Default
	@Column(name = "hash_cost", nullable = false)
	private Integer hashCost = 12;

	@Column(name = "updated_by", length = 50)
	private String updatedBy;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public boolean isPolicyActive() {
		return "Y".equalsIgnoreCase(this.isActive);
	}

	public void setPolicyActive(boolean active) {
		this.isActive = active ? "Y" : "N";
	}
}
