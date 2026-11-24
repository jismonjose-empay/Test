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

import com.empay.parameterservice.service.AcquirerBinsService;
import com.empay.parameterservice.valueobjects.AcquirerBinsVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class AcquirerBinsController {

	private final AcquirerBinsService acquirerBinsService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_ACQUIRER_BIN.VIEW')")
	@GetMapping(value = "/get-acquirerbins")
	public ResponseEntity<Object> getAcquirerBins(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "acquirerBins") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return acquirerBinsService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_ACQUIRER_BIN.VIEW')")
	@GetMapping(value = "/find-acquirerbin/{id}")
	public ResponseEntity<Object> findAcquirerBin(@PathVariable String id) {
		return acquirerBinsService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_ACQUIRER_BIN.CREATE')")
	@PostMapping(value = "/save-acquirerbin")
	public ResponseEntity<String> saveAcquirerBin(@RequestBody @Valid AcquirerBinsVO acquirerBinsVO,
			HttpServletRequest request) {
		String message = acquirerBinsService.saveAcquirerBin(acquirerBinsVO, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_ACQUIRER_BIN.EDIT')")
	@PutMapping(value = "/update-acquirerbin/{id}")
	public ResponseEntity<String> updateAcquirerBin(@PathVariable String id,
			@RequestBody @Valid AcquirerBinsVO acquirerBinsVO, HttpServletRequest request) {
		String message = acquirerBinsService.updateAcquirerBin(acquirerBinsVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_ACQUIRER_BIN.DELETE')")
	@DeleteMapping(value = "/delete-acquirerbin/{id}")
	public ResponseEntity<String> deleteAcquirerBin(@PathVariable String id) {
		String message = acquirerBinsService.deleteAcquirerBin(id);
		return ResponseEntity.ok(message);
	}
}
