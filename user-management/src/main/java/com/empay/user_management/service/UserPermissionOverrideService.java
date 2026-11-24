
package com.empay.user_management.service;


import com.empay.user_management.dto.UserPermissionOverridePageResponse;
import com.empay.user_management.dto.UserPermissionOverrideRequest;
import com.empay.user_management.dto.UserPermissionOverrideResponse;

public interface UserPermissionOverrideService {

    UserPermissionOverrideResponse createOverride(
            UserPermissionOverrideRequest request);

    UserPermissionOverridePageResponse getAllOverrides(
            String search,
            String sortBy,
            String sortDir,
            int page,
            int pageSize);

    UserPermissionOverrideResponse getOverrideById(Long id);
    
    void deleteOverride(Long overrideId, String changedBy);
    
}
