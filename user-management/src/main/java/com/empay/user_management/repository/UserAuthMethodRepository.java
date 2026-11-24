package com.empay.user_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.UserAuthMethodEntity;

@Repository
public interface UserAuthMethodRepository extends JpaRepository<UserAuthMethodEntity, Long> {

	List<UserAuthMethodEntity> findByUserSerialNo(Integer userSerialNo);

	Optional<UserAuthMethodEntity> findByUserSerialNoAndProvider(Integer userSerialNo, String provider);

	Optional<UserAuthMethodEntity> findByProviderAndExternalSubject(String provider, String externalSubject);

	Optional<UserAuthMethodEntity> findByProviderAndExternalUsername(String provider, String externalUsername);

	boolean existsByProviderIgnoreCase(String provider);

	boolean existsByProviderAndExternalSubjectAndUserSerialNoNot(String provider, String externalSubject, Integer userSerialNo);

	boolean existsByProviderAndExternalUsernameAndUserSerialNoNot(String provider, String externalUsername, Integer userSerialNo);
}
