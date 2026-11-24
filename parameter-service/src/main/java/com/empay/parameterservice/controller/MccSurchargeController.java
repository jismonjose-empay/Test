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

import com.empay.parameterservice.service.MccSurchargeService;
import com.empay.parameterservice.valueobjects.MccSurchargeVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class MccSurchargeController {

	private final MccSurchargeService mccSurchargeService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MCC_SURCHARGE.VIEW')")
	@GetMapping(value = "/get-mccsurcharge")
	public ResponseEntity<Object> getMccSurcharge(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return mccSurchargeService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MCC_SURCHARGE.VIEW')")
	@GetMapping(value = "/find-mccsurcharge/{id}")
	public ResponseEntity<Object> findMccSurcharge(@PathVariable Integer id) {
		return mccSurchargeService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MCC_SURCHARGE.CREATE')")
	@PostMapping(value = "/save-mccsurcharge")
	public ResponseEntity<String> saveMccSurcharge(@RequestBody @Valid MccSurchargeVO mccSurchargeVO,
			HttpServletRequest request) {
		String message = mccSurchargeService.saveMccSurcharge(mccSurchargeVO, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MCC_SURCHARGE.EDIT')")
	@PutMapping(value = "/update-mccsurcharge/{id}")
	public ResponseEntity<String> updateMccSurcharge(@PathVariable Integer id,
			@RequestBody @Valid MccSurchargeVO mccSurchargeVO, HttpServletRequest request) {
		String message = mccSurchargeService.updateMccSurcharge(mccSurchargeVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MCC_SURCHARGE.DELETE')")
	@DeleteMapping(value = "/delete-mccsurcharge/{id}")
	public ResponseEntity<String> deleteMccSurcharge(@PathVariable Integer id) {
		String message = mccSurchargeService.deleteMccSurcharge(id);
		return ResponseEntity.ok(message);
	}
}
