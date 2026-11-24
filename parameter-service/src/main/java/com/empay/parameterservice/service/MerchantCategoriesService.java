package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.MerchantCategoriesVO;

import jakarta.servlet.http.HttpServletRequest;

public interface MerchantCategoriesService {

	ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

	ResponseEntity<Object> findById(String id);

	String saveMerchantCategory(MerchantCategoriesVO merchantCategoriesVO, HttpServletRequest request);

	String updateMerchantCategory(MerchantCategoriesVO merchantCategoriesVO, String id, HttpServletRequest request);

	String deleteMerchantCategory(String id);
}
