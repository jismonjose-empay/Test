package com.empay.user_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.PermissionEntity;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {

	Optional<PermissionEntity> findByPermissionCode(String permissionCode);

	boolean existsByPermissionCode(String permissionCode);

	List<PermissionEntity> findAllByOrderByModuleAscResourceAscActionNameAsc();

	List<PermissionEntity> findByModuleOrderByResourceAscActionNameAsc(String module);

	@Query("SELECT DISTINCT p.module FROM PermissionEntity p ORDER BY p.module ASC")
	List<String> findDistinctModules();

	@Query("SELECT p FROM PermissionEntity p JOIN RolePermissionEntity rp ON p.permissionCode = rp.id.permissionCode WHERE rp.id.roleCode = :roleCode ORDER BY p.module ASC, p.resource ASC, p.actionName ASC")
	List<PermissionEntity> findPermissionsByRoleCode(@Param("roleCode") String roleCode);
}
