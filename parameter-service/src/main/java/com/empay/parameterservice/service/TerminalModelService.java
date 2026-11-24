package com.empay.parameterservice.service;

import com.empay.parameterservice.valueobjects.TerminalModelVO;
import org.springframework.http.ResponseEntity;

public interface TerminalModelService {

    ResponseEntity<Object> findAll(Integer page, int size, String sortBy, String sortDir, String search);

    ResponseEntity<Object> findById(Integer id);

    String saveTerminalModel(TerminalModelVO terminalModelVO);

    String updateTerminalModel(TerminalModelVO terminalModelVO, Integer id);

    String deleteTerminalModel(Integer id);
}
