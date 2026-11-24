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

import com.empay.parameterservice.service.PinCodeService;
import com.empay.parameterservice.utils.JwtUtils;
import com.empay.parameterservice.valueobjects.PinCodeVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class PinCodeController {

	private final PinCodeService pinCodeService;
	private final JwtUtils jwtUtils;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_PINCODE.VIEW')")
	@GetMapping(value = "/get-pincodes")
	public ResponseEntity<Object> getPincodes(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "serialNumber") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return pinCodeService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_PINCODE.VIEW')")
	@GetMapping(value = "/find-pincode/{id}")
	public ResponseEntity<Object> findPincode(@PathVariable Integer id) {
		return pinCodeService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_PINCODE.CREATE')")
	@PostMapping(value = "/save-pincode")
	public ResponseEntity<String> savePincode(@RequestBody @Valid PinCodeVO pincodeVO, HttpServletRequest request) {
		pincodeVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		String message = pinCodeService.savePincode(pincodeVO);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_PINCODE.EDIT')")
	@PutMapping(value = "/update-pincode/{id}")
	public ResponseEntity<String> updatePincode(@PathVariable Integer id, @RequestBody @Valid PinCodeVO pincodeVO,
			HttpServletRequest request) {
		pincodeVO.setUpdatedUser(jwtUtils.getUserSerNumber(request));
		String message = pinCodeService.updatePincode(pincodeVO, id);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_PINCODE.DELETE')")
	@DeleteMapping(value = "/delete-pincode/{id}")
	public ResponseEntity<String> deletePincode(@PathVariable Integer id) {
		String message = pinCodeService.deletePincode(id);
		return ResponseEntity.ok(message);
	}
}
