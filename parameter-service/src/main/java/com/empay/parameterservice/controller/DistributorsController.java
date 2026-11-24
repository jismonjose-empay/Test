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

import com.empay.parameterservice.service.DistributorsService;
import com.empay.parameterservice.valueobjects.DistributorsVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class DistributorsController {

	private final DistributorsService distributorsService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_DISTRIBUTOR.VIEW')")
	@GetMapping(value = "/get-distributors")
	public ResponseEntity<Object> getDistributors(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return distributorsService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_DISTRIBUTOR.VIEW')")
	@GetMapping(value = "/find-distributor/{id}")
	public ResponseEntity<Object> findDistributor(@PathVariable Integer id) {
		return distributorsService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_DISTRIBUTOR.CREATE')")
	@PostMapping(value = "/save-distributor")
	public ResponseEntity<String> saveDistributor(@RequestBody @Valid DistributorsVO distributorsVO,
			HttpServletRequest request) {
		String message = distributorsService.saveDistributor(distributorsVO,request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_DISTRIBUTOR.EDIT')")
	@PutMapping(value = "/update-distributor/{id}")
	public ResponseEntity<String> updateDistributor(@PathVariable Integer id,
			@RequestBody @Valid DistributorsVO distributorsVO, HttpServletRequest request) {
		String message = distributorsService.updateDistributor(distributorsVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_DISTRIBUTOR.DELETE')")
	@DeleteMapping(value = "/delete-distributor/{id}")
	public ResponseEntity<String> deleteDistributor(@PathVariable Integer id) {
		String message = distributorsService.deleteDistributor(id);
		return ResponseEntity.ok(message);
	}
}
