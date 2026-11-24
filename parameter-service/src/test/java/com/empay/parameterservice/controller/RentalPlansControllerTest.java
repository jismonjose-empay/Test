package com.empay.parameterservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.empay.parameterservice.service.RentalPlansService;
import com.empay.parameterservice.valueobjects.RentalPlansVO;

/**
 * Unit tests for RentalPlansController.
 */
@ExtendWith(MockitoExtension.class)
class RentalPlansControllerTest {

	@Mock
	private RentalPlansService rentalPlansService;

	@InjectMocks
	private RentalPlansController controller;

	private RentalPlansVO sampleVO;
	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		sampleVO = RentalPlansVO.builder()
				.serialNumber(101)
				.institutionCode(1)
				.planDescription("Monthly Terminal Standard")
				.rentalAmount(500.00)
				.rentalFrequency('M')
				.aggregatorSrlNo(201)
				.status(1)
				.build();

		request = new MockHttpServletRequest();
	}

	@Test
	@DisplayName("getRentalPlans delegates to service and returns 200 OK")
	void testGetRentalPlans() {
		when(rentalPlansService.findAll(1, 10, "serialNumber", "asc", "Standard", 1, 201))
				.thenReturn(new ResponseEntity<>(Map.of("voList", List.of(sampleVO), "totalElements", 1L), HttpStatus.OK));

		ResponseEntity<Object> response = controller.getRentalPlans(1, 10, "serialNumber", "asc", "Standard", 1, 201);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(rentalPlansService).findAll(1, 10, "serialNumber", "asc", "Standard", 1, 201);
	}

	@Test
	@DisplayName("getAllRentalPlans delegates to service and returns 200 OK")
	void testGetAllRentalPlans() {
		when(rentalPlansService.findAllActive(1, 201))
				.thenReturn(new ResponseEntity<>(List.of(sampleVO), HttpStatus.OK));

		ResponseEntity<Object> response = controller.getAllRentalPlans(1, 201);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(rentalPlansService).findAllActive(1, 201);
	}

	@Test
	@DisplayName("findRentalPlan delegates to service and returns 200 OK")
	void testFindRentalPlan() {
		when(rentalPlansService.findById(101))
				.thenReturn(new ResponseEntity<>(sampleVO, HttpStatus.OK));

		ResponseEntity<Object> response = controller.findRentalPlan(101);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(sampleVO, response.getBody());
		verify(rentalPlansService).findById(101);
	}

	@Test
	@DisplayName("saveRentalPlan returns 201 CREATED with success message")
	void testSaveRentalPlan() {
		when(rentalPlansService.saveRentalPlan(eq(sampleVO), any()))
				.thenReturn("Saved Successfully");

		ResponseEntity<String> response = controller.saveRentalPlan(sampleVO, request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals("Saved Successfully", response.getBody());
		verify(rentalPlansService).saveRentalPlan(sampleVO, request);
	}

	@Test
	@DisplayName("updateRentalPlan returns 200 OK with success message")
	void testUpdateRentalPlan() {
		when(rentalPlansService.updateRentalPlan(eq(sampleVO), eq(101), any()))
				.thenReturn("Updated Successfully");

		ResponseEntity<String> response = controller.updateRentalPlan(101, sampleVO, request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Updated Successfully", response.getBody());
		verify(rentalPlansService).updateRentalPlan(sampleVO, 101, request);
	}

	@Test
	@DisplayName("deleteRentalPlan returns 200 OK with success message")
	void testDeleteRentalPlan() {
		when(rentalPlansService.deleteRentalPlan(eq(101), any()))
				.thenReturn("Deleted Successfully");

		ResponseEntity<String> response = controller.deleteRentalPlan(101, request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Deleted Successfully", response.getBody());
		verify(rentalPlansService).deleteRentalPlan(101, request);
	}
}
