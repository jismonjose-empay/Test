package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.PinCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PinCodeRepository extends JpaRepository<PinCodeEntity, Integer>,
        JpaSpecificationExecutor<PinCodeEntity> {
}
