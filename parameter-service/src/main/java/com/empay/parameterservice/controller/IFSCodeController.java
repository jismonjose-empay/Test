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

import com.empay.parameterservice.service.IFSCodeService;
import com.empay.parameterservice.valueobjects.IFSCodeVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class IFSCodeController {

	private final IFSCodeService ifsCodeService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_IFSC.VIEW')")
	@GetMapping(value = "/get-ifsc")
	public ResponseEntity<Object> getIFSC(@RequestParam Integer page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "ifsc") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return ifsCodeService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_IFSC.VIEW')")
	@GetMapping(value = "/find-ifsc/{id}")
	public ResponseEntity<Object> findIFSC(@PathVariable String id) {
		return ifsCodeService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_IFSC.CREATE')")
	@PostMapping(value = "/save-ifsc")
	public ResponseEntity<String> saveIFSC(@RequestBody @Valid IFSCodeVO ifsCodeVO, HttpServletRequest request) {
		String message = ifsCodeService.saveIFSC(ifsCodeVO,request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_IFSC.EDIT')")
	@PutMapping(value = "/update-ifsc/{id}")
	public ResponseEntity<String> updateIFSC(@PathVariable String id, @RequestBody @Valid IFSCodeVO ifsCodeVO,
			HttpServletRequest request) {

		String message = ifsCodeService.updateIFSC(ifsCodeVO, id, request);
		return ResponseEntity.ok(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_IFSC.DELETE')")
	@DeleteMapping(value = "/delete-ifsc/{id}")
	public ResponseEntity<String> deleteIFSC(@PathVariable String id) {
		String message = ifsCodeService.deleteIFSC(id);
		return ResponseEntity.ok(message);
	}
}
