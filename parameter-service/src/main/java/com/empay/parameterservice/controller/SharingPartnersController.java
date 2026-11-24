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

import com.empay.parameterservice.service.SharingPartnersService;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.SharingPartnersVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class SharingPartnersController {

	private final SharingPartnersService sharingPartnersService;
	private final JwtUtils jwtUtils;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_SHARING_PARTNER.VIEW')")
	@GetMapping(value = "/get-sharingpartner")
	public ResponseEntity<Object> getSharingPartners(
			@RequestParam Integer page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return sharingPartnersService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_SHARING_PARTNER.VIEW')")
	@GetMapping(value = "/find-sharingpartner/{id}")
	public ResponseEntity<Object> findSharingPartner(@PathVariable Integer id) {
		return ResponseEntity.ok(sharingPartnersService.findById(id));
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_SHARING_PARTNER.CREATE')")
	@PostMapping(value = "/save-sharingpartner")
	public ResponseEntity<String> saveSharingPartner(@RequestBody @Valid SharingPartnersVO sharingPartnersVO,
			HttpServletRequest request) {
		sharingPartnersVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		sharingPartnersVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
		String message = sharingPartnersService.saveSharingPartner(sharingPartnersVO);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_SHARING_PARTNER.EDIT')")
	@PutMapping(value = "/update-sharingpartner/{id}")
	public ResponseEntity<String> updateSharingPartner(@PathVariable Integer id,
			@RequestBody @Valid SharingPartnersVO sharingPartnersVO, HttpServletRequest request) {
		sharingPartnersVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		sharingPartnersVO.setInstitutionCode(jwtUtils.getInstitutionCode(request));
		String message = sharingPartnersService.updateSharingPartner(sharingPartnersVO, id);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_SHARING_PARTNER.DELETE')")
	@DeleteMapping(value = "/delete-sharingpartner/{id}")
	public ResponseEntity<String> deleteSharingPartner(@PathVariable Integer id) {
		String message = sharingPartnersService.deleteSharingPartner(id);
		return ResponseEntity.ok(message);
	}
}
