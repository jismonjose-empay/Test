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

import com.empay.parameterservice.service.TerminalMakeService;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.TerminalMakeVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class TerminalMakeController {

	private final TerminalMakeService terminalMakeService;
	private final JwtUtils jwtUtils;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MAKE.VIEW')")
	@GetMapping(value = "/get-terminalmake")
	public ResponseEntity<Object> getTerminalMakes(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return terminalMakeService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MAKE.VIEW')")
	@GetMapping(value = "/find-terminalmake/{id}")
	public ResponseEntity<Object> findTerminalMake(@PathVariable Integer id) {
		return terminalMakeService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MAKE.CREATE')")
	@PostMapping(value = "/save-terminalmake")
	public ResponseEntity<String> saveTerminalMake(@RequestBody @Valid TerminalMakeVO terminalMakeVO,
			HttpServletRequest request) {
		terminalMakeVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		String message = terminalMakeService.saveTerminalMake(terminalMakeVO);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MAKE.EDIT')")
	@PutMapping(value = "/update-terminalmake/{id}")
	public ResponseEntity<String> updateTerminalMake(@PathVariable Integer id,
			@RequestBody @Valid TerminalMakeVO terminalMakeVO, HttpServletRequest request) {
		terminalMakeVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		String message = terminalMakeService.updateTerminalMake(terminalMakeVO, id);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MAKE.DELETE')")
	@DeleteMapping(value = "/delete-terminalmake/{id}")
	public ResponseEntity<String> deleteTerminalMake(@PathVariable Integer id) {
		String message = terminalMakeService.deleteTerminalMake(id);
		return ResponseEntity.ok(message);
	}
	
	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MAKE.VIEW')")
	@GetMapping(value = "/get-all-terminalmakes")
	public ResponseEntity<Object> getAllTerminalMakes() {
		return terminalMakeService.getAllTerminalMakesWithIns();
	}

}
