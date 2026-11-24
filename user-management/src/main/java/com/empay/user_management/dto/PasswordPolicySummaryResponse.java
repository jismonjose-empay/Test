package com.empay.user_management.dto;

import com.empay.user_management.entities.PasswordPolicyEntity;

/**
 * Summary record representing lightweight password policy metadata and status.
 *
 * @param policyId unique database identifier
 * @param policyCode unique alphanumeric business code
 * @param name human-readable policy name
 * @param description functional description of policy scope
 * @param policyStatus human-readable status indicating ACTIVE or INACTIVE
 * @param isActive true if the policy is currently active
 */
public record PasswordPolicySummaryResponse(
    Integer policyId,
    String policyCode,
    String name,
    String description,
    String policyStatus,
    boolean isActive
) {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    /**
     * Converts a password policy entity to a lightweight summary response.
     *
     * @param entity the policy entity to map
     * @return the mapped summary response, or null if the entity is null
     */
    public static PasswordPolicySummaryResponse fromEntity(PasswordPolicyEntity entity) {
        if (entity == null) {
            return null;
        }
        boolean active = entity.isPolicyActive();
        return new PasswordPolicySummaryResponse(
            entity.getPolicyId(),
            entity.getPolicyCode(),
            entity.getName(),
            entity.getDescription(),
            active ? STATUS_ACTIVE : STATUS_INACTIVE,
            active
        );
    }
}
