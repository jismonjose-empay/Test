package com.empay.user_management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new password policy.
 *
 * @param policyCode unique identifier code for the policy
 * @param name human-readable policy name
 * @param description functional description
 * @param isActive whether to activate this policy immediately
 * @param minLength minimum length
 * @param maxLength maximum length
 * @param requireUppercase whether uppercase character is required
 * @param requireLowercase whether lowercase character is required
 * @param requireDigit whether numeric digit is required
 * @param requireSpecial whether special character is required
 * @param minCharacterClasses minimum number of character categories required
 * @param checkCommonPasswords whether common password list is checked
 * @param checkUserSimilarity whether similarity with user info is rejected
 * @param checkSequential whether sequential characters are rejected
 * @param expiryDays days until password expires (0 for never)
 * @param warnBeforeDays days before expiry to alert user
 * @param minAgeHours minimum hours before user can change password again
 * @param historyDepth number of previous passwords remembered
 * @param maxFailedAttempts allowed failed attempts before lockout
 * @param lockoutMinutes duration of lockout in minutes
 * @param attemptWindowMinutes window for counting failed attempts
 * @param lockoutsBeforeDisable lockouts before administrative account disable
 * @param hashCost hashing cost factor
 */
public record CreatePasswordPolicyRequest(
	@NotBlank(message = "Policy code must not be blank")
	@Size(max = 40, message = "Policy code cannot exceed 40 characters")
	@Pattern(regexp = "^[A-Z0-9_\\-\\.]+$", message = "Policy code must contain only uppercase letters, numbers, hyphens, dots, or underscores")
	String policyCode,

	@NotBlank(message = "Policy name must not be blank")
	@Size(max = 100, message = "Policy name cannot exceed 100 characters")
	String name,

	@Size(max = 500, message = "Description cannot exceed 500 characters")
	String description,

	Boolean isActive,

	@Min(value = 8, message = "Minimum length must be at least 8 characters")
	Integer minLength,

	@Min(value = 64, message = "Maximum length must be at least 64 characters")
	Integer maxLength,

	Boolean requireUppercase,
	Boolean requireLowercase,
	Boolean requireDigit,
	Boolean requireSpecial,

	@Min(value = 0, message = "Character classes must be between 0 and 4")
	@Max(value = 4, message = "Character classes must be between 0 and 4")
	Integer minCharacterClasses,

	Boolean checkCommonPasswords,
	Boolean checkUserSimilarity,
	Boolean checkSequential,

	@Min(value = 0, message = "Expiry days cannot be negative")
	Integer expiryDays,

	@Min(value = 0, message = "Warn before days cannot be negative")
	Integer warnBeforeDays,

	@Min(value = 0, message = "Minimum age hours cannot be negative")
	Integer minAgeHours,

	@Min(value = 0, message = "History depth cannot be negative")
	Integer historyDepth,

	@Min(value = 1, message = "Max failed attempts must be at least 1")
	Integer maxFailedAttempts,

	@Min(value = 1, message = "Lockout minutes must be at least 1")
	Integer lockoutMinutes,

	@Min(value = 1, message = "Attempt window minutes must be at least 1")
	Integer attemptWindowMinutes,

	@Min(value = 1, message = "Lockouts before disable must be at least 1")
	Integer lockoutsBeforeDisable,

	@Min(value = 10, message = "Hash cost must be between 10 and 14")
	@Max(value = 14, message = "Hash cost must be between 10 and 14")
	Integer hashCost
) {
	public int effectiveMinLength() {
		return (minLength != null) ? minLength : 15;
	}

	public int effectiveMaxLength() {
		return (maxLength != null) ? maxLength : 64;
	}

	public boolean effectiveIsActive() {
		return Boolean.TRUE.equals(isActive);
	}

	public boolean effectiveRequireUppercase() {
		return Boolean.TRUE.equals(requireUppercase);
	}

	public boolean effectiveRequireLowercase() {
		return Boolean.TRUE.equals(requireLowercase);
	}

	public boolean effectiveRequireDigit() {
		return Boolean.TRUE.equals(requireDigit);
	}

	public boolean effectiveRequireSpecial() {
		return Boolean.TRUE.equals(requireSpecial);
	}

	public int effectiveMinCharacterClasses() {
		return (minCharacterClasses != null) ? minCharacterClasses : 0;
	}

	public boolean effectiveCheckCommonPasswords() {
		return (checkCommonPasswords == null) || checkCommonPasswords;
	}

	public boolean effectiveCheckUserSimilarity() {
		return (checkUserSimilarity == null) || checkUserSimilarity;
	}

	public boolean effectiveCheckSequential() {
		return (checkSequential == null) || checkSequential;
	}

	public int effectiveExpiryDays() {
		return (expiryDays != null) ? expiryDays : 0;
	}

	public int effectiveWarnBeforeDays() {
		return (warnBeforeDays != null) ? warnBeforeDays : 14;
	}

	public int effectiveMinAgeHours() {
		return (minAgeHours != null) ? minAgeHours : 24;
	}

	public int effectiveHistoryDepth() {
		return (historyDepth != null) ? historyDepth : 12;
	}

	public int effectiveMaxFailedAttempts() {
		return (maxFailedAttempts != null) ? maxFailedAttempts : 5;
	}

	public int effectiveLockoutMinutes() {
		return (lockoutMinutes != null) ? lockoutMinutes : 30;
	}

	public int effectiveAttemptWindowMinutes() {
		return (attemptWindowMinutes != null) ? attemptWindowMinutes : 15;
	}

	public int effectiveLockoutsBeforeDisable() {
		return (lockoutsBeforeDisable != null) ? lockoutsBeforeDisable : 3;
	}

	public int effectiveHashCost() {
		return (hashCost != null) ? hashCost : 12;
	}
}
