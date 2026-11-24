package com.empay.user_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.AppRoleEntity;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRoleEntity, String> {

	@EntityGraph(attributePaths = {"permissions"})
	Optional<AppRoleEntity> findByRoleCode(String roleCode);

	@EntityGraph(attributePaths = {"permissions"})
	Optional<AppRoleEntity> findWithPermissionsByRoleCode(String roleCode);

	boolean existsByRoleCode(String roleCode);

	@EntityGraph(attributePaths = {"permissions"})
	List<AppRoleEntity> findAllByOrderByRoleCodeAsc();

	@EntityGraph(attributePaths = {"permissions"})
	List<AppRoleEntity> findByRoleCodeInOrderByRoleCodeAsc(List<String> roleCodes);
}
