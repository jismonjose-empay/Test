package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.InstitutionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstitutionsRepository extends
        JpaRepository<InstitutionsEntity, Integer>,
        JpaSpecificationExecutor<InstitutionsEntity> {
	
	InstitutionsEntity findByInstitutionCode(Integer code);
	
}
