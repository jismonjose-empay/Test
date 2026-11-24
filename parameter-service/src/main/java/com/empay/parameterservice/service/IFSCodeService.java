package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.IFSCodeVO;

import jakarta.servlet.http.HttpServletRequest;

public interface IFSCodeService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(String code);

    String saveIFSC(IFSCodeVO ifsCodeVO, HttpServletRequest request);

    String updateIFSC(IFSCodeVO ifsCodeVO, String code, HttpServletRequest request);

    String deleteIFSC(String code);

}
