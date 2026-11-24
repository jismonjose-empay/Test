package com.empay.parameterservice.controller;

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

import com.empay.parameterservice.service.CitiesService;
import com.empay.parameterservice.valueobjects.CitiesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class CitiesController {

	private final CitiesService citiesService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CITY.CREATE')")
	@PostMapping("add-city")
	public ResponseEntity<Object> addCity(@RequestBody @Valid CitiesVO citiesVO, HttpServletRequest request) {
		return citiesService.addCity(citiesVO, request);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CITY.EDIT')")
	@PutMapping(value = "edit-city/{id}")
	public ResponseEntity<Object> updateCity(@PathVariable Integer id, @RequestBody @Valid CitiesVO citiesVO,
			HttpServletRequest request) {
		return citiesService.updateCity(id, citiesVO, request);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CITY.VIEW')")
	@GetMapping(value = "get-city-data/{id}")
	public ResponseEntity<Object> getCity(@PathVariable Integer id) {
		return citiesService.getCity(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CITY.DELETE')")
	@DeleteMapping(value = "delete-city/{id}")
	public ResponseEntity<Object> deleteCity(@PathVariable Integer id, HttpServletRequest request) {
		return citiesService.deleteCity(id, request);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CITY.VIEW')")
	@GetMapping(value = "/get-cities")
	public ResponseEntity<Object> getCities(@RequestParam Integer page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return ResponseEntity.ok(citiesService.getCities(page, size, sortBy, sortDir, search));
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_CITY.VIEW')")
	@GetMapping(value = "/get-all-cities")
	public ResponseEntity<Object> getAllCities() {
		return ResponseEntity.ok(citiesService.getAllCities());
	}

}
