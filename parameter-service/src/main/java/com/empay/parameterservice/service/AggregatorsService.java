package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.AggregatorsVO;

import jakarta.servlet.http.HttpServletRequest;

public interface AggregatorsService {
	
	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(Integer id);

    String saveAggregator(AggregatorsVO aggregatorsVO, HttpServletRequest request);

    String updateAggregator(AggregatorsVO aggregatorsVO, Integer id, HttpServletRequest request);

    String deleteAggregator(Integer id);
}
