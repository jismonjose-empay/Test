package com.empay.parameterservice.repository;

import com.empay.parameterservice.entities.SharingPartnersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SharingPartnersRepository extends JpaRepository<SharingPartnersEntity, Integer>,
        JpaSpecificationExecutor<SharingPartnersEntity> {
}
