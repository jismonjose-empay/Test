package com.empay.parameterservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empay.parameterservice.entities.RegionsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RegionsRepository extends JpaRepository<RegionsEntity,Integer>,
        JpaSpecificationExecutor<RegionsEntity> {
	boolean existsByRegionNameAndAlphaCodeAndNumericCode( String regionName,  String alphaCode,String numericCode);
	boolean existsByRegionNameAndAlphaCodeAndNumericCodeAndSerialNumberNot(String regionName,  String alphaCode,String numericCode, int serialNumber);
}
