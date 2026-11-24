package com.empay.user_management.serviceimpl;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.empay.user_management.dto.CreateRoleConflictRequest;
import com.empay.user_management.dto.RoleConflictResponse;
import com.empay.user_management.entities.RoleConflictEntity;
import com.empay.user_management.repository.AppRoleRepository;
import com.empay.user_management.repository.RoleConflictRepository;
import com.empay.user_management.repository.UserRoleRepositoryV2;
import com.empay.user_management.service.RoleConflictService;

/**
 * Implementation of the role conflict rule service. Manages segregation of
 * duties rules between application roles.
 */
@Service
@Transactional
public class RoleConflictServiceImpl implements RoleConflictService {

	private static final String SEVERITY_BLOCK = "BLOCK";
	private static final String SEVERITY_WARN = "WARN";

	private final RoleConflictRepository roleConflictRepository;
	private final AppRoleRepository appRoleRepository;
	private final UserRoleRepositoryV2 userRoleRepository;

	/**
	 * Creates a new role conflict service implementation with required
	 * dependencies.
	 *
	 * @param roleConflictRepository repository for role conflict entities
	 * @param appRoleRepository      repository for application roles
	 * @param userRoleRepository     repository for user role mappings
	 */
	public RoleConflictServiceImpl(RoleConflictRepository roleConflictRepository, AppRoleRepository appRoleRepository,
			UserRoleRepositoryV2 userRoleRepository) {
		this.roleConflictRepository = roleConflictRepository;
		this.appRoleRepository = appRoleRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoleConflictResponse> getAllRules() {
		return roleConflictRepository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public RoleConflictResponse getRuleById(Long id) {

		if (id == null || id <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role conflict ID must be a positive number");
		}

		RoleConflictEntity entity = roleConflictRepository.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role conflict rule not found: " + id));

		return toResponse(entity);
	}

	@Override
	public void deleteRule(Long id) {

		if (id == null || id <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role conflict ID must be a positive number");
		}

		RoleConflictEntity entity = roleConflictRepository.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role conflict rule not found: " + id));

		try {
			roleConflictRepository.delete(entity);
			roleConflictRepository.flush();

		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Cannot delete this role conflict rule because it is referenced by other records.", ex);
		}
	}

	@Override
	public RoleConflictResponse createRule(CreateRoleConflictRequest request) {

		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
		}

		if (request.getPair() == null || request.getPair().size() != 2) {

			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pair must contain exactly two role codes");
		}

		String roleA = normalize(request.getPair().get(0));
		String roleB = normalize(request.getPair().get(1));

		if (roleA.equals(roleB)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A role cannot conflict with itself");
		}

		String severity = normalizeSeverity(request.getSeverity());
		String rationale = normalizeRationale(request.getRationale());

		if (!appRoleRepository.existsById(roleA)) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Role does not exist: " + roleA);
		}

		if (!appRoleRepository.existsById(roleB)) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Role does not exist: " + roleB);
		}

		// Enforce the database ordering constraint.
		String first;
		String second;

		if (roleA.compareTo(roleB) < 0) {
			first = roleA;
			second = roleB;
		} else {
			first = roleB;
			second = roleA;
		}

		// Check for an existing pair, regardless of request order.
		if (roleConflictRepository.existsByRoleCodeAAndRoleCodeB(first, second)) {

			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Role conflict already exists for " + first + " and " + second);
		}

		if (userRoleRepository.existsUserWithBothRoles(first, second)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Cannot create role conflict because one or more users are already assigned both roles: " + first
							+ " and " + second);
		}

		RoleConflictEntity entity = RoleConflictEntity.builder().roleCodeA(first).roleCodeB(second).severity(severity)
				.rationale(rationale).build();

		try {
			RoleConflictEntity saved = roleConflictRepository.saveAndFlush(entity);

			return toResponse(saved);

		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Role conflict could not be saved because " + "it violates a database constraint", ex);
		}
	}

	private String normalize(String roleCode) {

		if (roleCode == null || roleCode.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role codes cannot be blank");
		}

		String normalized = roleCode.trim().toUpperCase(Locale.ROOT);

		if (normalized.length() > 40) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role code cannot exceed 40 characters");
		}

		return normalized;
	}

	private String normalizeSeverity(String severity) {

		if (severity == null || severity.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Severity is required");
		}

		String normalized = severity.trim().toUpperCase(Locale.ROOT);

		if (!SEVERITY_BLOCK.equals(normalized) && !SEVERITY_WARN.equals(normalized)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Severity must be BLOCK or WARN");
		}

		return normalized;
	}

	private String normalizeRationale(String rationale) {

		if (rationale == null || rationale.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Rationale must contain between 1 and 400 characters");
		}

		String normalized = rationale.trim();

		if (normalized.length() > 400) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Rationale must contain between 1 and 400 characters");
		}

		return normalized;
	}

	private RoleConflictResponse toResponse(RoleConflictEntity entity) {

		return new RoleConflictResponse("rcr-" + entity.getConflictId(),
				List.of(entity.getRoleCodeA(), entity.getRoleCodeB()), entity.getSeverity(), entity.getRationale());
	}
}

