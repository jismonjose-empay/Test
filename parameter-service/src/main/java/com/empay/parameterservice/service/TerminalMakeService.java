package com.empay.parameterservice.service;

import org.springframework.http.ResponseEntity;

import com.empay.parameterservice.valueobjects.TerminalMakeVO;

public interface TerminalMakeService {

    ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

    ResponseEntity<Object> findById(Integer id);

    String saveTerminalMake(TerminalMakeVO terminalMakeVO);

    String updateTerminalMake(TerminalMakeVO terminalMakeVO, Integer id);

    String deleteTerminalMake(Integer id);

	ResponseEntity<Object> getAllTerminalMakesWithIns();
}
