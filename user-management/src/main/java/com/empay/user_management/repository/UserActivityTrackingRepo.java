package com.empay.user_management.repository;

import com.empay.user_management.entities.UserActivityTrackingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserActivityTrackingRepo extends JpaRepository<UserActivityTrackingEntity, Integer>,
		JpaSpecificationExecutor<UserActivityTrackingEntity> {

	@Query(value = "SELECT TOP 1 * FROM AUDIT_EVENT WITH (UPDLOCK, HOLDLOCK) ORDER BY audit_id DESC", nativeQuery = true)
	Optional<UserActivityTrackingEntity> findTopByOrderBySerialNumberDesc();

	Optional<UserActivityTrackingEntity> findTopBySerialNumberLessThanOrderBySerialNumberDesc(Integer serialNumber);

}
