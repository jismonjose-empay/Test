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

import com.empay.parameterservice.service.AggregatorsService;
import com.empay.parameterservice.valueobjects.AggregatorsVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class AggregatorsController {

	private final AggregatorsService aggregatorsService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_AGGREGATOR.VIEW')")
	@GetMapping(value = "/get-aggregators")
	public ResponseEntity<Object> getAggregators(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return aggregatorsService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_AGGREGATOR.VIEW')")
	@GetMapping(value = "/find-aggregator/{id}")
	public ResponseEntity<Object> findAggregator(@PathVariable Integer id) {
		return aggregatorsService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_AGGREGATOR.CREATE')")
	@PostMapping(value = "/save-aggregator")
	public ResponseEntity<String> saveAggregator(@RequestBody @Valid AggregatorsVO aggregatorsVO,
			HttpServletRequest request) {
		String message = aggregatorsService.saveAggregator(aggregatorsVO, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_AGGREGATOR.EDIT')")
	@PutMapping(value = "/update-aggregator/{id}")
	public ResponseEntity<String> updateAggregator(@PathVariable Integer id,
			@RequestBody @Valid AggregatorsVO aggregatorsVO, HttpServletRequest request) {
		String message = aggregatorsService.updateAggregator(aggregatorsVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_AGGREGATOR.DELETE')")
	@DeleteMapping(value = "/delete-aggregator/{id}")
	public ResponseEntity<String> deleteAggregator(@PathVariable Integer id) {
		String message = aggregatorsService.deleteAggregator(id);
		return ResponseEntity.ok(message);
	}
}
