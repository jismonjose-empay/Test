package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.InstitutionsVO;

import jakarta.servlet.http.HttpServletRequest;

public interface InstitutionsService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(Integer id);

    String saveInstitution(InstitutionsVO institutionsVO, HttpServletRequest request);

    String updateInstitution(InstitutionsVO institutionsVO, Integer id, HttpServletRequest request);

    String deleteInstitution(Integer id);
}
