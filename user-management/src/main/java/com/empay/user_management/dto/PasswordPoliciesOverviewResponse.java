package com.empay.user_management.dto;

import java.util.List;

/**
 * Combined response payload containing policy summaries and active policy details.
 *
 * @param policies list of lightweight policy summaries
 * @param activePolicy full configuration details of the active policy, or null if none active
 */
public record PasswordPoliciesOverviewResponse(
    List<PasswordPolicySummaryResponse> policies,
    PasswordPolicyResponse activePolicy
) {

    /**
     * Factory method creating an overview response.
     *
     * @param policies list of summary policies
     * @param activePolicy full details of the active policy
     * @return created overview response
     */
    public static PasswordPoliciesOverviewResponse of(
            List<PasswordPolicySummaryResponse> policies,
            PasswordPolicyResponse activePolicy) {
        return new PasswordPoliciesOverviewResponse(
            policies != null ? List.copyOf(policies) : List.of(),
            activePolicy
        );
    }
}
