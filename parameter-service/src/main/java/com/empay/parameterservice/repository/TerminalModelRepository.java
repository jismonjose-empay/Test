package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.TerminalModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TerminalModelRepository extends JpaRepository<TerminalModelEntity,Integer>,
        JpaSpecificationExecutor<TerminalModelEntity> {
}
