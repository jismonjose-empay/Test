package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.empay.user_management.dto.UserPermissionOverrideHistoryFilter;
import com.empay.user_management.dto.UserPermissionOverrideHistoryPageResponse;
import com.empay.user_management.entities.UserPermissionOverrideHistoryEntity;
import com.empay.user_management.repository.UserPermissionOverrideHistoryRepository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class UserPermissionOverrideHistoryServiceImplTest {

	@Mock
	private UserPermissionOverrideHistoryRepository historyRepository;

	@InjectMocks
	private UserPermissionOverrideHistoryServiceImpl service;

	@Test
	@DisplayName("getAllHistory returns paginated history and exercises specification predicates")
	@SuppressWarnings("unchecked")
	void testGetAllHistory_WithAllFilters() {
		UserPermissionOverrideHistoryEntity entity = UserPermissionOverrideHistoryEntity.builder()
				.historyId(1L)
				.overrideId(10L)
				.userSerialNo(101L)
				.permissionCode("ADMIN.USER.VIEW")
				.effect("GRANT")
				.justification("Audit access")
				.actionTaken("CREATED")
				.changedBy("admin")
				.changedAt(LocalDateTime.now(ZoneOffset.UTC))
				.expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(5))
				.build();

		ArgumentCaptor<Specification<UserPermissionOverrideHistoryEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
		when(historyRepository.findAll(specCaptor.capture(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entity)));

		UserPermissionOverrideHistoryFilter filter = new UserPermissionOverrideHistoryFilter(
				"audit", "CREATED", 101L, 10L, "changedAt", "DESC", 1, 20);

		UserPermissionOverrideHistoryPageResponse response = service.getAllHistory(filter);

		assertNotNull(response);
		assertEquals(1, response.items().size());
		assertEquals(1L, response.total());
		assertEquals("CREATED", response.items().get(0).getActionTaken());

		// Execute specification lambda to test all predicate branches
		Specification<UserPermissionOverrideHistoryEntity> capturedSpec = specCaptor.getValue();
		Root<UserPermissionOverrideHistoryEntity> root = mock(Root.class, withSettings().lenient());
		CriteriaQuery<?> query = mock(CriteriaQuery.class, withSettings().lenient());
		CriteriaBuilder cb = mock(CriteriaBuilder.class, withSettings().lenient());
		Path path = mock(Path.class, withSettings().lenient());
		Predicate pred = mock(Predicate.class, withSettings().lenient());

		when(root.get(any(String.class))).thenReturn(path);
		when(cb.lower(any())).thenReturn(path);
		when(cb.upper(any())).thenReturn(path);
		when(cb.like(any(), any(String.class))).thenReturn(pred);
		doReturn(pred).when(cb).equal(any(), any());
		when(cb.or(any(Predicate[].class))).thenReturn(pred);
		when(cb.and(any(Predicate[].class))).thenReturn(pred);

		Predicate resultPredicate = capturedSpec.toPredicate(root, query, cb);
		assertNotNull(resultPredicate);
	}

	@Test
	@DisplayName("getAllHistory applies default sorting and pagination when input fields are empty")
	@SuppressWarnings("unchecked")
	void testGetAllHistory_Defaults() {
		when(historyRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));

		UserPermissionOverrideHistoryFilter filter = new UserPermissionOverrideHistoryFilter(
				null, null, null, null, null, "ASC", 0, -5);

		UserPermissionOverrideHistoryPageResponse response = service.getAllHistory(filter);

		assertNotNull(response);
		assertEquals(0, response.items().size());
		assertEquals(20, response.pageSize());
	}

	@Test
	@DisplayName("getHistoryByOverrideId returns history by override id")
	void testGetHistoryByOverrideId() {
		UserPermissionOverrideHistoryEntity entity = new UserPermissionOverrideHistoryEntity();
		entity.setHistoryId(5L);
		entity.setOverrideId(10L);

		when(historyRepository.findAllByOverrideIdOrderByChangedAtDesc(10L)).thenReturn(List.of(entity));

		List<UserPermissionOverrideHistoryEntity> list = service.getHistoryByOverrideId(10L);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(10L, list.get(0).getOverrideId());
	}

	@Test
	@DisplayName("getHistoryByUserSerialNo returns history by user serial number")
	void testGetHistoryByUserSerialNo() {
		UserPermissionOverrideHistoryEntity entity = new UserPermissionOverrideHistoryEntity();
		entity.setHistoryId(6L);
		entity.setUserSerialNo(101L);

		when(historyRepository.findAllByUserSerialNoOrderByChangedAtDesc(101L)).thenReturn(List.of(entity));

		List<UserPermissionOverrideHistoryEntity> list = service.getHistoryByUserSerialNo(101L);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(101L, list.get(0).getUserSerialNo());
	}
}
