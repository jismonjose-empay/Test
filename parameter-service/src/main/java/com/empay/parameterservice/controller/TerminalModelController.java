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

import com.empay.parameterservice.service.TerminalModelService;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.TerminalModelVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class TerminalModelController {

	private final TerminalModelService terminalModelService;
	private final JwtUtils jwtUtils;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MODEL.VIEW')")
	@GetMapping(value = "/get-terminalmodel")
	public ResponseEntity<Object> getTerminalModels(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return terminalModelService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MODEL.VIEW')")
	@GetMapping(value = "/find-terminalmodel/{id}")
	public ResponseEntity<Object> findTerminalModel(@PathVariable Integer id) {
		return terminalModelService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MODEL.CREATE')")
	@PostMapping(value = "/save-terminalmodel")
	public ResponseEntity<String> saveTerminalModel(@RequestBody @Valid TerminalModelVO terminalModelVO,
			HttpServletRequest request) {
		terminalModelVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		String message = terminalModelService.saveTerminalModel(terminalModelVO);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MODEL.EDIT')")
	@PutMapping(value = "/update-terminalmodel/{id}")
	public ResponseEntity<String> updateTerminalModel(@PathVariable Integer id,
			@RequestBody @Valid TerminalModelVO terminalModelVO, HttpServletRequest request) {
		terminalModelVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		String message = terminalModelService.updateTerminalModel(terminalModelVO, id);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_TERMINAL_MODEL.DELETE')")
	@DeleteMapping(value = "/delete-terminalmodel/{id}")
	public ResponseEntity<String> deleteTerminalModel(@PathVariable Integer id) {
		String message = terminalModelService.deleteTerminalModel(id);
		return ResponseEntity.ok(message);
	}
}
