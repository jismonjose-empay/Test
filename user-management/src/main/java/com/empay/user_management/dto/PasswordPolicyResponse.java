package com.empay.user_management.dto;

import java.time.LocalDateTime;

import com.empay.user_management.entities.PasswordPolicyEntity;

/**
 * Data transfer object for password policy details and complexity requirements.
 *
 * @param policyId unique database identifier of the policy
 * @param policyCode unique operational code of the policy
 * @param name human-readable name of the policy
 * @param description functional description of the policy
 * @param isActive true if this policy is currently enforced across the system
 * @param minLength minimum acceptable password length
 * @param maxLength maximum acceptable password length
 * @param requireUppercase true if at least one uppercase letter is mandatory
 * @param requireLowercase true if at least one lowercase letter is mandatory
 * @param requireDigit true if at least one numeric digit is mandatory
 * @param requireSpecial true if at least one special character is mandatory
 * @param minCharacterClasses minimum number of character categories required
 * @param checkCommonPasswords true if common password dictionary checks are enforced
 * @param checkUserSimilarity true if similarity to user identity fields is checked
 * @param checkSequential true if sequential alphanumeric sequences are rejected
 * @param expiryDays days before password expiration, zero denotes no expiration
 * @param warnBeforeDays days before expiration to start issuing warning notifications
 * @param minAgeHours minimum hours that must elapse before a password can be changed again
 * @param historyDepth count of previous passwords stored to prevent reuse
 * @param maxFailedAttempts maximum consecutive failed login attempts before lockout
 * @param lockoutMinutes duration in minutes of account lockout
 * @param attemptWindowMinutes rolling window in minutes for counting failed attempts
 * @param lockoutsBeforeDisable number of lockouts before account is administratively disabled
 * @param hashCost hashing cost parameter for password derivation
 * @param updatedBy user identifier that last updated the policy
 * @param updatedAt timestamp when the policy was last updated
 */
public record PasswordPolicyResponse(
	Integer policyId,
	String policyCode,
	String name,
	String description,
	boolean isActive,
	int minLength,
	int maxLength,
	boolean requireUppercase,
	boolean requireLowercase,
	boolean requireDigit,
	boolean requireSpecial,
	int minCharacterClasses,
	boolean checkCommonPasswords,
	boolean checkUserSimilarity,
	boolean checkSequential,
	int expiryDays,
	int warnBeforeDays,
	int minAgeHours,
	int historyDepth,
	int maxFailedAttempts,
	int lockoutMinutes,
	int attemptWindowMinutes,
	int lockoutsBeforeDisable,
	int hashCost,
	String updatedBy,
	LocalDateTime updatedAt
) {
	/**
	 * Converts a PasswordPolicyEntity to a PasswordPolicyResponse DTO.
	 *
	 * @param entity the policy entity to convert
	 * @return the mapped response, or null if entity is null
	 */
	public static PasswordPolicyResponse fromEntity(PasswordPolicyEntity entity) {
		if (entity == null) {
			return null;
		}

		return new PasswordPolicyResponse(
			entity.getPolicyId(),
			entity.getPolicyCode(),
			entity.getName(),
			entity.getDescription(),
			entity.isPolicyActive(),
			entity.getMinLength(),
			entity.getMaxLength(),
			"Y".equalsIgnoreCase(entity.getRequireUppercase()),
			"Y".equalsIgnoreCase(entity.getRequireLowercase()),
			"Y".equalsIgnoreCase(entity.getRequireDigit()),
			"Y".equalsIgnoreCase(entity.getRequireSpecial()),
			entity.getMinCharacterClasses(),
			"Y".equalsIgnoreCase(entity.getCheckCommonPasswords()),
			"Y".equalsIgnoreCase(entity.getCheckUserSimilarity()),
			"Y".equalsIgnoreCase(entity.getCheckSequential()),
			entity.getExpiryDays(),
			entity.getWarnBeforeDays(),
			entity.getMinAgeHours(),
			entity.getHistoryDepth(),
			entity.getMaxFailedAttempts(),
			entity.getLockoutMinutes(),
			entity.getAttemptWindowMinutes(),
			entity.getLockoutsBeforeDisable(),
			entity.getHashCost(),
			entity.getUpdatedBy(),
			entity.getUpdatedAt()
		);
	}
}
