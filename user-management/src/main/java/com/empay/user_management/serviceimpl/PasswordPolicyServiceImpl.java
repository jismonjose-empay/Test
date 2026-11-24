package com.empay.user_management.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.user_management.dto.CreatePasswordPolicyRequest;
import com.empay.user_management.dto.PasswordPoliciesOverviewResponse;
import com.empay.user_management.dto.PasswordPolicyResponse;
import com.empay.user_management.dto.PasswordPolicySummaryResponse;
import com.empay.user_management.dto.PasswordValidationResult;
import com.empay.user_management.dto.UpdatePasswordPolicyRequest;
import com.empay.user_management.dto.UserContext;
import com.empay.user_management.entities.PasswordPolicyEntity;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.PasswordBlocklistRepository;
import com.empay.user_management.repository.PasswordPolicyRepository;
import com.empay.user_management.service.PasswordPolicyService;
import com.empay.user_management.utils.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

/**
 * Service implementation for managing password policies and evaluating password validity.
 */
@Service
@Transactional
@Log4j2
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

	private static final String DEFAULT_ACTOR = "SYSTEM";
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");
	private static final String RESOURCE_NAME = "PasswordPolicy";
	private static final String FIELD_ID = "policyId";
	private static final String FIELD_CODE = "policyCode";

	private static final Set<String> COMMON_PASSWORDS_FALLBACK = Set.of(
		"password", "password123", "123456", "12345678", "123456789", "qwerty",
		"admin", "admin123", "welcome", "welcome1", "letmein", "monkey",
		"dragon", "master", "pass1234", "empay123", "mars1234"
	);

	private final PasswordPolicyRepository passwordPolicyRepository;
	private final PasswordBlocklistRepository passwordBlocklistRepository;
	private final JwtUtils jwtUtils;

	@Autowired
	public PasswordPolicyServiceImpl(
			PasswordPolicyRepository passwordPolicyRepository,
			PasswordBlocklistRepository passwordBlocklistRepository,
			@Autowired(required = false) JwtUtils jwtUtils) {
		this.passwordPolicyRepository = passwordPolicyRepository;
		this.passwordBlocklistRepository = passwordBlocklistRepository;
		this.jwtUtils = jwtUtils;
	}

	@Override
	@Transactional(readOnly = true)
	public PasswordPoliciesOverviewResponse getPoliciesOverview() {
		log.info("Fetching password policies overview and active policy details");
		List<PasswordPolicySummaryResponse> summaryList = passwordPolicyRepository.findAllByOrderByPolicyIdAsc().stream()
			.map(PasswordPolicySummaryResponse::fromEntity)
			.toList();

		PasswordPolicyResponse activePolicyResponse = passwordPolicyRepository.findByIsActive("Y")
			.map(PasswordPolicyResponse::fromEntity)
			.orElse(null);

		return PasswordPoliciesOverviewResponse.of(summaryList, activePolicyResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PasswordPolicyResponse> getAllPolicies() {
		log.info("Fetching all password policies");
		return passwordPolicyRepository.findAllByOrderByPolicyIdAsc().stream()
			.map(PasswordPolicyResponse::fromEntity)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PasswordPolicyResponse getPolicyById(Integer id) {
		log.info("Fetching password policy by ID: {}", id);
		PasswordPolicyEntity entity = findPolicyById(id);
		return PasswordPolicyResponse.fromEntity(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public PasswordPolicyResponse getPolicyByCode(String code) {
		log.info("Fetching password policy by code: {}", code);
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Policy code must not be blank");
		}
		PasswordPolicyEntity entity = passwordPolicyRepository.findByPolicyCode(code.trim().toUpperCase(Locale.ROOT))
			.orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, FIELD_CODE, "Password policy not found with code: " + code));
		return PasswordPolicyResponse.fromEntity(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public PasswordPolicyResponse getActivePolicy() {
		log.info("Fetching active password policy");
		try {
			List<PasswordPolicyEntity> activeList = passwordPolicyRepository.findActivePolicies();
			if (activeList != null && !activeList.isEmpty()) {
				return PasswordPolicyResponse.fromEntity(activeList.get(0));
			}
		} catch (Exception ex) {
			log.debug("findActivePolicies query failed or not supported in mock: {}", ex.getMessage());
		}
		PasswordPolicyEntity entity = passwordPolicyRepository.findByIsActive("Y")
			.orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "isActive", "No active password policy found"));
		return PasswordPolicyResponse.fromEntity(entity);
	}

	@Override
	public PasswordPolicyResponse createPolicy(CreatePasswordPolicyRequest request, HttpServletRequest httpRequest) {
		log.info("Creating password policy with code: {}", request.policyCode());
		String normalizedCode = request.policyCode().trim().toUpperCase(Locale.ROOT);

		if (passwordPolicyRepository.existsByPolicyCode(normalizedCode)) {
			throw new ResourceAlreadyExistsException(RESOURCE_NAME, FIELD_CODE, "Password policy already exists with code: " + normalizedCode);
		}

		validatePolicyRules(
			request.effectiveMinLength(),
			request.effectiveMaxLength(),
			request.effectiveExpiryDays(),
			request.effectiveWarnBeforeDays(),
			request.effectiveMinAgeHours(),
			request.effectiveMinCharacterClasses(),
			request.effectiveHashCost()
		);

		String actor = resolveActor(httpRequest);
		LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);

		if (request.effectiveIsActive()) {
			deactivateExistingActivePolicy(null, actor, now);
		}

		PasswordPolicyEntity entity = PasswordPolicyEntity.builder()
			.policyCode(normalizedCode)
			.name(request.name().trim())
			.description(request.description() != null ? request.description().trim() : null)
			.isActive(request.effectiveIsActive() ? "Y" : "N")
			.minLength(request.effectiveMinLength())
			.maxLength(request.effectiveMaxLength())
			.requireUppercase(request.effectiveRequireUppercase() ? "Y" : "N")
			.requireLowercase(request.effectiveRequireLowercase() ? "Y" : "N")
			.requireDigit(request.effectiveRequireDigit() ? "Y" : "N")
			.requireSpecial(request.effectiveRequireSpecial() ? "Y" : "N")
			.minCharacterClasses(request.effectiveMinCharacterClasses())
			.checkCommonPasswords(request.effectiveCheckCommonPasswords() ? "Y" : "N")
			.checkUserSimilarity(request.effectiveCheckUserSimilarity() ? "Y" : "N")
			.checkSequential(request.effectiveCheckSequential() ? "Y" : "N")
			.expiryDays(request.effectiveExpiryDays())
			.warnBeforeDays(request.effectiveWarnBeforeDays())
			.minAgeHours(request.effectiveMinAgeHours())
			.historyDepth(request.effectiveHistoryDepth())
			.maxFailedAttempts(request.effectiveMaxFailedAttempts())
			.lockoutMinutes(request.effectiveLockoutMinutes())
			.attemptWindowMinutes(request.effectiveAttemptWindowMinutes())
			.lockoutsBeforeDisable(request.effectiveLockoutsBeforeDisable())
			.hashCost(request.effectiveHashCost())
			.updatedBy(actor)
			.updatedAt(now)
			.build();

		PasswordPolicyEntity saved = passwordPolicyRepository.saveAndFlush(entity);
		return PasswordPolicyResponse.fromEntity(saved);
	}

	@Override
	public PasswordPolicyResponse updatePolicy(Integer id, UpdatePasswordPolicyRequest request, HttpServletRequest httpRequest) {
		log.info("Updating password policy ID: {}", id);
		PasswordPolicyEntity entity = findPolicyById(id);

		int minLength = (request.minLength() != null) ? request.minLength() : entity.getMinLength();
		int maxLength = (request.maxLength() != null) ? request.maxLength() : entity.getMaxLength();
		int expiryDays = (request.expiryDays() != null) ? request.expiryDays() : entity.getExpiryDays();
		int warnBeforeDays = (request.warnBeforeDays() != null) ? request.warnBeforeDays() : entity.getWarnBeforeDays();
		int minAgeHours = (request.minAgeHours() != null) ? request.minAgeHours() : entity.getMinAgeHours();
		int minClasses = (request.minCharacterClasses() != null) ? request.minCharacterClasses() : entity.getMinCharacterClasses();
		int hashCost = (request.hashCost() != null) ? request.hashCost() : entity.getHashCost();

		validatePolicyRules(minLength, maxLength, expiryDays, warnBeforeDays, minAgeHours, minClasses, hashCost);

		String actor = resolveActor(httpRequest);
		LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);

		if (Boolean.TRUE.equals(request.isActive()) && !entity.isPolicyActive()) {
			deactivateExistingActivePolicy(entity.getPolicyId(), actor, now);
			entity.setPolicyActive(true);
		}

		applyUpdateFields(entity, request);
		entity.setUpdatedBy(actor);
		entity.setUpdatedAt(now);

		PasswordPolicyEntity saved = passwordPolicyRepository.saveAndFlush(entity);
		return PasswordPolicyResponse.fromEntity(saved);
	}

	@Override
	public PasswordPolicyResponse activatePolicy(Integer id, HttpServletRequest httpRequest) {
		log.info("Activating password policy ID: {}", id);
		PasswordPolicyEntity target = findPolicyById(id);

		if (target.isPolicyActive()) {
			return PasswordPolicyResponse.fromEntity(target);
		}

		String actor = resolveActor(httpRequest);
		LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);

		deactivateExistingActivePolicy(target.getPolicyId(), actor, now);

		target.setPolicyActive(true);
		target.setUpdatedBy(actor);
		target.setUpdatedAt(now);

		PasswordPolicyEntity saved = passwordPolicyRepository.saveAndFlush(target);
		return PasswordPolicyResponse.fromEntity(saved);
	}

	@Override
	public void deletePolicy(Integer id) {
		log.info("Deleting password policy ID: {}", id);
		PasswordPolicyEntity entity = findPolicyById(id);

		if (entity.isPolicyActive()) {
			throw new IllegalArgumentException("Cannot delete active password policy. Only inactive password policies can be deleted.");
		}

		passwordPolicyRepository.delete(entity);
		passwordPolicyRepository.flush();
	}

	@Override
	public void deletePolicyByCode(String code) {
		log.info("Deleting password policy by code: {}", code);
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Policy code must not be blank");
		}
		PasswordPolicyEntity entity = passwordPolicyRepository.findByPolicyCode(code.trim().toUpperCase(Locale.ROOT))
			.orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, FIELD_CODE, "Password policy not found with code: " + code));

		if (entity.isPolicyActive()) {
			throw new IllegalArgumentException("Cannot delete active password policy. Only inactive password policies can be deleted.");
		}

		passwordPolicyRepository.delete(entity);
		passwordPolicyRepository.flush();
	}

	@Override
	@Transactional(readOnly = true)
	public PasswordValidationResult validatePassword(String rawPassword, UserContext userContext) {
		return doValidatePassword(rawPassword, userContext);
	}

	@Override
	@Transactional(readOnly = true)
	public PasswordValidationResult validatePassword(String rawPassword) {
		return doValidatePassword(rawPassword, UserContext.empty());
	}

	private PasswordValidationResult doValidatePassword(String rawPassword, UserContext userContext) {
		if (rawPassword == null || rawPassword.isEmpty()) {
			return PasswordValidationResult.failure(List.of("Password must not be empty"));
		}

		PasswordPolicyEntity policy = getActivePolicyEntityOrDefault();

		List<String> violations = new ArrayList<>();

		checkLength(rawPassword, policy, violations);
		checkCharacterRequirements(rawPassword, policy, violations);
		checkSequentialCharacters(rawPassword, policy, violations);
		checkCommonBlocklist(rawPassword, policy, violations);
		checkIdentitySimilarity(rawPassword, userContext, policy, violations);

		if (violations.isEmpty()) {
			return PasswordValidationResult.success();
		}
		return PasswordValidationResult.failure(violations);
	}

	private PasswordPolicyEntity getActivePolicyEntityOrDefault() {
		try {
			List<PasswordPolicyEntity> activeList = passwordPolicyRepository.findActivePolicies();
			if (activeList != null && !activeList.isEmpty()) {
				return activeList.get(0);
			}
		} catch (Exception ex) {
			log.debug("findActivePolicies query failed or not supported in mock: {}", ex.getMessage());
		}
		return passwordPolicyRepository.findByIsActive("Y")
			.orElseGet(this::createDefaultFallbackPolicy);
	}

	private void checkLength(String password, PasswordPolicyEntity policy, List<String> violations) {
		int min = policy.getMinLength() != null ? policy.getMinLength() : 8;
		int max = policy.getMaxLength() != null ? policy.getMaxLength() : 64;
		if (password.length() < min) {
			violations.add(String.format("Password length must be at least %d characters", min));
		}
		if (password.length() > max) {
			violations.add(String.format("Password length must not exceed %d characters", max));
		}
	}

	private void checkCharacterRequirements(String password, PasswordPolicyEntity policy, List<String> violations) {
		boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
		boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
		boolean hasDigit = password.chars().anyMatch(Character::isDigit);
		boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

		if ("Y".equalsIgnoreCase(policy.getRequireUppercase()) && !hasUpper) {
			violations.add("Password must contain at least one uppercase letter");
		}
		if ("Y".equalsIgnoreCase(policy.getRequireLowercase()) && !hasLower) {
			violations.add("Password must contain at least one lowercase letter");
		}
		if ("Y".equalsIgnoreCase(policy.getRequireDigit()) && !hasDigit) {
			violations.add("Password must contain at least one numeric digit");
		}
		if ("Y".equalsIgnoreCase(policy.getRequireSpecial()) && !hasSpecial) {
			violations.add("Password must contain at least one special character");
		}

		int categoriesPresent = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
		if (policy.getMinCharacterClasses() > 0 && categoriesPresent < policy.getMinCharacterClasses()) {
			violations.add(String.format(
				"Password must contain characters from at least %d distinct character categories (uppercase, lowercase, digits, special characters)",
				policy.getMinCharacterClasses()
			));
		}
	}

	private void checkSequentialCharacters(String password, PasswordPolicyEntity policy, List<String> violations) {
		if ("Y".equalsIgnoreCase(policy.getCheckSequential()) && containsSequentialChars(password, 3)) {
			violations.add("Password must not contain sequential character sequences (such as 123, abc, or cba)");
		}
	}

	private void checkCommonBlocklist(String password, PasswordPolicyEntity policy, List<String> violations) {
		if ("Y".equalsIgnoreCase(policy.getCheckCommonPasswords())) {
			String lower = password.toLowerCase(Locale.ROOT);
			boolean inDatabase = passwordBlocklistRepository.existsById(lower);
			boolean inFallback = COMMON_PASSWORDS_FALLBACK.contains(lower);
			if (inDatabase || inFallback) {
				violations.add("Password is too common and matches an entry on the password blocklist");
			}
		}
	}

	private void checkIdentitySimilarity(String password, UserContext userContext, PasswordPolicyEntity policy, List<String> violations) {
		if ("Y".equalsIgnoreCase(policy.getCheckUserSimilarity()) && userContext != null) {
			String lowerPassword = password.toLowerCase(Locale.ROOT);
			if (isSimilarToIdentity(lowerPassword, userContext.username())
					|| isSimilarToIdentity(lowerPassword, userContext.email())
					|| isSimilarToIdentity(lowerPassword, userContext.fullName())) {
				violations.add("Password must not contain parts of your username, email, or full name");
			}
		}
	}

	private boolean isSimilarToIdentity(String lowerPassword, String identityField) {
		if (identityField == null || identityField.isBlank()) {
			return false;
		}

		String cleaned = identityField.toLowerCase(Locale.ROOT);
		if (cleaned.contains("@")) {
			cleaned = cleaned.substring(0, cleaned.indexOf('@'));
		}

		String[] tokens = cleaned.split("[._\\-\\s]+");
		for (String token : tokens) {
			if (token.length() >= 3 && lowerPassword.contains(token)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsSequentialChars(String password, int sequenceLength) {
		if (password == null || password.length() < sequenceLength) {
			return false;
		}

		for (int i = 0; i <= password.length() - sequenceLength; i++) {
			if (isContiguousAscending(password, i, sequenceLength) || isContiguousDescending(password, i, sequenceLength)) {
				return true;
			}
		}
		return false;
	}

	private boolean isContiguousAscending(String text, int start, int length) {
		for (int j = 0; j < length - 1; j++) {
			char curr = text.charAt(start + j);
			char next = text.charAt(start + j + 1);
			if (next - curr != 1) {
				return false;
			}
		}
		return true;
	}

	private boolean isContiguousDescending(String text, int start, int length) {
		for (int j = 0; j < length - 1; j++) {
			char curr = text.charAt(start + j);
			char next = text.charAt(start + j + 1);
			if (curr - next != 1) {
				return false;
			}
		}
		return true;
	}

	private void validatePolicyRules(
			int minLength,
			int maxLength,
			int expiryDays,
			int warnBeforeDays,
			int minAgeHours,
			int minClasses,
			int hashCost) {
		if (minLength < 8) {
			throw new IllegalArgumentException("Minimum length must be at least 8 characters");
		}
		if (maxLength < 64) {
			throw new IllegalArgumentException("Maximum length must be at least 64 characters");
		}
		if (minLength > maxLength) {
			throw new IllegalArgumentException("Minimum length cannot exceed maximum length");
		}
		if (expiryDays > 0 && minAgeHours >= expiryDays * 24) {
			throw new IllegalArgumentException("Minimum age hours must be less than expiry days in hours");
		}
		if (expiryDays > 0 && warnBeforeDays >= expiryDays) {
			throw new IllegalArgumentException("Warn before days must be less than expiry days");
		}
		if (minClasses < 0 || minClasses > 4) {
			throw new IllegalArgumentException("Character classes must be between 0 and 4");
		}
		if (hashCost < 10 || hashCost > 14) {
			throw new IllegalArgumentException("Hash cost must be between 10 and 14");
		}
	}

	private void deactivateExistingActivePolicy(Integer excludeId, String actor, LocalDateTime now) {
		Optional<PasswordPolicyEntity> existingActive = passwordPolicyRepository.findByIsActive("Y");
		if (existingActive.isPresent()) {
			PasswordPolicyEntity prev = existingActive.get();
			if (!Objects.equals(prev.getPolicyId(), excludeId)) {
				prev.setPolicyActive(false);
				prev.setUpdatedBy(actor);
				prev.setUpdatedAt(now);
				passwordPolicyRepository.saveAndFlush(prev);
			}
		}
	}

	private PasswordPolicyEntity findPolicyById(Integer id) {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException("Policy ID must be a positive integer");
		}
		return passwordPolicyRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, FIELD_ID, "Password policy not found with ID: " + id));
	}

	private void applyUpdateFields(PasswordPolicyEntity entity, UpdatePasswordPolicyRequest request) {
		applyGeneralSettings(entity, request);
		applyCharacterRules(entity, request);
		applyLifecycleRules(entity, request);
		applyLockoutAndHashRules(entity, request);
	}

	private void applyGeneralSettings(PasswordPolicyEntity entity, UpdatePasswordPolicyRequest request) {
		if (request.name() != null && !request.name().isBlank()) {
			entity.setName(request.name().trim());
		}
		if (request.description() != null) {
			entity.setDescription(request.description().trim());
		}
		if (request.minLength() != null) {
			entity.setMinLength(request.minLength());
		}
		if (request.maxLength() != null) {
			entity.setMaxLength(request.maxLength());
		}
	}

	private void applyCharacterRules(PasswordPolicyEntity entity, UpdatePasswordPolicyRequest request) {
		if (request.requireUppercase() != null) {
			entity.setRequireUppercase(Boolean.TRUE.equals(request.requireUppercase()) ? "Y" : "N");
		}
		if (request.requireLowercase() != null) {
			entity.setRequireLowercase(Boolean.TRUE.equals(request.requireLowercase()) ? "Y" : "N");
		}
		if (request.requireDigit() != null) {
			entity.setRequireDigit(Boolean.TRUE.equals(request.requireDigit()) ? "Y" : "N");
		}
		if (request.requireSpecial() != null) {
			entity.setRequireSpecial(Boolean.TRUE.equals(request.requireSpecial()) ? "Y" : "N");
		}
		if (request.minCharacterClasses() != null) {
			entity.setMinCharacterClasses(request.minCharacterClasses());
		}
	}

	private void applyLifecycleRules(PasswordPolicyEntity entity, UpdatePasswordPolicyRequest request) {
		if (request.checkCommonPasswords() != null) {
			entity.setCheckCommonPasswords(Boolean.TRUE.equals(request.checkCommonPasswords()) ? "Y" : "N");
		}
		if (request.checkUserSimilarity() != null) {
			entity.setCheckUserSimilarity(Boolean.TRUE.equals(request.checkUserSimilarity()) ? "Y" : "N");
		}
		if (request.checkSequential() != null) {
			entity.setCheckSequential(Boolean.TRUE.equals(request.checkSequential()) ? "Y" : "N");
		}
		if (request.expiryDays() != null) {
			entity.setExpiryDays(request.expiryDays());
		}
		if (request.warnBeforeDays() != null) {
			entity.setWarnBeforeDays(request.warnBeforeDays());
		}
		if (request.minAgeHours() != null) {
			entity.setMinAgeHours(request.minAgeHours());
		}
		if (request.historyDepth() != null) {
			entity.setHistoryDepth(request.historyDepth());
		}
	}

	private void applyLockoutAndHashRules(PasswordPolicyEntity entity, UpdatePasswordPolicyRequest request) {
		if (request.maxFailedAttempts() != null) {
			entity.setMaxFailedAttempts(request.maxFailedAttempts());
		}
		if (request.lockoutMinutes() != null) {
			entity.setLockoutMinutes(request.lockoutMinutes());
		}
		if (request.attemptWindowMinutes() != null) {
			entity.setAttemptWindowMinutes(request.attemptWindowMinutes());
		}
		if (request.lockoutsBeforeDisable() != null) {
			entity.setLockoutsBeforeDisable(request.lockoutsBeforeDisable());
		}
		if (request.hashCost() != null) {
			entity.setHashCost(request.hashCost());
		}
	}

	private String resolveActor(HttpServletRequest httpRequest) {
		if (httpRequest != null && jwtUtils != null) {
			try {
				String userId = jwtUtils.getUserId(httpRequest);
				if (userId != null && !userId.isBlank()) {
					return userId;
				}
			} catch (Exception ex) {
				log.debug("Unable to extract user ID from JWT cookies: {}", ex.getMessage());
			}
		}
		return DEFAULT_ACTOR;
	}

	private PasswordPolicyEntity createDefaultFallbackPolicy() {
		return PasswordPolicyEntity.builder()
			.policyCode("DEFAULT_FALLBACK")
			.name("Default Fallback Policy")
			.isActive("Y")
			.minLength(8)
			.maxLength(64)
			.requireUppercase("N")
			.requireLowercase("N")
			.requireDigit("N")
			.requireSpecial("N")
			.minCharacterClasses(0)
			.checkCommonPasswords("Y")
			.checkUserSimilarity("Y")
			.checkSequential("Y")
			.build();
	}
}
