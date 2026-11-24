package com.empay.user_management.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.RoleConflictEntity;

@Repository
public interface RoleConflictRepository extends JpaRepository<RoleConflictEntity, Long> {

	@EntityGraph(attributePaths = {"roleA", "roleB"})
	List<RoleConflictEntity> findByRoleCodeAOrRoleCodeB(String roleCodeA, String roleCodeB);

	@EntityGraph(attributePaths = {"roleA", "roleB"})
	List<RoleConflictEntity> findByRoleCodeAInAndRoleCodeBIn(Collection<String> roleCodesA, Collection<String> roleCodesB);

	@Override
	@EntityGraph(attributePaths = {"roleA", "roleB"})
	List<RoleConflictEntity> findAll();
	
	boolean existsByRoleCodeAAndRoleCodeB(String roleCodeA, String roleCodeB);

	@Query("""
        SELECT rc
        FROM RoleConflictEntity rc
        WHERE rc.roleCodeA IN :roles
          AND rc.roleCodeB IN :roles
        """)
	List<RoleConflictEntity> findConflictsByRoles(
			@Param("roles") List<String> roles);
}
