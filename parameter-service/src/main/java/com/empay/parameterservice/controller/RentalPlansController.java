package com.empay.parameterservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empay.parameterservice.service.RentalPlansService;
import com.empay.parameterservice.valueobjects.RentalPlansVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for rental plan management operations.
 */
@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class RentalPlansController {

	private final RentalPlansService rentalPlansService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RENTAL_PLAN.VIEW')")
	@GetMapping(value = { "/get-rentalplans", "/get-rentalplan" })
	public ResponseEntity<Object> getRentalPlans(
			@RequestParam(required = false, defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Integer institutionCode,
			@RequestParam(required = false) Integer aggregatorSrlNo) {
		return rentalPlansService.findAll(page, size, sortBy, sortDir, search, institutionCode, aggregatorSrlNo);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RENTAL_PLAN.VIEW')")
	@GetMapping(value = "/get-all-rentalplans")
	public ResponseEntity<Object> getAllRentalPlans(
			@RequestParam(required = false) Integer institutionCode,
			@RequestParam(required = false) Integer aggregatorSrlNo) {
		return rentalPlansService.findAllActive(institutionCode, aggregatorSrlNo);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RENTAL_PLAN.VIEW')")
	@GetMapping(value = "/find-rentalplan/{id}")
	public ResponseEntity<Object> findRentalPlan(@PathVariable Integer id) {
		return rentalPlansService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RENTAL_PLAN.CREATE')")
	@PostMapping(value = "/save-rentalplan")
	public ResponseEntity<String> saveRentalPlan(
			@RequestBody @Valid RentalPlansVO rentalPlansVO,
			HttpServletRequest request) {
		String message = rentalPlansService.saveRentalPlan(rentalPlansVO, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RENTAL_PLAN.EDIT')")
	@PutMapping(value = "/update-rentalplan/{id}")
	public ResponseEntity<String> updateRentalPlan(
			@PathVariable Integer id,
			@RequestBody @Valid RentalPlansVO rentalPlansVO,
			HttpServletRequest request) {
		String message = rentalPlansService.updateRentalPlan(rentalPlansVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RENTAL_PLAN.DELETE')")
	@DeleteMapping(value = "/delete-rentalplan/{id}")
	public ResponseEntity<String> deleteRentalPlan(
			@PathVariable Integer id,
			HttpServletRequest request) {
		String message = rentalPlansService.deleteRentalPlan(id, request);
		return ResponseEntity.ok(message);
	}
}
