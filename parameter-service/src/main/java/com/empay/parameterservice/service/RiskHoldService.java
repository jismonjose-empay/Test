package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.RiskHoldVO;

public interface RiskHoldService {

    ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

    ResponseEntity<Object> findById(Integer id);

    String saveRiskHold(RiskHoldVO riskHoldVO);

    String updateRiskHold(RiskHoldVO riskHoldVO, Integer id);

    String deleteRiskHold(Integer id);
}
