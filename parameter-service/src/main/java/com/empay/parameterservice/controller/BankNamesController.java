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

import com.empay.parameterservice.service.BankNamesService;
import com.empay.parameterservice.valueobjects.BankNamesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class BankNamesController {

	private final BankNamesService bankNamesService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_BANK.VIEW')")
	@GetMapping(value = "/get-banknames")
	public ResponseEntity<Object> getBankNames(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return bankNamesService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_BANK.VIEW')")
	@GetMapping(value = "/find-bankname/{id}")
	public ResponseEntity<Object> findBankNames(@PathVariable Integer id) {
		return bankNamesService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_BANK.CREATE')")
	@PostMapping(value = "/save-bankname")
	public ResponseEntity<String> saveBankNames(@RequestBody @Valid BankNamesVO bankNamesVO,
			HttpServletRequest request) {
		String message = bankNamesService.saveBankName(bankNamesVO,request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_BANK.EDIT')")
	@PutMapping(value = "/update-bankname/{id}")
	public ResponseEntity<String> updateBankNames(@PathVariable Integer id, @RequestBody @Valid BankNamesVO bankNamesVO,
			HttpServletRequest request) {
		String message = bankNamesService.updateBankName(bankNamesVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_BANK.DELETE')")
	@DeleteMapping(value = "/delete-bankname/{id}")
	public ResponseEntity<String> deleteBankNames(@PathVariable Integer id) {
		String message = bankNamesService.deleteBankName(id);
		return ResponseEntity.ok(message);
	}
	
	@PreAuthorize("hasAuthority('PARAMS.PARAM_BANK.VIEW')")
	@GetMapping(value = "/get-all-banknames")
	public ResponseEntity<Object> getAllBankNames() {
		return bankNamesService.getAllBankNames();
	}
}
