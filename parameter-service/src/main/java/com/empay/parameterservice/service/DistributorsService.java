package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.DistributorsVO;

import jakarta.servlet.http.HttpServletRequest;

public interface DistributorsService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(Integer id);

	String saveDistributor(DistributorsVO distributorsVO, HttpServletRequest request);

	String updateDistributor(DistributorsVO distributorsVO, Integer id, HttpServletRequest request);

	String deleteDistributor(Integer id);
}
