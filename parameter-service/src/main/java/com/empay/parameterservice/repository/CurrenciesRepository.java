package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.CurrenciesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrenciesRepository extends JpaRepository<CurrenciesEntity, String>,
        JpaSpecificationExecutor<CurrenciesEntity> {

}
