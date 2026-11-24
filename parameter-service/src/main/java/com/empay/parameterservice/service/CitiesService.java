package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.CitiesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

public interface CitiesService {

	ResponseEntity<Object> addCity(@Valid CitiesVO citiesVO, HttpServletRequest request);

	ResponseEntity<Object> updateCity(Integer id, CitiesVO citiesVO, HttpServletRequest request);

	ResponseEntity<Object> getCity(Integer id);

	ResponseEntity<Object> deleteCity(Integer id, HttpServletRequest request);

	Object getCities(Integer page, int size, String sortBy, String sortDir, String search);

	Object getAllCities();

}
