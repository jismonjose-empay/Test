
package com.empay.user_management.serviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.user_management.dto.UserPermissionOverrideHistoryFilter;
import com.empay.user_management.dto.UserPermissionOverrideHistoryPageResponse;
import com.empay.user_management.entities.UserPermissionOverrideHistoryEntity;
import com.empay.user_management.repository.UserPermissionOverrideHistoryRepository;
import com.empay.user_management.service.UserPermissionOverrideHistoryService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPermissionOverrideHistoryServiceImpl implements UserPermissionOverrideHistoryService {

	private static final String ACTION_TAKEN = "actionTaken";

	private final UserPermissionOverrideHistoryRepository historyRepository;

	@Override
	public UserPermissionOverrideHistoryPageResponse getAllHistory(UserPermissionOverrideHistoryFilter filter) {

		int pageNumber = Math.max(filter.page() - 1, 0);
		int size = filter.pageSize() <= 0 ? 20 : Math.min(filter.pageSize(), 100);

		List<String> allowedSortFields = List.of("historyId", "overrideId", "userSerialNo", "permissionCode", "effect",
				ACTION_TAKEN, "changedBy", "changedAt", "expiresAt");

		String sortField = (filter.sortBy() != null && allowedSortFields.contains(filter.sortBy())) ? filter.sortBy() : "changedAt";

		Sort.Direction direction = "ASC".equalsIgnoreCase(filter.sortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;

		Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(direction, sortField));

		Specification<UserPermissionOverrideHistoryEntity> specification = (root, query, cb) -> {

			List<Predicate> predicates = new ArrayList<>();

			if (filter.search() != null && !filter.search().isBlank()) {
				String pattern = "%" + filter.search().trim().toLowerCase(Locale.ROOT) + "%";

				predicates.add(cb.or(cb.like(cb.lower(root.get("permissionCode")), pattern),
						cb.like(cb.lower(root.get("effect")), pattern),
						cb.like(cb.lower(root.get("justification")), pattern),
						cb.like(cb.lower(root.get("changedBy")), pattern),
						cb.like(cb.lower(root.get(ACTION_TAKEN)), pattern)));
			}

			if (filter.actionTaken() != null && !filter.actionTaken().isBlank()) {
				predicates
						.add(cb.equal(cb.upper(root.get(ACTION_TAKEN)), filter.actionTaken().trim().toUpperCase(Locale.ROOT)));
			}

			if (filter.userSerialNo() != null) {
				predicates.add(cb.equal(root.get("userSerialNo"), filter.userSerialNo()));
			}

			if (filter.overrideId() != null) {
				predicates.add(cb.equal(root.get("overrideId"), filter.overrideId()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<UserPermissionOverrideHistoryEntity> resultPage = historyRepository.findAll(specification, pageable);

		return new UserPermissionOverrideHistoryPageResponse(resultPage.getContent(), resultPage.getTotalElements(),
				pageNumber + 1, size, resultPage.getTotalPages(), resultPage.hasNext());
	}

	@Override
	public List<UserPermissionOverrideHistoryEntity> getHistoryByOverrideId(Long overrideId) {

		return historyRepository.findAllByOverrideIdOrderByChangedAtDesc(overrideId);
	}

	@Override
	public List<UserPermissionOverrideHistoryEntity> getHistoryByUserSerialNo(Long userSerialNo) {

		return historyRepository.findAllByUserSerialNoOrderByChangedAtDesc(userSerialNo);
	}
}
