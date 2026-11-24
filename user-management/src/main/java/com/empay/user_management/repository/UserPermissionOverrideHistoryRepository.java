
package com.empay.user_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.UserPermissionOverrideHistoryEntity;

@Repository
public interface UserPermissionOverrideHistoryRepository
        extends JpaRepository<UserPermissionOverrideHistoryEntity, Long>,
                JpaSpecificationExecutor<UserPermissionOverrideHistoryEntity> {

    List<UserPermissionOverrideHistoryEntity>
            findAllByOverrideIdOrderByChangedAtDesc(Long overrideId);

    List<UserPermissionOverrideHistoryEntity>
            findAllByUserSerialNoOrderByChangedAtDesc(Long userSerialNo);
}
