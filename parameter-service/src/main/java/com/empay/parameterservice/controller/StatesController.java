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

import com.empay.parameterservice.service.StatesService;
import com.empay.parameterservice.valueobjects.StatesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class StatesController {

	private final StatesService statesService;
	
	@PreAuthorize("hasAuthority('PARAMS.PARAM_STATE.CREATE')")
	@PostMapping("add-state")
	public ResponseEntity<Object> addState(@RequestBody @Valid StatesVO statesVO, HttpServletRequest request) {
		return statesService.addState(statesVO, request);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_STATE.EDIT')")
	@PutMapping("/edit-state/{id}")
	public ResponseEntity<Object> updateState(@PathVariable String id,@Valid @RequestBody StatesVO statesVO,
	        HttpServletRequest request) {
	    return statesService.updateState(statesVO, id, request);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_STATE.VIEW')")
	@GetMapping(value = "get-state-data/{id}")
	public ResponseEntity<Object> getState(@PathVariable String id) {
		return statesService.getState(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_STATE.DELETE')")
	@DeleteMapping(value = "delete-state/{id}")
	public ResponseEntity<Object> deleteState(@PathVariable String id, HttpServletRequest request) {
		return statesService.deleteState(id, request);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_STATE.VIEW')")
	@GetMapping(value = "/get-states")
	public ResponseEntity<Object> getStates(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "stateCode") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return ResponseEntity.ok(statesService.getStates(page, size, sortBy, sortDir, search));
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_STATE.VIEW')")
	@GetMapping(value = "/get-all-states")
	public ResponseEntity<Object> getAllStates() {
		return ResponseEntity.ok(statesService.getAllStates());
	}

}
