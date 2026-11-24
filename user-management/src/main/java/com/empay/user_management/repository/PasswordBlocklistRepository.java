package com.empay.user_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.PasswordBlocklistEntity;

/**
 * Spring Data repository for accessing the password blocklist.
 */
@Repository
public interface PasswordBlocklistRepository extends JpaRepository<PasswordBlocklistEntity, String> {
}
