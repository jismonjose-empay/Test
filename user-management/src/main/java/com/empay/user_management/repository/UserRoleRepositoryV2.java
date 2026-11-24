package com.empay.user_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empay.user_management.entities.UserRoleEntityV2;

/**
 * Repository interface for user role assignments.
 * Provides data access operations for user role mappings.
 */
public interface UserRoleRepositoryV2 extends JpaRepository<UserRoleEntityV2, Integer>, JpaSpecificationExecutor<UserRoleEntityV2> {

	/**
	 * Finds all role assignments for a user by user serial number.
	 *
	 * @param serialNumber the user serial number
	 * @return the list of user role assignments
	 */
	List<UserRoleEntityV2> findBySerialNumber(Integer serialNumber);

	/**
	 * Checks if any user currently holds both specified roles simultaneously.
	 *
	 * @param roleA the first role code
	 * @param roleB the second role code
	 * @return true if at least one user holds both roles, false otherwise
	 */
	@Query("SELECT CASE WHEN COUNT(ur1) > 0 THEN true ELSE false END " +
	       "FROM UserRoleEntityV2 ur1 " +
	       "WHERE ur1.roleCode = :roleA " +
	       "AND ur1.serialNumber IN (" +
	       "    SELECT ur2.serialNumber FROM UserRoleEntityV2 ur2 WHERE ur2.roleCode = :roleB" +
	       ")")
	boolean existsUserWithBothRoles(@Param("roleA") String roleA, @Param("roleB") String roleB);

	boolean existsByRoleCode(String roleCode);
}
