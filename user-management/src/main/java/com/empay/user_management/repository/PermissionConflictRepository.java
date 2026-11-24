
package com.empay.user_management.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.PermissionConflictEntity;

@Repository
public interface PermissionConflictRepository
        extends JpaRepository<PermissionConflictEntity, Long> {

    // Check whether a permission pair already exists
    boolean existsByPermissionCodeAAndPermissionCodeB(
            String permissionCodeA,
            String permissionCodeB);

    // Check for a duplicate pair excluding the current record during update
    boolean existsByPermissionCodeAAndPermissionCodeBAndConflictIdNot(
            String permissionCodeA,
            String permissionCodeB,
            Long conflictId);

    // Find conflicts matching collections of permission codes
    @EntityGraph(attributePaths = {
        "derivedRoleConflict", "permissionA", "permissionB"
    })
    List<PermissionConflictEntity>
            findByPermissionCodeAInAndPermissionCodeBIn(
                    Collection<String> permissionCodesA,
                    Collection<String> permissionCodesB);

    // Find conflicts involving either permission
    @EntityGraph(attributePaths = {
        "derivedRoleConflict", "permissionA", "permissionB"
    })
    List<PermissionConflictEntity> findByPermissionCodeAOrPermissionCodeB(
            String permissionCodeA,
            String permissionCodeB);

    // Fetch all conflicts with their related entities
    @Override
    @EntityGraph(attributePaths = {
        "derivedRoleConflict", "permissionA", "permissionB"
    })
    List<PermissionConflictEntity> findAll();
}
