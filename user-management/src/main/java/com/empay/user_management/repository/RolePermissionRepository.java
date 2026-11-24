package com.empay.user_management.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.RolePermissionEntity;
import com.empay.user_management.entities.RolePermissionId;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

	@EntityGraph(attributePaths = { "permission", "role" })
	List<RolePermissionEntity> findByIdRoleCode(String roleCode);

	@EntityGraph(attributePaths = { "permission", "role" })
	List<RolePermissionEntity> findByIdPermissionCode(String permissionCode);

	boolean existsByIdRoleCode(String roleCode);

	boolean existsByIdPermissionCode(String permissionCode);

	boolean existsByIdRoleCodeAndIdPermissionCode(String roleCode, String permissionCode);

	@Query("SELECT rp.id.permissionCode FROM RolePermissionEntity rp WHERE rp.id.roleCode = :roleCode")
	Set<String> findAssignedPermissionCodesByRoleCode(@Param("roleCode") String roleCode);

	@EntityGraph(attributePaths = { "permission", "role" })
	@Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.id.roleCode = :roleCode ORDER BY rp.permission.module ASC, rp.permission.resource ASC")
	List<RolePermissionEntity> findAssignmentsWithPermissionByRoleCode(@Param("roleCode") String roleCode);

	@EntityGraph(attributePaths = { "permission", "role" })
	@Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.id.roleCode IN :roleCodes ORDER BY rp.id.roleCode ASC, rp.permission.module ASC, rp.permission.resource ASC, rp.permission.actionName ASC")
	List<RolePermissionEntity> findAssignmentsByRoleCodes(@Param("roleCodes") java.util.Collection<String> roleCodes);

	@Modifying
	@Query("DELETE FROM RolePermissionEntity rp WHERE rp.id.roleCode = :roleCode")
	void deleteByRoleCode(@Param("roleCode") String roleCode);

	@Modifying
	@Query("DELETE FROM RolePermissionEntity rp WHERE rp.id.roleCode = :roleCode AND rp.id.permissionCode IN :permissionCodes")
	void deleteByRoleCodeAndPermissionCodes(@Param("roleCode") String roleCode,
			@Param("permissionCodes") List<String> permissionCodes);

	@Modifying
	@Query("DELETE FROM RolePermissionEntity rp WHERE rp.id.permissionCode = :permissionCode")
	void deleteByPermissionCode(@Param("permissionCode") String permissionCode);

	@Query("""
			SELECT COUNT(rp1) > 0
			FROM RolePermissionEntity rp1, RolePermissionEntity rp2
			WHERE rp1.id.roleCode = rp2.id.roleCode
			  AND rp1.id.permissionCode = :permissionCodeA
			  AND rp2.id.permissionCode = :permissionCodeB
			""")
	boolean existsRoleWithBothPermissions(@Param("permissionCodeA") String permissionCodeA,
			@Param("permissionCodeB") String permissionCodeB);

	@Query("""
			SELECT COUNT(rp1) > 0
			FROM RolePermissionEntity rp1, RolePermissionEntity rp2
			WHERE rp1.id.roleCode = :roleCodeA
			  AND rp2.id.roleCode = :roleCodeB
			  AND rp1.id.permissionCode = rp2.id.permissionCode
			""")
	boolean existsSharedPermission(@Param("roleCodeA") String roleCodeA, @Param("roleCodeB") String roleCodeB);
}
