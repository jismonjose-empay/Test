
package com.empay.parameterservice.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import com.empay.parameterservice.valueobjects.MDRPlanVO;
import com.empay.parameterservice.service.MDRPlanService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/params")
@RequiredArgsConstructor
public class MDRPlanController {

	private final MDRPlanService mdrPlanService;

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MDR_PLAN.VIEW')")
	@GetMapping("/get-mdrplans")
	public ResponseEntity<Object> getMDRPlans(
	        @RequestParam Integer page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(defaultValue = "mdrSerNumber") String sortBy,
	        @RequestParam(defaultValue = "asc") String sortDir,
	        @RequestParam(required = false) String search) {

	    return mdrPlanService.findAll(page, size, sortBy, sortDir, search);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MDR_PLAN.VIEW')")
	@GetMapping(value = "/find-mdrplan/{id}")
	public ResponseEntity<Object> findMDRPlan(@PathVariable Integer id) {
		return mdrPlanService.getMDRPlanById(id);
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MDR_PLAN.CREATE')")
	@PostMapping(value = "/save-mdrplan", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> saveMDRPlan(@RequestBody @Valid MDRPlanVO mdrPlanVo, HttpServletRequest request) {

		String message = mdrPlanService.createMDRPlan(mdrPlanVo, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", message, "status", "SUCCESS"));
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MDR_PLAN.EDIT')")
	@PutMapping(value = "/update-mdrplan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> updateMDRPlan(@PathVariable Integer id, @RequestBody @Valid MDRPlanVO mdrPlanVo,
			HttpServletRequest request) {

		String message = mdrPlanService.updateMDRPlan(mdrPlanVo, id, request);

		return ResponseEntity.ok(Map.of("message", message, "status", "SUCCESS"));
	}

	@PreAuthorize("hasAuthority('PARAMS.PARAM_MDR_PLAN.DELETE')")
	@DeleteMapping(value = "/delete-mdrplan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> deleteMDRPlan(@PathVariable Integer id) {

		String message = mdrPlanService.deleteMDRPlan(id);

		return ResponseEntity.ok(Map.of("message", message, "status", "SUCCESS"));
	}
}
