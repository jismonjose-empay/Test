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

import com.empay.parameterservice.service.CountriesService;
import com.empay.parameterservice.valueobjects.CountriesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class CountriesController {

	private final CountriesService countriesService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_COUNTRY.VIEW')")
	@GetMapping(value = "/get-countries")
	public ResponseEntity<Object> getCountries(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "countryCode") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return countriesService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_COUNTRY.VIEW')")
	@GetMapping(value = "/find-country/{id}")
	public ResponseEntity<Object> findCountry(@PathVariable String id) {
		return countriesService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_COUNTRY.CREATE')")
	@PostMapping(value = "/save-country")
	public ResponseEntity<String> saveCountry(@RequestBody @Valid CountriesVO countriesVO, HttpServletRequest request) {
		String message = countriesService.saveCountry(countriesVO, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_COUNTRY.EDIT')")
	@PutMapping(value = "/update-country/{id}")
	public ResponseEntity<String> updateCountry(@PathVariable String id, @RequestBody @Valid CountriesVO countriesVO,
			HttpServletRequest request) {
		String message = countriesService.updateCountry(countriesVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_COUNTRY.DELETE')")
	@DeleteMapping(value = "/delete-country/{id}")
	public ResponseEntity<String> deleteCountry(@PathVariable String id) {
		String message = countriesService.deleteCountry(id);
		return ResponseEntity.ok(message);
	}
	
	@PreAuthorize("hasAuthority('PARAMS.PARAM_COUNTRY.VIEW')")
	@GetMapping(value = "/get-all-countries")
	public ResponseEntity<Object> getAllCountries() {
		return countriesService.getAllCountries();
	}
}
