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

import com.empay.parameterservice.service.InstitutionsService;
import com.empay.parameterservice.valueobjects.InstitutionsVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class InstitutionsController {

	private final InstitutionsService institutionsService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_INSTITUTIONS.VIEW')")
	@GetMapping(value = "/get-institutions")
	public ResponseEntity<Object> getInstitutions(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "institutionCode") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return institutionsService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_INSTITUTIONS.VIEW')")
	@GetMapping(value = "/find-institution/{id}")
	public ResponseEntity<Object> findInstitution(@PathVariable Integer id) {
		return institutionsService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_INSTITUTIONS.CREATE')")
	@PostMapping(value = "/save-institution")
	public ResponseEntity<String> saveInstitution(@RequestBody @Valid InstitutionsVO institutionsVO,
			HttpServletRequest request) {
		String message = institutionsService.saveInstitution(institutionsVO, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_INSTITUTIONS.EDIT')")
	@PutMapping(value = "/update-institution/{id}")
	public ResponseEntity<String> updateInstitution(@PathVariable Integer id,
			@RequestBody @Valid InstitutionsVO institutionsVO, HttpServletRequest request) {
		String message = institutionsService.updateInstitution(institutionsVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_INSTITUTIONS.DELETE')")
	@DeleteMapping(value = "/delete-institution/{id}")
	public ResponseEntity<String> deleteInstitution(@PathVariable Integer id) {
		String message = institutionsService.deleteInstitution(id);
		return ResponseEntity.ok(message);
	}
}
