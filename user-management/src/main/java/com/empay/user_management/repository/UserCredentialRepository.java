package com.empay.user_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.empay.user_management.entities.UserCredentialEntity;

public interface UserCredentialRepository
		extends JpaRepository<UserCredentialEntity, Integer>, JpaSpecificationExecutor<UserCredentialEntity> {

	UserCredentialEntity findByUserSerialNo(Integer userSerialNo);

}
