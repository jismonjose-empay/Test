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

import com.empay.parameterservice.service.RiskHoldService;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.RiskHoldVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class RiskHoldController {

	private final RiskHoldService riskHoldService;
	private final JwtUtils jwtUtils;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RISKHOLD.VIEW')")
	@GetMapping(value = "/get-riskhold")
	public ResponseEntity<Object> getRiskHolds(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return riskHoldService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RISKHOLD.VIEW')")
	@GetMapping(value = "/find-riskhold/{id}")
	public ResponseEntity<Object> findRiskHold(@PathVariable Integer id) {
		return riskHoldService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RISKHOLD.CREATE')")
	@PostMapping(value = "/save-riskhold")
	public ResponseEntity<String> saveRiskHold(@RequestBody @Valid RiskHoldVO riskHoldVO, HttpServletRequest request) {
		riskHoldVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		riskHoldVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
		String message = riskHoldService.saveRiskHold(riskHoldVO);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RISKHOLD.EDIT')")
	@PutMapping(value = "/update-riskhold/{id}")
	public ResponseEntity<String> updateRiskHold(@PathVariable Integer id, @RequestBody @Valid RiskHoldVO riskHoldVO,
			HttpServletRequest request) {
		riskHoldVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		riskHoldVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
		String message = riskHoldService.updateRiskHold(riskHoldVO, id);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_RISKHOLD.DELETE')")
	@DeleteMapping(value = "/delete-riskhold/{id}")
	public ResponseEntity<String> deleteRiskHold(@PathVariable Integer id) {
		String message = riskHoldService.deleteRiskHold(id);
		return ResponseEntity.ok(message);
	}
}
