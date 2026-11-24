
package com.empay.user_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.UserPermissionOverrideEntity;

@Repository
public interface UserPermissionOverrideRepository
extends JpaRepository<UserPermissionOverrideEntity, Long>,
        JpaSpecificationExecutor<UserPermissionOverrideEntity>{

    boolean existsByUserSerialNoAndPermissionCode(
            Long userSerialNo, String permissionCode);

    List<UserPermissionOverrideEntity> findByUserSerialNo(Long userSerialNo);
    
    
}
