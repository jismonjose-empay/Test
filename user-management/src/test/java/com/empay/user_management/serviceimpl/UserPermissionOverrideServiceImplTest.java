package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.empay.user_management.dto.UserPermissionOverridePageResponse;
import com.empay.user_management.dto.UserPermissionOverrideRequest;
import com.empay.user_management.dto.UserPermissionOverrideResponse;
import com.empay.user_management.entities.UserPermissionOverrideEntity;
import com.empay.user_management.exceptions.ResourceAlreadyExistsException;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.UserPermissionOverrideHistoryRepository;
import com.empay.user_management.repository.UserPermissionOverrideRepository;
import com.empay.user_management.repository.UsersRepositoryV2;

@ExtendWith(MockitoExtension.class)
class UserPermissionOverrideServiceImplTest {

	@Mock
	private UserPermissionOverrideRepository overrideRepository;

	@Mock
	private UsersRepositoryV2 usersRepositoryV2;

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private UserPermissionOverrideHistoryRepository historyRepository;

	@InjectMocks
	private UserPermissionOverrideServiceImpl service;

	private UserPermissionOverrideRequest buildRequest(Long userSerialNo, String code, String effect, String just, String grantedBy, LocalDateTime expiresAt) {
		UserPermissionOverrideRequest req = new UserPermissionOverrideRequest();
		req.setUserSerialNo(userSerialNo);
		req.setPermissionCode(code);
		req.setEffect(effect);
		req.setJustification(just);
		req.setGrantedBy(grantedBy);
		req.setExpiresAt(expiresAt);
		return req;
	}

	@Test
	@DisplayName("createOverride successfully creates and saves an override")
	void testCreateOverride_Success() {
		UserPermissionOverrideRequest request = buildRequest(
				101L, "ADMIN.USER.VIEW", "GRANT", "Temporary access for audit",
				"admin_user", LocalDateTime.now(ZoneOffset.UTC).plusDays(5));

		when(usersRepositoryV2.existsById(101)).thenReturn(true);
		when(permissionRepository.existsById("ADMIN.USER.VIEW")).thenReturn(true);
		when(overrideRepository.existsByUserSerialNoAndPermissionCode(101L, "ADMIN.USER.VIEW")).thenReturn(false);

		UserPermissionOverrideEntity saved = new UserPermissionOverrideEntity();
		saved.setOverrideId(1L);
		saved.setUserSerialNo(101L);
		saved.setPermissionCode("ADMIN.USER.VIEW");
		saved.setEffect("GRANT");
		saved.setJustification("Temporary access for audit");
		saved.setGrantedBy("admin_user");
		saved.setGrantedAt(LocalDateTime.now(ZoneOffset.UTC));
		saved.setExpiresAt(request.getExpiresAt());

		when(overrideRepository.saveAndFlush(any())).thenReturn(saved);

		UserPermissionOverrideResponse response = service.createOverride(request);

		assertNotNull(response);
		assertEquals(1L, response.getOverrideId());
		assertEquals("GRANT", response.getEffect());
		verify(historyRepository).save(any());
	}

	@Test
	@DisplayName("createOverride validates null request and invalid parameters")
	void testCreateOverride_Validations() {
		assertThrows(IllegalArgumentException.class, () -> service.createOverride(null));

		UserPermissionOverrideRequest invalidSerial = buildRequest(0L, "CODE", "GRANT", "Justification", "admin", null);
		assertThrows(IllegalArgumentException.class, () -> service.createOverride(invalidSerial));

		UserPermissionOverrideRequest emptyJust = buildRequest(1L, "CODE", "GRANT", "   ", "admin", null);
		assertThrows(IllegalArgumentException.class, () -> service.createOverride(emptyJust));

		UserPermissionOverrideRequest tooLongJust = buildRequest(1L, "CODE", "GRANT", "A".repeat(401), "admin", null);
		assertThrows(IllegalArgumentException.class, () -> service.createOverride(tooLongJust));

		UserPermissionOverrideRequest invalidGrantedBy = buildRequest(1L, "CODE", "GRANT", "Valid Justification", "", null);
		assertThrows(IllegalArgumentException.class, () -> service.createOverride(invalidGrantedBy));

		UserPermissionOverrideRequest pastExpiry = buildRequest(1L, "CODE", "GRANT", "Valid Justification", "admin", LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
		assertThrows(IllegalArgumentException.class, () -> service.createOverride(pastExpiry));

		UserPermissionOverrideRequest invalidEffect = buildRequest(1L, "CODE", "INVALID", "Valid Justification", "admin", null);
		assertThrows(IllegalArgumentException.class, () -> service.createOverride(invalidEffect));
	}

	@Test
	@DisplayName("createOverride throws ResourceNotFoundException when user or permission does not exist")
	void testCreateOverride_NotFoundScenarios() {
		UserPermissionOverrideRequest request = buildRequest(999L, "ADMIN.VIEW", "GRANT", "Reason", "admin", null);

		when(usersRepositoryV2.existsById(999)).thenReturn(false);
		assertThrows(ResourceNotFoundException.class, () -> service.createOverride(request));

		when(usersRepositoryV2.existsById(999)).thenReturn(true);
		when(permissionRepository.existsById("ADMIN.VIEW")).thenReturn(false);
		assertThrows(ResourceNotFoundException.class, () -> service.createOverride(request));
	}

	@Test
	@DisplayName("createOverride throws ResourceAlreadyExistsException on duplicate or constraint violation")
	void testCreateOverride_AlreadyExists() {
		UserPermissionOverrideRequest request = buildRequest(101L, "ADMIN.VIEW", "GRANT", "Reason", "admin", null);

		when(usersRepositoryV2.existsById(101)).thenReturn(true);
		when(permissionRepository.existsById("ADMIN.VIEW")).thenReturn(true);
		when(overrideRepository.existsByUserSerialNoAndPermissionCode(101L, "ADMIN.VIEW")).thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class, () -> service.createOverride(request));

		when(overrideRepository.existsByUserSerialNoAndPermissionCode(101L, "ADMIN.VIEW")).thenReturn(false);
		when(overrideRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
		assertThrows(ResourceAlreadyExistsException.class, () -> service.createOverride(request));
	}

	@Test
	@DisplayName("getOverrideById returns override when found and throws when not found")
	void testGetOverrideById() {
		assertThrows(IllegalArgumentException.class, () -> service.getOverrideById(null));
		assertThrows(IllegalArgumentException.class, () -> service.getOverrideById(-1L));

		when(overrideRepository.findById(1L)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.getOverrideById(1L));

		UserPermissionOverrideEntity entity = new UserPermissionOverrideEntity();
		entity.setOverrideId(1L);
		entity.setUserSerialNo(101L);
		entity.setPermissionCode("VIEW");
		entity.setEffect("GRANT");
		when(overrideRepository.findById(1L)).thenReturn(Optional.of(entity));

		UserPermissionOverrideResponse response = service.getOverrideById(1L);
		assertNotNull(response);
		assertEquals(1L, response.getOverrideId());
	}

	@Test
	@DisplayName("deleteOverride deletes override and records history")
	void testDeleteOverride() {
		assertThrows(IllegalArgumentException.class, () -> service.deleteOverride(null, "admin"));
		assertThrows(IllegalArgumentException.class, () -> service.deleteOverride(1L, "  "));

		when(overrideRepository.findById(1L)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.deleteOverride(1L, "admin"));

		UserPermissionOverrideEntity entity = new UserPermissionOverrideEntity();
		entity.setOverrideId(1L);
		entity.setUserSerialNo(101L);
		when(overrideRepository.findById(1L)).thenReturn(Optional.of(entity));

		service.deleteOverride(1L, "admin");
		verify(overrideRepository).delete(entity);
		verify(historyRepository).save(any());
	}

	@Test
	@DisplayName("getAllOverrides returns paged list with filter, sort, and exercises specification")
	@SuppressWarnings("unchecked")
	void testGetAllOverrides() {
		UserPermissionOverrideEntity entity = new UserPermissionOverrideEntity();
		entity.setOverrideId(1L);
		entity.setUserSerialNo(101L);
		entity.setPermissionCode("VIEW");
		entity.setEffect("GRANT");

		org.mockito.ArgumentCaptor<Specification<UserPermissionOverrideEntity>> captor = org.mockito.ArgumentCaptor.forClass(Specification.class);
		when(overrideRepository.findAll(captor.capture(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entity)));

		UserPermissionOverridePageResponse page = service.getAllOverrides("VIEW", "permissionCode", "ASC", 1, 10);
		assertNotNull(page);
		assertEquals(1, page.items().size());
		assertEquals(1L, page.total());

		// Execute specification lambda
		Specification<UserPermissionOverrideEntity> spec = captor.getValue();
		jakarta.persistence.criteria.Root<UserPermissionOverrideEntity> root = mock(jakarta.persistence.criteria.Root.class);
		jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
		jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
		jakarta.persistence.criteria.Path path = mock(jakarta.persistence.criteria.Path.class);
		jakarta.persistence.criteria.Predicate pred = mock(jakarta.persistence.criteria.Predicate.class);

		when(root.get(any(String.class))).thenReturn(path);
		when(cb.lower(any())).thenReturn(path);
		when(cb.like(any(), any(String.class))).thenReturn(pred);
		when(cb.or(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(pred);
		when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(pred);

		assertNotNull(spec.toPredicate(root, query, cb));

		// Test default sort & pagination
		UserPermissionOverridePageResponse defaultPage = service.getAllOverrides(null, null, null, 0, -5);
		assertNotNull(defaultPage);
		assertEquals(20, defaultPage.pageSize());
	}
}
