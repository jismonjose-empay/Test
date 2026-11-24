package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.RentalPlansVO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for rental plan operations and business logic.
 */
public interface RentalPlansService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search,
			Integer institutionCode, Integer aggregatorSrlNo);

	ResponseEntity<Object> findAllActive(Integer institutionCode, Integer aggregatorSrlNo);

	ResponseEntity<Object> findById(Integer id);

	String saveRentalPlan(RentalPlansVO rentalPlansVO, HttpServletRequest request);

	String updateRentalPlan(RentalPlansVO rentalPlansVO, Integer id, HttpServletRequest request);

	String deleteRentalPlan(Integer id, HttpServletRequest request);
}
