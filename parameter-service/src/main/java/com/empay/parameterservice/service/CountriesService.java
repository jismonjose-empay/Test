package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.CountriesVO;

import jakarta.servlet.http.HttpServletRequest;

public interface CountriesService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(String id);

    String saveCountry(CountriesVO countriesVO, HttpServletRequest request);

    String updateCountry(CountriesVO countriesVO, String id, HttpServletRequest request);

    String deleteCountry(String id);

	ResponseEntity<Object> getAllCountries();
}
