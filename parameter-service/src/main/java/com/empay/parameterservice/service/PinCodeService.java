package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.PinCodeVO;

public interface PinCodeService {

    ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

    ResponseEntity<Object> findById(Integer id);

    String savePincode(PinCodeVO pinCodeVO);

    String updatePincode(PinCodeVO pinCodeVO, Integer id);

    String deletePincode(Integer id);
}
