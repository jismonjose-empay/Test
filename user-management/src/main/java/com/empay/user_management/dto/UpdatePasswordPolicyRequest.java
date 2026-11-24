package com.empay.user_management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing password policy.
 *
 * @param name updated policy name
 * @param description updated description
 * @param isActive whether to activate this policy
 * @param minLength updated minimum length
 * @param maxLength updated maximum length
 * @param requireUppercase whether uppercase character is required
 * @param requireLowercase whether lowercase character is required
 * @param requireDigit whether numeric digit is required
 * @param requireSpecial whether special character is required
 * @param minCharacterClasses minimum number of character categories required
 * @param checkCommonPasswords whether common password list is checked
 * @param checkUserSimilarity whether similarity with user info is rejected
 * @param checkSequential whether sequential characters are rejected
 * @param expiryDays days until password expires
 * @param warnBeforeDays days before expiry to alert user
 * @param minAgeHours minimum hours before user can change password again
 * @param historyDepth number of previous passwords remembered
 * @param maxFailedAttempts allowed failed attempts before lockout
 * @param lockoutMinutes duration of lockout in minutes
 * @param attemptWindowMinutes window for counting failed attempts
 * @param lockoutsBeforeDisable lockouts before account disable
 * @param hashCost hashing cost factor
 */
public record UpdatePasswordPolicyRequest(
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
}
