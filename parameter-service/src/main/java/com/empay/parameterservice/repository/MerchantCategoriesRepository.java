package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.MerchantCategoriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantCategoriesRepository extends JpaRepository<MerchantCategoriesEntity, String>,
        JpaSpecificationExecutor<MerchantCategoriesEntity> {

}
