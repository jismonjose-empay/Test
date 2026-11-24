package com.empay.user_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.empay.user_management.entities.UserStatusHistoryEntity;

public interface UserStatusHistoryRepository
		extends JpaRepository<UserStatusHistoryEntity, Integer>, JpaSpecificationExecutor<UserStatusHistoryEntity> {

	List<UserStatusHistoryEntity> findByUserSerialNo(Integer userSerialNo);

}
