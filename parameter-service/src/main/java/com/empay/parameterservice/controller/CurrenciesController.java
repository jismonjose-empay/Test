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

import com.empay.parameterservice.service.CurrenciesService;
import com.empay.parameterservice.valueobjects.CurrenciesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/params")
@RequiredArgsConstructor
public class CurrenciesController {

	private final CurrenciesService currenciesService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CURRENCY.VIEW')")
	@GetMapping(value = "/get-currencies")
	public ResponseEntity<Object> getCurrencies(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "currencyCode") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return currenciesService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CURRENCY.VIEW')")
	@GetMapping(value = "/find-currency/{id}")
	public ResponseEntity<Object> getCurrency(@PathVariable String id) {
		return currenciesService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CURRENCY.CREATE')")
	@PostMapping(value = "/save-currency")
	public ResponseEntity<String> saveCurrency(@RequestBody @Valid CurrenciesVO currenciesVO,
			HttpServletRequest request) {
		String message = currenciesService.saveCurrency(currenciesVO, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CURRENCY.EDIT')")
	@PutMapping(value = "/update-currency/{id}")
	public ResponseEntity<String> updateCurrency(@PathVariable String id, @RequestBody @Valid CurrenciesVO currenciesVO,
			HttpServletRequest request) {
		String message = currenciesService.updateCurrency(currenciesVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CURRENCY.DELETE')")
	@DeleteMapping(value = "/delete-currency/{id}")
	public ResponseEntity<String> deleteCurrency(@PathVariable String id) {
		String message = currenciesService.deleteCurrency(id);
		return ResponseEntity.ok(message);
	}
	
	@PreAuthorize("hasAuthority('PARAMS.PARAM_CURRENCY.VIEW')")
	@GetMapping(value = "/get-all-currencies")
	public ResponseEntity<Object> getAllCurrencies() {
		return currenciesService.getAllCurrencies();
	}
}
