package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.AcquirerBinsVO;

import jakarta.servlet.http.HttpServletRequest;

public interface AcquirerBinsService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(String id);

    String saveAcquirerBin(AcquirerBinsVO acquirerBinsVO, HttpServletRequest request);

    String updateAcquirerBin(AcquirerBinsVO acquirerBinsVO, String id, HttpServletRequest request);

    String deleteAcquirerBin(String id);
}
