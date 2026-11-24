package com.empay.user_management.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.empay.user_management.entities.LoginAttemptEntity;

public interface LoginAttemptRepository
		extends JpaRepository<LoginAttemptEntity, Long>, JpaSpecificationExecutor<LoginAttemptEntity> {

	List<LoginAttemptEntity> findTop100ByOrderByAttemptedAtDesc();

	Page<LoginAttemptEntity> findAllByOrderByAttemptedAtDesc(Pageable pageable);
}
