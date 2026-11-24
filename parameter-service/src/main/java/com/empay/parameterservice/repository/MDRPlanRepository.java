package com.empay.parameterservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.stereotype.Repository;

import com.empay.parameterservice.entities.MDRPlansEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface MDRPlanRepository
		extends JpaRepository<MDRPlansEntity, Integer>, JpaSpecificationExecutor<MDRPlansEntity> {

	Optional<MDRPlansEntity> findByMdrSerNumber(Integer id);

	List<MDRPlansEntity> findByMdrInsCode(Integer insCode);
}