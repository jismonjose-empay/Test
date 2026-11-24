package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.MccSurchargeVO;

import jakarta.servlet.http.HttpServletRequest;

public interface MccSurchargeService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(Integer id);

    String saveMccSurcharge(MccSurchargeVO mccSurchargeVO, HttpServletRequest request);

    String updateMccSurcharge(MccSurchargeVO mccSurchargeVO, Integer id, HttpServletRequest request);

    String deleteMccSurcharge(Integer id);
}
