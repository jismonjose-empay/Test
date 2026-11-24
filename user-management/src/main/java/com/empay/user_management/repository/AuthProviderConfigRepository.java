package com.empay.user_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.AuthProviderConfigEntity;

@Repository
public interface AuthProviderConfigRepository extends JpaRepository<AuthProviderConfigEntity, String> {

	List<AuthProviderConfigEntity> findAllByOrderByDisplayOrderAsc();

	Optional<AuthProviderConfigEntity> findByProviderIdIgnoreCase(String providerId);

	boolean existsByProviderIdIgnoreCase(String providerId);
}

