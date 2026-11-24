package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.StatesVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

public interface StatesService {

	ResponseEntity<Object> addState(@Valid StatesVO statesVO, HttpServletRequest request);

	ResponseEntity<Object> updateState(StatesVO statesVO,String id,HttpServletRequest request);

	ResponseEntity<Object> getState(String id);

	ResponseEntity<Object> deleteState(String id, HttpServletRequest request);

	Object getStates(Integer page, int size, String sortBy, String sortDir, String search);

	Object getAllStates();

}
