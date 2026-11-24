package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.IFSCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IFSCodeRepository extends JpaRepository<IFSCodeEntity, String>,
        JpaSpecificationExecutor<IFSCodeEntity> {
}
