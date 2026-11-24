package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.RegionsVO;

public interface RegionsService {

    ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

    ResponseEntity<Object> findById(Integer id);

    String saveRegion(RegionsVO regionsVO);

    String updateRegion(RegionsVO regionsVO, Integer id);

    String deleteRegion(Integer id);
}
