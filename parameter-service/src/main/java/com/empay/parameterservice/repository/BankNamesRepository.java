package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.BankNamesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BankNamesRepository extends
        JpaRepository<BankNamesEntity, Integer>,
        JpaSpecificationExecutor<BankNamesEntity> {

}
