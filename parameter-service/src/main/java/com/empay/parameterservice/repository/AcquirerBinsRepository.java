package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.AcquirerBinsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AcquirerBinsRepository extends JpaRepository<AcquirerBinsEntity, String>,
        JpaSpecificationExecutor<AcquirerBinsEntity> {
}
