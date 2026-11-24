package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.RiskHoldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RiskHoldRepository extends JpaRepository<RiskHoldEntity, Integer>,
        JpaSpecificationExecutor<RiskHoldEntity> {
}
