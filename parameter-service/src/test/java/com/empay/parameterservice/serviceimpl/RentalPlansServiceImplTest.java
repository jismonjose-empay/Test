package com.empay.parameterservice.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.empay.parameterservice.entities.RentalPlansEntity;
import com.empay.parameterservice.exceptions.ResourceAlreadyExistsException;
import com.empay.parameterservice.exceptions.ResourceNotFoundException;
import com.empay.parameterservice.exceptions.ServiceException;
import com.empay.parameterservice.repository.RentalPlansRepository;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.RentalPlansVO;

/**
 * Unit tests for RentalPlansServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class RentalPlansServiceImplTest {

	@Mock
	private RentalPlansRepository rentalPlansRepository;

	@Mock
	private JwtUtils jwtUtils;

	@InjectMocks
	private RentalPlansServiceImpl service;

	private RentalPlansEntity sampleEntity;
	private RentalPlansVO sampleVO;
	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		sampleEntity = RentalPlansEntity.builder()
				.serialNumber(101)
				.institutionCode(1)
				.planDescription("Monthly Terminal Standard")
				.rentalAmount(500.00)
				.rentalFrequency('M')
				.aggregatorSrlNo(201)
				.status(RentalPlansServiceImpl.STATUS_ACTIVE)
				.lastUpdatedDate(LocalDateTime.now())
				.updatedUser(5)
				.build();

		sampleVO = RentalPlansVO.builder()
				.serialNumber(101)
				.institutionCode(1)
				.planDescription("Monthly Terminal Standard")
				.rentalAmount(500.00)
				.rentalFrequency('M')
				.aggregatorSrlNo(201)
				.status(RentalPlansServiceImpl.STATUS_ACTIVE)
				.build();

		request = new MockHttpServletRequest();
	}

	@Test
	@DisplayName("findAll returns paged results with mapped value objects")
	void testFindAll_Success() {
		Page<RentalPlansEntity> page = new PageImpl<>(List.of(sampleEntity));
		when(rentalPlansRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

		ResponseEntity<Object> response = service.findAll(1, 10, "serialNumber", "asc", "Standard", 1, 201);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) response.getBody();
		assertEquals(1L, map.get("totalElements"));
		@SuppressWarnings("unchecked")
		List<RentalPlansVO> list = (List<RentalPlansVO>) map.get("voList");
		assertEquals(1, list.size());
		assertEquals("Monthly Terminal Standard", list.getFirst().getPlanDescription());
		assertEquals("Monthly 1", list.getFirst().getFrequencyDescription());
	}

	@Test
	@DisplayName("findAllActive returns filtered active list")
	void testFindAllActive_WithFilters() {
		when(rentalPlansRepository.findByInstitutionCodeAndAggregatorSrlNoAndStatusNot(1, 201, RentalPlansServiceImpl.STATUS_DELETED))
				.thenReturn(List.of(sampleEntity));

		ResponseEntity<Object> response = service.findAllActive(1, 201);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		@SuppressWarnings("unchecked")
		List<RentalPlansVO> list = (List<RentalPlansVO>) response.getBody();
		assertNotNull(list);
		assertEquals(1, list.size());
	}

	@Test
	@DisplayName("findAllActive without filters calls findByStatusNot")
	void testFindAllActive_NoFilters() {
		when(rentalPlansRepository.findByStatusNot(RentalPlansServiceImpl.STATUS_DELETED))
				.thenReturn(List.of(sampleEntity));

		ResponseEntity<Object> response = service.findAllActive(null, null);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		@SuppressWarnings("unchecked")
		List<RentalPlansVO> list = (List<RentalPlansVO>) response.getBody();
		assertNotNull(list);
		assertEquals(1, list.size());
	}

	@Test
	@DisplayName("findById returns mapped entity when active")
	void testFindById_Found() {
		when(rentalPlansRepository.findById(101)).thenReturn(Optional.of(sampleEntity));

		ResponseEntity<Object> response = service.findById(101);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		RentalPlansVO vo = (RentalPlansVO) response.getBody();
		assertNotNull(vo);
		assertEquals(101, vo.getSerialNumber());
		assertEquals("Monthly Terminal Standard", vo.getPlanDescription());
	}

	@Test
	@DisplayName("findById throws ResourceNotFoundException when absent or marked deleted")
	void testFindById_NotFound() {
		when(rentalPlansRepository.findById(999)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.findById(999));

		RentalPlansEntity deletedEntity = RentalPlansEntity.builder()
				.serialNumber(102)
				.status(RentalPlansServiceImpl.STATUS_DELETED)
				.build();
		when(rentalPlansRepository.findById(102)).thenReturn(Optional.of(deletedEntity));

		assertThrows(ResourceNotFoundException.class, () -> service.findById(102));
	}

	@Test
	@DisplayName("saveRentalPlan persists entity successfully")
	void testSaveRentalPlan_Success() {
		sampleVO.setSerialNumber(null);
		when(jwtUtils.getUserSerNumber(request)).thenReturn(7);
		when(rentalPlansRepository.saveAndFlush(any(RentalPlansEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		String result = service.saveRentalPlan(sampleVO, request);

		assertEquals("Saved Successfully", result);
		verify(rentalPlansRepository).saveAndFlush(any(RentalPlansEntity.class));
	}

	@Test
	@DisplayName("saveRentalPlan throws ResourceAlreadyExistsException when serialNumber exists")
	void testSaveRentalPlan_Duplicate() {
		when(rentalPlansRepository.existsById(101)).thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class, () -> service.saveRentalPlan(sampleVO, request));
	}

	@Test
	@DisplayName("saveRentalPlan wraps generic exception into ServiceException")
	void testSaveRentalPlan_GenericError() {
		sampleVO.setSerialNumber(null);
		when(rentalPlansRepository.saveAndFlush(any(RentalPlansEntity.class)))
				.thenThrow(new RuntimeException("DB Connection down"));

		assertThrows(ServiceException.class, () -> service.saveRentalPlan(sampleVO, request));
	}

	@Test
	@DisplayName("updateRentalPlan modifies existing entity")
	void testUpdateRentalPlan_Success() {
		when(rentalPlansRepository.findById(101)).thenReturn(Optional.of(sampleEntity));
		when(jwtUtils.getUserSerNumber(request)).thenReturn(8);

		sampleVO.setPlanDescription("Updated Plan Description");
		sampleVO.setRentalAmount(750.00);

		String result = service.updateRentalPlan(sampleVO, 101, request);

		assertEquals("Updated Successfully", result);
		assertEquals("Updated Plan Description", sampleEntity.getPlanDescription());
		assertEquals(750.00, sampleEntity.getRentalAmount());
		verify(rentalPlansRepository).saveAndFlush(sampleEntity);
	}

	@Test
	@DisplayName("updateRentalPlan throws IllegalArgumentException on id mismatch")
	void testUpdateRentalPlan_IdMismatch() {
		sampleVO.setSerialNumber(200);

		assertThrows(IllegalArgumentException.class, () -> service.updateRentalPlan(sampleVO, 101, request));
	}

	@Test
	@DisplayName("deleteRentalPlan marks entity with soft-delete status")
	void testDeleteRentalPlan_Success() {
		when(rentalPlansRepository.findById(101)).thenReturn(Optional.of(sampleEntity));
		when(jwtUtils.getUserSerNumber(request)).thenReturn(9);

		String result = service.deleteRentalPlan(101, request);

		assertEquals("Deleted Successfully", result);
		assertEquals(RentalPlansServiceImpl.STATUS_DELETED, sampleEntity.getStatus());
		assertEquals(9, sampleEntity.getUpdatedUser());
		verify(rentalPlansRepository).saveAndFlush(sampleEntity);
	}

	@Test
	@DisplayName("resolveFrequencyDescription correctly maps all valid frequencies")
	void testResolveFrequencyDescription() {
		assertEquals("Daily", RentalPlansServiceImpl.resolveFrequencyDescription('D'));
		assertEquals("Monthly 1", RentalPlansServiceImpl.resolveFrequencyDescription('M'));
		assertEquals("Monthly 2", RentalPlansServiceImpl.resolveFrequencyDescription('P'));
		assertEquals("Quarterly", RentalPlansServiceImpl.resolveFrequencyDescription('Q'));
		assertEquals("Half Yearly", RentalPlansServiceImpl.resolveFrequencyDescription('H'));
		assertEquals("Yearly", RentalPlansServiceImpl.resolveFrequencyDescription('Y'));
		assertEquals("Life Time", RentalPlansServiceImpl.resolveFrequencyDescription('L'));
		assertEquals("", RentalPlansServiceImpl.resolveFrequencyDescription(null));
		assertEquals("X", RentalPlansServiceImpl.resolveFrequencyDescription('X'));
	}

	@Test
	@DisplayName("mapEntityToVO and mapVOToEntity null checks")
	void testNullMappings() {
		assertNull(service.mapEntityToVO(null));
		assertNull(service.mapVOToEntity(null));
	}
}
