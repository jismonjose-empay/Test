package com.empay.user_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.empay.user_management.entities.PasswordPolicyEntity;

/**
 * Spring Data repository for managing password policies.
 */
@Repository
public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicyEntity, Integer> {

	/**
	 * Finds a password policy by its unique policy code.
	 *
	 * @param policyCode the unique code of the policy
	 * @return optional containing the policy entity if found
	 */
	Optional<PasswordPolicyEntity> findByPolicyCode(String policyCode);

	/**
	 * Finds the active password policy.
	 *
	 * @param isActive the active flag ('Y' or 'N')
	 * @return optional containing the active policy entity if found
	 */
	Optional<PasswordPolicyEntity> findByIsActive(String isActive);

	/**
	 * Finds all active password policies ordered by policyId descending.
	 * Using UPPER(TRIM(isActive)) = 'Y' guarantees whitespace and case tolerance.
	 *
	 * @return list of active password policies
	 */
	@Query("SELECT p FROM PasswordPolicyEntity p WHERE UPPER(TRIM(p.isActive)) = 'Y' ORDER BY p.policyId DESC")
	List<PasswordPolicyEntity> findActivePolicies();

	/**
	 * Checks whether a policy exists with the specified policy code.
	 *
	 * @param policyCode the policy code to verify
	 * @return true if the policy code exists
	 */
	boolean existsByPolicyCode(String policyCode);

	/**
	 * Retrieves all password policies ordered by policy identifier ascending.
	 *
	 * @return list of password policy entities
	 */
	List<PasswordPolicyEntity> findAllByOrderByPolicyIdAsc();
}
