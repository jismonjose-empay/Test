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

import com.empay.parameterservice.service.RegionsService;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.RegionsVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class RegionsController {

	private final RegionsService regionsService;
	private final JwtUtils jwtUtils;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_REGION.VIEW')")
	@GetMapping(value = "/get-regions")
	public ResponseEntity<Object> getRegions(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return regionsService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_REGION.VIEW')")
	@GetMapping(value = "/find-region/{id}")
	public ResponseEntity<Object> findRegion(@PathVariable Integer id) {
		return regionsService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_REGION.CREATE')")
	@PostMapping(value = "/save-region")
	public ResponseEntity<String> saveRegion(@RequestBody @Valid RegionsVO regionsVO, HttpServletRequest request) {
		regionsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		regionsVO.setInsCode(jwtUtils.getInstitutionCode(request));
		String message = regionsService.saveRegion(regionsVO);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_REGION.EDIT')")
	@PutMapping(value = "/update-region/{id}")
	public ResponseEntity<String> updateRegion(@PathVariable Integer id, @RequestBody @Valid RegionsVO regionsVO,
			HttpServletRequest request) {
		regionsVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		regionsVO.setInsCode(jwtUtils.getInstitutionCode(request));
		String message = regionsService.updateRegion(regionsVO, id);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_REGION.DELETE')")
	@DeleteMapping(value = "/delete-region/{id}")
	public ResponseEntity<String> deleteRegion(@PathVariable Integer id) {
		String message = regionsService.deleteRegion(id);
		return ResponseEntity.ok(message);
	}
}
