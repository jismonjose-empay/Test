package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.BankNamesVO;

import jakarta.servlet.http.HttpServletRequest;

public interface BankNamesService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(Integer id);

    String saveBankName(BankNamesVO bankNamesVO, HttpServletRequest request);

    String updateBankName(BankNamesVO bankNamesVO, Integer id, HttpServletRequest request);

    String deleteBankName(Integer id);

	ResponseEntity<Object> getAllBankNames();
}
