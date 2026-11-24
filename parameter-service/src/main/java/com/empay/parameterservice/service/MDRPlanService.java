package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.MDRPlanVO;

import jakarta.servlet.http.HttpServletRequest;

public interface MDRPlanService {

	ResponseEntity<Object> getAllMDRPlans();

	ResponseEntity<Object> getMDRPlanById(Integer id);

	String createMDRPlan(MDRPlanVO mdrPlanDTO, HttpServletRequest request);

	String updateMDRPlan(MDRPlanVO mdrPlanDTO, Integer id, HttpServletRequest request);

	String deleteMDRPlan(Integer id);

	ResponseEntity<Object> findAll(int page, int size, String sortBy, String sortDir, String search);
}