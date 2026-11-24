package com.empay.parameterservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.empay.parameterservice.entities.RentalPlansEntity;

/**
 * Spring Data JPA repository for rental plan entities.
 */
@Repository
public interface RentalPlansRepository extends JpaRepository<RentalPlansEntity, Integer>,
		JpaSpecificationExecutor<RentalPlansEntity> {

	List<RentalPlansEntity> findByStatusNot(Integer status);

	List<RentalPlansEntity> findByInstitutionCodeAndStatusNot(Integer institutionCode, Integer status);

	List<RentalPlansEntity> findByInstitutionCodeAndAggregatorSrlNoAndStatusNot(Integer institutionCode,
			Integer aggregatorSrlNo, Integer status);
}
