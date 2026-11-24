package com.empay.parameterservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.empay.parameterservice.entities.StatesEntity;

@Repository
public interface StatesRepo extends JpaRepository<StatesEntity, String>,JpaSpecificationExecutor<StatesEntity>{

	StatesEntity findByStateCode(String stateCode);
	

}
