package com.empay.user_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.RolePermissionHistoryEntity;

/**
 * Data access repository for role permission history entities.
 * Supports specification execution for dynamic filtering and pagination.
 */
@Repository
public interface RolePermissionHistoryRepository
		extends JpaRepository<RolePermissionHistoryEntity, Long>,
		JpaSpecificationExecutor<RolePermissionHistoryEntity> {
}
