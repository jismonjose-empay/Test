package com.empay.parameterservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.empay.parameterservice.entities.CitiesEntity;

public interface CitiesRepo extends JpaRepository<CitiesEntity, Integer>, JpaSpecificationExecutor<CitiesEntity> {

	CitiesEntity findBySerialNumber(Integer id);

}
