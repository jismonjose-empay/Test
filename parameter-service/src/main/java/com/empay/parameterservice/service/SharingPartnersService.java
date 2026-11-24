package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.SharingPartnersVO;

public interface SharingPartnersService {

    ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

    ResponseEntity<Object> findById(Integer id);

    String saveSharingPartner(SharingPartnersVO sharingPartnersVO);

    String updateSharingPartner(SharingPartnersVO sharingPartnersVO, Integer id);

    String deleteSharingPartner(Integer id);
}
