
package com.empay.user_management.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.user_management.dto.UserPermissionOverridePageResponse;
import com.empay.user_management.dto.UserPermissionOverrideRequest;
import com.empay.user_management.dto.UserPermissionOverrideResponse;
import com.empay.user_management.entities.UserPermissionOverrideEntity;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.UserPermissionOverrideRepository;
import com.empay.user_management.service.UserPermissionOverrideService;

import jakarta.persistence.criteria.Predicate;

import java.time.ZoneOffset;

import com.empay.user_management.entities.UserPermissionOverrideHistoryEntity;
import com.empay.user_management.repository.UserPermissionOverrideHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserPermissionOverrideServiceImpl implements UserPermissionOverrideService {

	private static final String OVERRIDE_ID = "overrideId";

	private final UserPermissionOverrideRepository overrideRepository;
	private final UsersRepositoryV2 usersRepositoryV2;
	private final PermissionRepository permissionRepository;
	private final UserPermissionOverrideHistoryRepository historyRepository;

	@Override
	public UserPermissionOverrideResponse createOverride(UserPermissionOverrideRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("Request body is required");
		}

		Long userSerialNo = request.getUserSerialNo();

		if (userSerialNo == null || userSerialNo <= 0) {
			throw new IllegalArgumentException("A valid user serial number is required");
		}

		String permissionCode = normalizePermissionCode(request.getPermissionCode());

		String effect = normalizeEffect(request.getEffect());

		String justification = request.getJustification().trim();

		String grantedBy = request.getGrantedBy().trim();

		LocalDateTime expiresAt = request.getExpiresAt();

		if (justification.isEmpty()) {
			throw new IllegalArgumentException("Justification is required");
		}

		if (justification.length() > 400) {
			throw new IllegalArgumentException("Justification cannot exceed 400 characters");
		}

		if (grantedBy.isEmpty() || grantedBy.length() > 50) {
			throw new IllegalArgumentException("Granted by must contain 1 to 50 characters");
		}

		if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
			throw new IllegalArgumentException("Expiry date must be in the future");
		}

		if (userSerialNo > Integer.MAX_VALUE || !usersRepositoryV2.existsById(userSerialNo.intValue())) {

			throw new ResourceNotFoundException("User", "userSerialNo",
					"User not found with serial number: " + userSerialNo);
		}

		if (!permissionRepository.existsById(permissionCode)) {
			throw new ResourceNotFoundException("Permission", "permissionCode",
					"Permission not found: " + permissionCode);
		}

		if (overrideRepository.existsByUserSerialNoAndPermissionCode(userSerialNo, permissionCode)) {
			throw new ResourceAlreadyExistsException("UserPermissionOverride", "permissionCode",
					"An override already exists for this user and permission");
		}

		UserPermissionOverrideEntity entity = new UserPermissionOverrideEntity();

		entity.setUserSerialNo(userSerialNo);
		entity.setPermissionCode(permissionCode);
		entity.setEffect(effect);
		entity.setJustification(justification);
		entity.setGrantedBy(grantedBy);
		entity.setExpiresAt(expiresAt);

		try {
			UserPermissionOverrideEntity saved = overrideRepository.saveAndFlush(entity);
			saveHistory(saved, "CREATED", request.getGrantedBy());

			return toResponse(saved);

		} catch (DataIntegrityViolationException ex) {
			throw new ResourceAlreadyExistsException("UserPermissionOverride", "permissionCode",
					"Unable to create the override. Check for an existing "
							+ "user-permission override or a database constraint violation.");
		}

	}

	@Override
	public void deleteOverride(Long overrideId, String changedBy) {

		if (overrideId == null || overrideId <= 0) {
			throw new IllegalArgumentException("Override ID must be a positive number");
		}

		if (changedBy == null || changedBy.isBlank()) {
			throw new IllegalArgumentException("Changed by is required");
		}

		UserPermissionOverrideEntity entity = overrideRepository.findById(overrideId)
				.orElseThrow(() -> new ResourceNotFoundException("UserPermissionOverride", OVERRIDE_ID,
						"Permission override not found with ID: " + overrideId));

		saveHistory(entity, "DELETED", changedBy.trim());

		overrideRepository.delete(entity);
		overrideRepository.flush();
	}

	@Override
	@Transactional(readOnly = true)
	public UserPermissionOverridePageResponse getAllOverrides(
	        String search,
	        String sortBy,
	        String sortDir,
	        int page,
	        int pageSize) {

	    return queryOverrides(search, sortBy, sortDir, page, pageSize);
	}
	private UserPermissionOverridePageResponse queryOverrides(String search, String sortBy, String sortDir, int page,
			int pageSize) {

		int pageNumber = Math.max(page - 1, 0);
		int size = (pageSize <= 0) ? 20 : Math.min(pageSize, 100);

		List<String> allowedSortFields = List.of(OVERRIDE_ID, "userSerialNo", "permissionCode", "effect", "grantedBy",
				"grantedAt", "expiresAt");

		String sortField = (sortBy != null && allowedSortFields.contains(sortBy)) ? sortBy : "grantedAt";

		Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

		Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(direction, sortField));

		Specification<UserPermissionOverrideEntity> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

				predicates.add(cb.or(cb.like(cb.lower(root.get("permissionCode")), pattern),
						cb.like(cb.lower(root.get("effect")), pattern),
						cb.like(cb.lower(root.get("justification")), pattern),
						cb.like(cb.lower(root.get("grantedBy")), pattern)));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<UserPermissionOverrideEntity> resultPage = overrideRepository.findAll(spec, pageable);

		List<UserPermissionOverrideResponse> items = resultPage.getContent().stream().map(this::toResponse).toList();

		return new UserPermissionOverridePageResponse(items, resultPage.getTotalElements(), pageNumber + 1, size,
				resultPage.getTotalPages(), resultPage.hasNext());
	}

	@Override
	@Transactional(readOnly = true)
	public UserPermissionOverrideResponse getOverrideById(Long id) {

		if (id == null || id <= 0) {
			throw new IllegalArgumentException("A valid override ID is required");
		}

		UserPermissionOverrideEntity entity = overrideRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("UserPermissionOverride", OVERRIDE_ID,
						"Override not found with ID: " + id));

		return toResponse(entity);
	}

	private String normalizePermissionCode(String permissionCode) {

		if (permissionCode == null || permissionCode.isBlank()) {
			throw new IllegalArgumentException("Permission code is required");
		}

		String normalized = permissionCode.trim().toUpperCase(Locale.ROOT);

		if (normalized.length() > 60) {
			throw new IllegalArgumentException("Permission code cannot exceed 60 characters");
		}

		return normalized;
	}

	private String normalizeEffect(String effect) {

		if (effect == null || effect.isBlank()) {
			throw new IllegalArgumentException("Effect is required");
		}

		String normalized = effect.trim().toUpperCase(Locale.ROOT);

		if (!normalized.equals("GRANT") && !normalized.equals("DENY")) {
			throw new IllegalArgumentException("Effect must be either GRANT or DENY");
		}

		return normalized;
	}

	private UserPermissionOverrideResponse toResponse(UserPermissionOverrideEntity entity) {

		return new UserPermissionOverrideResponse(entity.getOverrideId(), entity.getUserSerialNo(),
				entity.getPermissionCode(), entity.getEffect(), entity.getJustification(), entity.getGrantedBy(),
				entity.getGrantedAt(), entity.getExpiresAt());
	}

	private void saveHistory(UserPermissionOverrideEntity override, String actionTaken, String changedBy) {

		UserPermissionOverrideHistoryEntity history = UserPermissionOverrideHistoryEntity.builder()
				.overrideId(override.getOverrideId()).userSerialNo(override.getUserSerialNo())
				.permissionCode(override.getPermissionCode()).effect(override.getEffect())
				.justification(override.getJustification()).actionTaken(actionTaken).changedBy(changedBy)
				.changedAt(LocalDateTime.now(ZoneOffset.UTC)).expiresAt(override.getExpiresAt()).build();

		historyRepository.save(history);
	}
}
