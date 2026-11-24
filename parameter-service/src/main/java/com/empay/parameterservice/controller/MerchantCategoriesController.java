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

import com.empay.parameterservice.service.MerchantCategoriesService;
import com.empay.parameterservice.valueobjects.MerchantCategoriesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class MerchantCategoriesController {

	private final MerchantCategoriesService merchantCategoriesService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MERCHANT_CAT.VIEW')")
	@GetMapping(value = "/get-merchantcategories")
	public ResponseEntity<Object> getMerchantCategories(
			@RequestParam Integer page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "merchantCategoryCode") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir, @RequestParam(required = false) String search) {
		return merchantCategoriesService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MERCHANT_CAT.VIEW')")
	@GetMapping(value = "/find-merchantcategory/{id}")
	public ResponseEntity<Object> findMerchantCategory(@PathVariable String id) {
		return merchantCategoriesService.findById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MERCHANT_CAT.CREATE')")
	@PostMapping(value = "/save-merchantcategory")
	public ResponseEntity<String> saveMerchantCategory(@RequestBody @Valid MerchantCategoriesVO merchantCategoriesVO,
			HttpServletRequest request) {
		String message = merchantCategoriesService.saveMerchantCategory(merchantCategoriesVO, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(message);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MERCHANT_CAT.EDIT')")
	@PutMapping(value = "/update-merchantcategory/{id}")
	public ResponseEntity<String> updateMerchantCategory(@PathVariable String id,
			@RequestBody @Valid MerchantCategoriesVO merchantCategoriesVO, HttpServletRequest request) {
		String message = merchantCategoriesService.updateMerchantCategory(merchantCategoriesVO, id, request);
		return ResponseEntity.ok(message);
	}
	
	@PreAuthorize("hasAuthority('PARAMS.PARAM_MERCHANT_CAT.DELETE')")
	@DeleteMapping(value = "/delete-merchantcategory/{id}")
	public ResponseEntity<String> deleteMerchantCategory(@PathVariable String id) {
		String message = merchantCategoriesService.deleteMerchantCategory(id);
		return ResponseEntity.ok(message);
	}
}
