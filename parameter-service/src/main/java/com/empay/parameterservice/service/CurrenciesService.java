package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.CurrenciesVO;

import jakarta.servlet.http.HttpServletRequest;

public interface CurrenciesService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(String code);

    String saveCurrency(CurrenciesVO currenciesVO, HttpServletRequest request);

    String updateCurrency(CurrenciesVO currenciesVO, String code, HttpServletRequest request);

    String deleteCurrency(String code);

	ResponseEntity<Object> getAllCurrencies();
}
